package com.pawankumbhare.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.Test;

import com.pawankumbhare.cacheservice.CacheObject;
import com.pawankumbhare.cacheservice.CacheService;

/* Apache Commons Lang3 v3.6
 * Dependencies - https://commons.apache.org/proper/commons-lang/
 * 1) commons-lang3-3.6.jar
 */

public class CacheServiceTest {
	
	static class TestClass {
		int id;
		String name;
		
		public TestClass() {}
		
		public TestClass(int id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String toString() {
			return "TestClass [id=" + id + ", name=" + name + "]";
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testGetInstance() {		
		//To check for multiple instances via cloning
		int orignalHashCode = CacheService.getInstance().hashCode();
		int clonedHashCode = CacheService.getInstance().clone().hashCode();
		assertEquals(orignalHashCode, clonedHashCode);
		
		//To check for multiple instances via serialization
		String key = "test";
		serialize(key, CacheService.getInstance());
		CacheService obj = (CacheService) deserialize(key);
		assertEquals(orignalHashCode, obj.hashCode());
		CacheService.getInstance().stop();
		
		//To check for multiple instances via Reflection
        try {
        	CacheService.getInstance();
            Constructor[] constructors = CacheService.class.getDeclaredConstructors();
            for (Constructor constructor : constructors) {
                constructor.setAccessible(true);
                constructor.newInstance();
                break;
            }
    		fail("Should throw Runtime Exception.");
        } catch (Exception e) {

        }
	}
	
	private void serialize(String key, Object obj) {
		try {
			FileOutputStream fos = new FileOutputStream(key + ".ser");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(obj);
			oos.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Object deserialize(String key) {
		Object obj = null;
		try {
			FileInputStream fis = new FileInputStream(key + ".ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			obj = ois.readObject();
			ois.close();
			File file = new File(key + ".ser");
			file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}

	//@Test
	public void testGet() {
		String key = "1";
		TestClass val = new TestClass(1, "test");
		assertEquals(null, CacheService.getInstance().get(key));
		CacheService.getInstance().set(key, val);
		Assert.assertTrue(EqualsBuilder.reflectionEquals(val, CacheService.getInstance().get(key)));
		CacheService.getInstance().stop();
	}

	//@Test
	public void testSetStringObject() throws InterruptedException {
		String key1 = "1";
		TestClass val1 = new TestClass(1, "test");
		CacheService.getInstance().set(key1, val1);
		
		if (!EqualsBuilder.reflectionEquals(val1, CacheService.getInstance().get(key1)))
			fail("Not equal - \n" + val1 + "\n" + CacheService.getInstance().get(key1));
		
		Thread.sleep(CacheObject.DEFAULT_EXPIRE_TIME + 1000);
		
		String key2 = "2";
		TestClass val2 = new TestClass(2, "test2");
		CacheService.getInstance().set(key2, val2);
		
		assertEquals(null, CacheService.getInstance().get(key1));
		if (!EqualsBuilder.reflectionEquals(val2, CacheService.getInstance().get(key2)))
			fail("Not equal - \n" + val2 + "\n" + CacheService.getInstance().get(key2));
		
		CacheService.getInstance().stop();
	}

	//@Test
	public void testSetStringObjectInt() throws InterruptedException {
		String key = "1";
		int min = 1;
		TestClass val = new TestClass(1, "test");
		CacheService.getInstance().set(key, val, min);
		Assert.assertTrue(EqualsBuilder.reflectionEquals(val, CacheService.getInstance().get(key)));
		CacheService.getInstance().stop();
	}

	//@Test
	public void testRemove() {
		String key = "1";
		TestClass val = new TestClass(1, "test");
		CacheService.getInstance().set(key, val);
		CacheService.getInstance().remove(key);
		assertEquals(null, CacheService.getInstance().get(key));
		CacheService.getInstance().stop();
	}
}
