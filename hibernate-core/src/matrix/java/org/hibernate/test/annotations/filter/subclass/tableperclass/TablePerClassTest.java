package org.hibernate.test.annotations.filter.subclass.tableperclass;

import junit.framework.Assert;

import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class TablePerClassTest extends BaseCoreFunctionalTestCase{


	
	@Override
	protected void afterConfigurationBuilt(Configuration configuration) {
		configuration.setProperty("hibernate.show_sql", "true");
		super.afterConfigurationBuilt(configuration);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{Animal.class, Mammal.class, Human.class};
	}
	
	@Override
	protected void prepareTest() throws Exception {
		createHuman(false, 90);
		createHuman(false, 100);
		createHuman(true, 110);
	}

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
		session.beginTransaction();
		
		assertCount(3);	
		session.enableFilter("iqRange").setParameter("min", 101).setParameter("max", 140);
		assertCount(1);	
		
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPregnantFilter(){
		session.beginTransaction();
		
		assertCount(3);	
		session.enableFilter("pregnantOnly");
		assertCount(1);	
		
		session.getTransaction().commit();
		session.close();
	}
	@Test
	public void testNonHumanFilter(){
		session.beginTransaction();
		
		assertCount(3);	
		session.enableFilter("ignoreSome").setParameter("name", "Homo Sapiens");
		assertCount(0);	
		
		session.getTransaction().commit();
		session.close();
	}
	
	private void createHuman(boolean pregnant, int iq){
		openSession();
		session.beginTransaction();
		Human human = new Human();
		human.setName("Homo Sapiens");
		human.setPregnant(pregnant);
		human.setIq(iq);
		session.persist(human);
		session.getTransaction().commit();
	}
	
	private void assertCount(long expected){
		long count = (Long) session.createQuery("select count(h) from Human h").uniqueResult();	
		Assert.assertEquals(expected, count);
	}

}
