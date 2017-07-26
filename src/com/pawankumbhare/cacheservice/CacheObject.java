package com.pawankumbhare.cacheservice;

public class CacheObject {

	protected static final long MAX_EXPIRE_TIME 	= 24 * 60 * 60 * 1000; // 24 hrs
	public static final long DEFAULT_EXPIRE_TIME = 1	 * 60 * 60 * 1000; // 1 hr
	protected static final long MIN_EXPIRE_TIME 	= 1	      * 60 * 1000; // 1 min
	
	private String key;
	private Object cachedObject;
	private long   startTime;
	private long   expiryTime;

	public CacheObject() { }

	public CacheObject(String key, Object object) {
		if (object == null || key == null || key.length() == 0) {
			System.err.println("Invalid parameters.");
			return;
		}
		this.key 			= key;
		this.cachedObject 	= object;
		this.startTime 		= System.currentTimeMillis();
		this.expiryTime 	= this.startTime + DEFAULT_EXPIRE_TIME;
	}

	public CacheObject(String key, Object object, int minutes) {
		if (object == null || key == null || key.length() == 0) {
			System.err.println("Invalid parameters.");
			return;
		}
		long milliseconds = minutes * 60 * 1000;
		if (milliseconds > MAX_EXPIRE_TIME || milliseconds < MIN_EXPIRE_TIME) {
			System.err.println("Expiry time out of range, values allowed between 1 min to 120 mins.");
			return;
		}
		this.key = key;
		this.cachedObject = object;
		this.startTime = System.currentTimeMillis();
		this.expiryTime = this.startTime + milliseconds; // minutes to milliseconds
	}
	
	public String getKey() {
		return key;
	}

	public Object getCachedObject() {
		return cachedObject;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getExpiryTime() {
		return expiryTime;
	}

	public boolean isExpired() {
		if (System.currentTimeMillis() > this.expiryTime) return true;
		return false;
	}
}