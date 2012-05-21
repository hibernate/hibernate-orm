package org.hibernate.test.annotations.filter.subclass;

import junit.framework.Assert;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public abstract class SubClassTest extends BaseCoreFunctionalTestCase{
	
	@Override
	protected void prepareTest() throws Exception {
		openSession();
		session.beginTransaction();
		
		persistTestData();
		
		session.getTransaction().commit();
		session.close();
	}
	
	protected abstract void persistTestData();
	
	@Override
	protected void cleanupTest() throws Exception {
		openSession();
		session.beginTransaction();
		
		session.createQuery("delete from Human").executeUpdate();
		
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testIqFilter(){
		openSession();
		session.beginTransaction();
		
		assertCount(3);	
		session.enableFilter("iqRange").setParameter("min", 101).setParameter("max", 140);
		assertCount(1);	
		
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPregnantFilter(){
		openSession();
		session.beginTransaction();
		
		assertCount(3);	
		session.enableFilter("pregnantOnly");
		assertCount(1);	
		
		session.getTransaction().commit();
		session.close();
	}
	@Test
	public void testNonHumanFilter(){
		openSession();
		session.beginTransaction();
		
		assertCount(3);	
		session.enableFilter("ignoreSome").setParameter("name", "Homo Sapiens");
		assertCount(0);	
		
		session.getTransaction().commit();
		session.close();
	}
	
	
	private void assertCount(long expected){
		long count = (Long) session.createQuery("select count(h) from Human h").uniqueResult();	
		Assert.assertEquals(expected, count);
	}

}
