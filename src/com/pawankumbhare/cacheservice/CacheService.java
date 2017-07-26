package com.pawankumbhare.cacheservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.PriorityBlockingQueue;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.pawankumbhare.data_structure.LFUCache;

/* Kyro v4.0.1 is used to serialize and deserialize any object which may or may not implement serializable interface.
 * Dependencies - https://github.com/EsotericSoftware/kryo
 *  1) kryo-4.0.1.jar
 *  2) kryo-4.0.1-javadoc.jar
 *  3) kryo-4.0.1-sources.jar
 *  4) objenesis-2.1.jar
 *  5) minlog-1.3.0.jar
 *  6) reflectasm-1.10.1-shaded.jar 
 */

public final class CacheService implements Serializable, Cloneable {

	private   static final long   serialVersionUID 				= 402566264949198429L;
	private   static final int	  INITIAL_CACHE_CAPACITY		= 100; 		// Initial cache capacity.
	protected static final int 	  EXPIRED_OBJECT_CHECK_INTERVAL = 1 * 1000; // Interval to check for expired object -> 1 sec
	private   static final String EXTENSION 					= ".obj"; 	// Extension for deserialized objects.
	
	private volatile static	ExpiredObjectManager			   expiredObjMgr;
	private volatile static Timer							   timer;
	private volatile static	CacheService 					   instance;	// Single instance's reference of CacheService
	private	volatile static PriorityBlockingQueue<CacheObject> expiryQueue;	// Thread safe Minimum Priority Queue.
	private	volatile static LFUCache<String, CacheObject> 	   lfu; 		// Least Frequently Used Cache.
	
	/*
	 * Constructor.
	 */
	private CacheService() {
		// Initally system will reserve space for INITIAL_CACHE_CAPACITY times object.
		expiryQueue = new PriorityBlockingQueue<>(INITIAL_CACHE_CAPACITY, new Comparator<CacheObject>() {
			@Override
			public int compare(CacheObject o1, CacheObject o2) {
				return Long.compare(o1.getExpiryTime(), o2.getExpiryTime());
			}
		});
		lfu = new LFUCache<>(INITIAL_CACHE_CAPACITY); // Initializing LFU Cache with INITIAL_CACHE_CAPACITY
		//Instantiating timer object to call checkExpiredObjects() in every EXPIRED_OBJECT_CHECK_INTERVAL
        timer = new Timer();
        expiredObjMgr = new ExpiredObjectManager();
        timer.schedule(expiredObjMgr, CacheObject.MIN_EXPIRE_TIME, EXPIRED_OBJECT_CHECK_INTERVAL); 
	}
	
	class ExpiredObjectManager extends TimerTask {
        public void run() {
        	CacheService.getInstance().checkExpiredObjects();
        }
    }
	
	/*
	 * CacheService is a Singleton class, its single instance can be accessed via getInstance() method. 
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
	 * To maintain Singleton Pattern by preventing Serialization of CacheService's object.
	 */
	public Object readResolve() {
        return instance;
    }
	
	/*
	 * To maintain Singleton Pattern by preventing Cloning of CacheService's object.
	 */
	public Object clone() {
        return CacheService.getInstance();
    }

	/*
	 * This method gets object by key from cache.
	 * If in LFU cache 
	 * 		If not expired -> returns the object
	 * 		Else return null
	 * Else If object exists in Disk
	 * 		If not expired -> Deserialize and add in LFU cache and return the object
	 * 		Else return null
	 */
	public Object get(String key) {
		if (lfu.containsKey(key)) {
			CacheObject cacheObj = (CacheObject) lfu.get(key);
			if (!cacheObj.isExpired()) {
				CacheObject tmpCacheObj = lfu.get(key);
				if (tmpCacheObj != null) return tmpCacheObj.getCachedObject();
			} else {
				System.out.println("Key - " + cacheObj.getKey() + " is expired");
				remove(cacheObj.getKey());
				return null;
			}
		}
		CacheObject cacheObj = deserialize(key);
		if (cacheObj == null) return null;
	    if (cacheObj.isExpired()) remove(cacheObj.getKey());
	    else {
	    	CacheObject removed = lfu.set(cacheObj.getKey(), cacheObj);
	    	serialize(removed);
	    	removeFromDisk(cacheObj.getKey());
	    	return cacheObj.getCachedObject();
	    }
		return null;
	}
	
