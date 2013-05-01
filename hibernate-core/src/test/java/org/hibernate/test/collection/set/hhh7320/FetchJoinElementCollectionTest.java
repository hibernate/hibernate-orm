package org.hibernate.test.collection.set.hhh7320;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class FetchJoinElementCollectionTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Article.class, Event.class,
				LocalizedArticle.class, LocalizedEvent.class };
	}

	private static final String WORKING_QUERY = "SELECT DISTINCT a "
			+ "FROM Article a " + "LEFT OUTER JOIN FETCH a.event "
			+ "LEFT OUTER JOIN FETCH a.event.localized";
	private static final String NOT_WORKING_QUERY = "SELECT DISTINCT a "
			+ "FROM Article a " + "LEFT OUTER JOIN FETCH a.localized "
			+ "LEFT OUTER JOIN FETCH a.event "
			+ "LEFT OUTER JOIN FETCH a.event.localized";

	@Test
	public void test() {
		Session session = null;
		
		try {
			session = openSession();
	
			session.createQuery(WORKING_QUERY);
			session.createQuery(NOT_WORKING_QUERY);
		} finally {
			session.close();
		}
	}
}
