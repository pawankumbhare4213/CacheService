package com.pawankumbhare.cacheservice;

public class CachedObject {

	private String key;
	private Object object;
	private long startTime;
	private long expiryTime;

	protected static final long MAX_EXPIRY_ALLOWED = 2 * 60 * 60 * 1000; // 2 hrs
	protected static final long DEFAULT_EXPIRY_TIME = 10 * 60 * 1000; // 10 min
	protected static final long MIN_EXPIRY_ALLOWED = 1 * 60 * 1000; // 1 min

	public CachedObject() { }

	public CachedObject(String key, Object object) {
		if (object == null || key == null || key.length() == 0) {
			System.err.println("Invalid parameters.");
			return;
		}
		this.key = key;
		this.object = object;
		this.startTime = System.currentTimeMillis();
		this.expiryTime = this.startTime + DEFAULT_EXPIRY_TIME;
	}

	public CachedObject(String key, Object object, int minutes) {
		if (object == null || key == null || key.length() == 0) {
			System.err.println("Invalid parameters.");
			return;
		}
		long milliseconds = minutes * 60 * 1000;
		if (milliseconds > MAX_EXPIRY_ALLOWED || milliseconds < MIN_EXPIRY_ALLOWED) {
			System.err.println("Expiry time out of range, values allowed between 1 min to 120 mins.");
			return;
		}
		this.key = key;
		this.object = object;
		this.startTime = System.currentTimeMillis();
		this.expiryTime = this.startTime + milliseconds; // minutes to milliseconds
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(long expiryTime) {
		this.expiryTime = expiryTime;
	}

	public boolean isExpired() {
		if (System.currentTimeMillis() > this.expiryTime)
			return true;
		return false;
	}
}