	/*
	 * This method will set object in LFU cache with DEFAULT_EXPIRE_TIME and if any object gets removed from LFU cache, 
	 * system will serialize it to preserve memory.
	 */
	public void set(String key, Object object) {
		if (!lfu.containsKey(key)) {
			CacheObject cacheObj = new CacheObject(key, object);
			expiryQueue.add(cacheObj);
			CacheObject removed = lfu.set(key, cacheObj);
			if (removed != null) serialize(removed);
			System.out.println("Key - " + key + " will expire on " + new Date(cacheObj.getExpiryTime()).toString());
		} else System.err.println("Key - " + key + " already exists");
	}
	
	/*
	 * This method will set object in LFU cache with user defined minutes between MIN_EXPIRE_TIME and MAX_EXPIRE_TIME 
	 * and if any object gets removed from LFU cache, system will serialize it to preserve memory.
	 */
	public void set(String key, Object object, int minutes) {
		if (!lfu.containsKey(key)) {
			CacheObject cacheObj = new CacheObject(key, object, minutes);
			if (cacheObj.getKey() == null) return;
			expiryQueue.add(cacheObj);
			CacheObject removed = lfu.set(key, cacheObj);
			if (removed != null) serialize(removed);
			System.out.println("Key - " + key + " will expire on " + new Date(cacheObj.getExpiryTime()).toString());
		} else System.err.println("Key - " + key + " key already exists");
	}
	
	/*
	 * This method remove the expired objects from the cache.
	 */
	private void checkExpiredObjects() {
		CacheObject obj = expiryQueue.peek();
		if (obj != null && obj.isExpired()) {
			System.out.println("Key - " + obj.getKey() + " is expired");
			removeFromDisk(obj.getKey());
			lfu.remove(obj.getKey());
			expiryQueue.poll();
		}
	}
	
	/*
	 * This method removes the serialized object from the disk.
	 */
	private void removeFromDisk(String key) {
		try {
			File file = new File(key + EXTENSION);
			file.delete();
		} catch (Exception e) {
			System.err.println("Key - " + key + " not found");
		}
	}

	/*
	 * This method removes the object from the cache.
	 */
	public boolean remove(String key) {
		removeFromDisk(key);
		expiryQueue.remove(lfu.get(key));
		lfu.remove(key);
		return true;
	}

	/*
	 * This method stops the CacheService and removes all data and objects
	 */
	public void stop() {
		expiredObjMgr.cancel();
		timer.cancel();
		Iterator<CacheObject> itr = expiryQueue.iterator();
		while (itr.hasNext()) {
			String key = itr.next().getKey();
			removeFromDisk(key);
			lfu.remove(key);
		}
		expiryQueue.clear();
		instance = null;
	}
	
	/*
	 * This method serializes the object
	 */
	private boolean serialize(CacheObject cacheObj) {
		Kryo kryo = null;
	    Output output = null;
		try {
			kryo = new Kryo();
			kryo.register(CacheObject.class);
			output = new Output(new FileOutputStream(cacheObj.getKey() + EXTENSION));
			kryo.writeObject(output, cacheObj);
		    output.close();
		    System.out.println("Serializing key - " + cacheObj.getKey());
		    return true;
		} catch (Exception e) {
			System.err.println("Unable to serialize key - " + cacheObj.getKey());
		} finally {
			if (output != null) output.close();
		}
		return false;
	}
	
	/*
	 * This method deserializes the object
	 */
	private CacheObject deserialize(String key) {
		Kryo kryo = null;
		Input input = null;
		try {
			kryo = new Kryo();
			kryo.register(CacheObject.class);
			input = new Input(new FileInputStream(key + EXTENSION));
			CacheObject cacheObj = kryo.readObject(input, CacheObject.class);
		    System.out.println("Deserializing key - " + cacheObj.getKey());
		    return cacheObj;
		} catch (Exception e) {
			if (!(e instanceof FileNotFoundException)) System.err.println("Unable to deserialize key - " + key);
		} finally {
		    if (input != null) input.close();
		}
		return null;
	}
	
	@Override
	protected void finalize() throws Throwable {
		stop();
		super.finalize();
	}
}