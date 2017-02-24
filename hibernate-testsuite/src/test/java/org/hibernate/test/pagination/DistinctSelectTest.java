package org.hibernate.test.pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;

/**
 * HHH-5715 bug test case: Duplicated entries when using select distinct with join and pagination. 
 * 
 * @author Valotasios Yoryos
 * 
 */
public class DistinctSelectTest extends FunctionalTestCase {
	private static final int NUM_OF_USERS = 30;
	private static final int NUM_OF_TAGS = 5;

	public DistinctSelectTest(String string) {
		super(string);
	}

	public String[] getMappings() {
		return new String[] { "pagination/EntryTag.hbm.xml" };
	}

	public void feedDatabase() {
		List<Tag> tags = new ArrayList<Tag>();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		for (int i = 0; i < NUM_OF_TAGS; i++) {
			Tag tag = new Tag( "Tag: " + UUID.randomUUID().toString() );
			tags.add(tag);
			s.save(tag);
		}

		Random r = new Random();
		for (int i = 0; i < NUM_OF_USERS; i++) {
			Entry e = new Entry("Entry: " + UUID.randomUUID().toString());
			for (Tag tag: tags) {
				if (r.nextBoolean()) e.getTags().add( tag );
			};
			s.save(e);
		}
		t.commit();
		s.close();
	}

	public void testDistinctSelectWithJoin() {
		feedDatabase();
		
		Session s = openSession();
		s.beginTransaction();
		List<Entry> entries = s.createQuery("select distinct e from Entry e join e.tags t where t.surrogate != null order by e.name").setFirstResult(10).setMaxResults(5).list();
		Entry firstEntry = entries.remove(0);
		assertFalse("The list of entries should not contain dublicated Entry objects as we've done a distinct select", entries.contains(firstEntry));
		s.getTransaction().commit();
		s.close();
	}
	
	//HHH-6310 bug test case
	public void testDistinctSelectWithinAggragateFunction() {
		feedDatabase();
		
		Session s = openSession();
		s.beginTransaction();
		
		@SuppressWarnings("unchecked")
		List<Object[]> list = s.createQuery( "select e, count(distinct t) from Entry e join e.tags t group by e" )
			.setFirstResult( 10 )
			.setMaxResults( 5 )
			.list();
		
		assertEquals(5, list.size());
		
		for (Object[] o: list) {
			Entry e = (Entry) o[0];
			Long numOfTags = (Long) o[1];
			assertEquals( e.getTags().size(), numOfTags.intValue() );
		}

		s.getTransaction().commit();
		s.close();
	}
}
