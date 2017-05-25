package com.pawankumbhare.cacheservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.PriorityBlockingQueue;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.pawankumbhare.data_structure.ConcurrentLRU;

/* Kyro ver 4.0.0 is used to serialize and deserialize unknown objects as objects sent by client may or may not be serializable.
 * Dependency on 4 jar files - 
 *  1) kryo-4.0.0
 *  2) objenesis-2.1.jar
 *  3) minlog-1.3.0.jar
 *  4) reflectasm-1.10.1-shaded.jar 
 */

public class CacheService {

	private static CacheService instance = null;
	private volatile static PriorityBlockingQueue<CachedObject> expiryPQ; // Thread safe Min Priority Queue.
	private volatile static ConcurrentLRU<String, CachedObject> lru; // Thread safe Least Recently Used Cache.
	private static final int ITITIAL_LRU_CAPACITY = 1000; // Initial LRU capacity.
	private static final int EXPIRY_CHECK_INTERVAL = 1 * 1000; // Interval to check for expired object. // 1 secs
	private static final String EXT = ".cache"; // Extension for deserialized objects.
	
	/*
	 * CacheService Constructor.
	 */
	private CacheService() {
		// Initally system will reserve space for 1000 objects
		expiryPQ = new PriorityBlockingQueue<>(1000, new Comparator<CachedObject>() { // Initializing Thread Safe Min Priority Queue
			@Override
			public int compare(CachedObject o1, CachedObject o2) {
				return Long.compare(o1.getExpiryTime(), o2.getExpiryTime());
			}
		});
		lru = new ConcurrentLRU<>(ITITIAL_LRU_CAPACITY); // Initializing Thread Safe LRU Cache with ITITIAL_LRU_CAPACITY
		class ExpiredObjectManager extends TimerTask {
            public void run() {
            	CacheService.getInstance().checkAndRemoveExpiredObjects();
            }
        }
		//Initializing timer object to call checkAndRemoveExipredObjects() in every EXPIRY_CHECK_INTERVAL
        Timer timer = new Timer();
        timer.schedule(new ExpiredObjectManager(), CachedObject.MIN_EXPIRY_ALLOWED, EXPIRY_CHECK_INTERVAL); 
	}
	
	/*
	 * CacheService is a Singleton class, so accessing its instance via getInstance() method. 
	 */
	public static CacheService getInstance() {
		if (instance == null) {
			synchronized (CacheService.class) {
				if (instance == null) instance = new CacheService();
			}
		}
		return instance;
	}
	
	/*
	 * Serialize objects if get removed from LRU cache
	 */
	private boolean serialize(CachedObject cachedObj) {
		Kryo kryo = null;
	    Output output = null;
		try {
			kryo = new Kryo();
			kryo.register(CachedObject.class);
			output = new Output(new FileOutputStream(cachedObj.getKey() + EXT));
			kryo.writeObject(output, cachedObj);
		    output.close();
		    System.out.println("SERIALIZED KEY : " + cachedObj.getKey());
		    return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Unable to serialize key " + cachedObj.getKey());
		} finally {
			if (output != null) output.close();
		}
		return false;
	}
	
	/*
	 * Check and deserialize objects if not found in LRU cache.
	 * Will return the object, if cached object is not expired.
	 * Otherwise will return null;
	 */
	private CachedObject deserialize(String key) {
		Kryo kryo = null;
		Input input = null;
		try {
			kryo = new Kryo();
			kryo.register(CachedObject.class);
			input = new Input(new FileInputStream(key + EXT));
			CachedObject cachedObj = kryo.readObject(input, CachedObject.class);
		    System.out.println("DESERIALIZED KEY : " + cachedObj.getKey());
		    return cachedObj;
		} catch (Exception e) {
			System.err.println("Unable to deserialize key " + key);
		} finally {
		    if (input != null) input.close();
		}
		return null;
	}

	/*
	 * Get cached object by key.
	 * First check in LRU cache, if present return the cached object.
	 * If not present in LRU cache, deserialize the object.
	 * If found in disk check its expiry, if expired then return null.
	 * If found in disk and not expired put in LRU cache and return cached object after removing retrieved copy from disk.
	 */
	public Object getObjectByKey(String key) {
		if (lru.containsKey(key)) {
			CachedObject cachedObj = (CachedObject) lru.get(key);
			if (!cachedObj.isExpired()) {
				CachedObject lruObject = lru.get(key);
				if (lruObject != null) return lruObject.getObject();
			} else System.err.println(cachedObj.getKey() + " key is expired.");
		}
		CachedObject cachedObj = deserialize(key);
		if (cachedObj == null) return null;
	    if (cachedObj.isExpired()) if (remove(cachedObj.getKey())) return null;
	    else {
	    	lru.put(cachedObj.getKey(), cachedObj);
	    	removeSerializedObject(cachedObj.getKey());
	    	return cachedObj.getObject();
	    }
		return null;
	}
	
	/*
	 * Set object to cache : DEFAULT_EXPIRY_TIME is used.
	 * It will set object in LRU cache and if any object gets removed from LRU cache, system will serialize it to preserve memory.
	 */
	public void setObject(String key, Object object) {
		if (!lru.containsKey(key)) {
			CachedObject cachedObj = new CachedObject(key, object);
			expiryPQ.add(cachedObj);
			CachedObject removed = lru.put(key, cachedObj);
			if (removed != null) serialize(removed);
			System.out.println(key + " key will be expired on " + new Date(cachedObj.getExpiryTime()).toString());
		} else System.err.println(key + " key already exists, please use different key.");
	}
	
	/*
	 * Set object to cache : User defined EXPIRY_TIME is used.
	 * EXPIRY_TIME must be between MIN_EXPIRY_ALLOWED and MAX_EXPIRY_ALLOWED.
	 * It will set object in LRU cache and if any object gets removed from LRU cache, system will serialize it to preserve memory.
	 */
	public void setObject(String key, Object object, int minutes) {
		if (!lru.containsKey(key)) {
			CachedObject cachedObj = new CachedObject(key, object, minutes);
			if (cachedObj.getKey() == null) return;
			expiryPQ.add(cachedObj);
			CachedObject removed = lru.put(key, cachedObj);
			if (removed != null) serialize(removed);
			System.out.println(key + " key will be expired on " + new Date(cachedObj.getExpiryTime()).toString());
		} else System.err.println(key + " key already exists, please use different key.");
	}
	
	/*
	 * Check if any object is expiring.
	 * If expired removes the object from cache system.
	 */
	public void checkAndRemoveExpiredObjects() {
		CachedObject obj = expiryPQ.peek();
		//if (obj != null) System.out.println("Key " + obj.getKey() + " will be expired on " + new Date(obj.getExpiryTime()));
		if (obj != null && obj.isExpired()) {
			System.out.println("EXPIRED : REMOVING KEY " + obj.getKey());
			removeSerializedObject(obj.getKey());
			lru.remove(obj.getKey());
			expiryPQ.poll();
		}
	}
	
	/*
	 * Removes serialized objects from disk only.
	 */
	private void removeSerializedObject(String key) {
		try {
			File file = new File(key + ".cache");
			if (file.delete()) System.out.println(key + " key is deleted!");
			else System.err.println("Delete operation of key " + key + " is failed.");
		} catch (Exception e) {
			System.err.println(key + " key not found!");
		}
	}

	/*
	 * Removes objects from whole cache system.
	 */
	public boolean remove(String key) {
		removeSerializedObject(key);
		expiryPQ.remove(lru.get(key));
		lru.remove(key);
		return true;
	}
}