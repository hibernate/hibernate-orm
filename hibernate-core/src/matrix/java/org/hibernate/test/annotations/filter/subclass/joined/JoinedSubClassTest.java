package org.hibernate.test.annotations.filter.subclass.joined;

import junit.framework.Assert;

import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class JoinedSubClassTest extends BaseCoreFunctionalTestCase{


	
	@Override
	protected void afterConfigurationBuilt(Configuration configuration) {
		configuration.setProperty("hibernate.show_sql", "true");
		super.afterConfigurationBuilt(configuration);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{Animal.class, Mammal.class, Human.class, Club.class};
	}
	
	@Override
	protected void prepareTest() throws Exception {
		openSession();
		session.beginTransaction();
		
		Club club = new Club();
		club.setName("Mensa applicants");
		club.getMembers().add(createHuman(club, false, 90));
		club.getMembers().add(createHuman(club, false, 100));
		club.getMembers().add(createHuman(club, true, 110));
		session.persist(club);
		
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected void cleanupTest() throws Exception {
		openSession();
		session.beginTransaction();
		
		session.createQuery("delete from Human").executeUpdate();
		session.createQuery("delete from Club").executeUpdate();
		
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
	
	@Test
	public void testClub(){
		openSession();
		session.beginTransaction();

		Club club =  (Club) session.createQuery("from Club").uniqueResult();
		Assert.assertEquals(3, club.getMembers().size());
		session.clear();
		
		session.enableFilter("pregnantMembers");
		club =  (Club) session.createQuery("from Club").uniqueResult();
		Assert.assertEquals(1, club.getMembers().size());
		session.clear();
		
		session.enableFilter("iqMin").setParameter("min", 148);
		club =  (Club) session.createQuery("from Club").uniqueResult();
		Assert.assertEquals(0, club.getMembers().size());
		
		session.getTransaction().commit();
		session.close();
	}
	
	private Human createHuman(Club club, boolean pregnant, int iq){
		Human human = new Human();
		human.setClub(club);
		human.setName("Homo Sapiens");
		human.setPregnant(pregnant);
		human.setIq(iq);
		session.persist(human);
		return human;
	}
	
	private void assertCount(long expected){
		long count = (Long) session.createQuery("select count(h) from Human h").uniqueResult();	
		Assert.assertEquals(expected, count);
	}

}
