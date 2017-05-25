package com.pawankumbhare.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.pawankumbhare.cacheservice.CacheService;

public class CacheServiceTest {

	@Test
	public void testGetInstance() throws InterruptedException {
		Integer obj1 = new Integer(1);
		Integer obj2 = new Integer(2);
		CacheService service = CacheService.getInstance();
		if (service == null) fail("Not yet implemented");
		String key = "1";
		System.out.println("TESTING CACHE SERVICE START");
		service.setObject(key, obj1, 1);
		Thread.sleep(10000);
		service.setObject("2", obj2, 1);
		System.out.println("GET : " + service.getObjectByKey(key));
		service.checkAndRemoveExpiredObjects();
		//System.out.println("REMOVE : " + service.remove(key));
		System.out.println("GET : " + service.getObjectByKey("2"));
		//System.out.println("TESTING CACHE SERVICE END");
	}
}
