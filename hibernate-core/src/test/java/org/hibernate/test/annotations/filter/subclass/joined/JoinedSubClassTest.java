package org.hibernate.test.annotations.filter.subclass.joined;

import junit.framework.Assert;

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.test.annotations.filter.subclass.SubClassTest;
import org.junit.Test;
import org.hibernate.testing.*;

@SkipForDialect(
        value = CUBRIDDialect.class,
        comment = "As of verion 8.4.1 CUBRID doesn't support temporary tables. This test fails with" +
                "HibernateException: cannot doAfterTransactionCompletion multi-table deletes using dialect not supporting temp tables"
)
public class JoinedSubClassTest extends SubClassTest{

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{Animal.class, Mammal.class, Human.class, Club.class};
	}
	
	@Override
	protected void cleanupTest() throws Exception {
		super.cleanupTest();
		openSession();
		session.beginTransaction();
		
		session.createQuery("delete from Club").executeUpdate();
		
		session.getTransaction().commit();
		session.close();
	}	
	
	@Override
	protected void persistTestData() {
		Club club = new Club();
		club.setName("Mensa applicants");
		club.getMembers().add(createHuman(club, false, 90));
		club.getMembers().add(createHuman(club, false, 100));
		club.getMembers().add(createHuman(club, true, 110));
		session.persist(club);
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
	

}
