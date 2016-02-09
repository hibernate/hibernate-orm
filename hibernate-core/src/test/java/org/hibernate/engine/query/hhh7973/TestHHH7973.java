package org.hibernate.engine.query.hhh7973;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * HHH-7973:
 * When calling Session.createQuery() with the query string
 * FROM Phase WHERE Comment = 'This is a test, Phase 1'
 * it is turned into
 * FROM com.example.Phase WHERE Comment = 'This is a test, com.example.Phase 1'
 * as shown by log output in the QueryTranslatorImpl.
 * This is, to put it mildly, highly surprising and undesirable behaviour.
 * The trigger seems to be the sequence <Comma Space Entity-Name>, which
 * triggers a transformation of the entity name into the fully qualified class
 * name even in string literals, where that has no business happening
 * whatsoever.
 */
public class TestHHH7973 extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Phase.class };
	}

	/**
	 * If the WHERE condition contains a class name of an entity then the class
	 * name will be replaced with the full qualified class name.
	 * Example:
	 * com/example/Phase.java with a field comment.
	 * The WHERE condition
	 * comment LIKE 'This is a test, Phase 1'
	 * will lead to a completely wrong HQL statement:
	 * FROM com.example.Phase WHERE Comment LIKE
	 * 'This is a test, com.example.Phase 1'
	 * The correct HQL statement should be:
	 * FROM com.example.Phase WHERE Comment LIKE
	 * 'This is a test, Phase 1'
	 *
	 * @throws Exception if an error occurs.
	 */
	@Test
	public void hhh7973Test() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Phase phase = new Phase();
		phase.setComment("This is a test, Phase 1");
		s.persist(phase);

		Query query = s.createQuery("FROM Phase");
		List<?> result = query.list();
		Assert.assertEquals("Result size of 1 expected but was " + result.size(), 1, result.size());

		query = s.createQuery("FROM Phase WHERE Comment = 'This is a test, Phase 1'");
		result = query.list();
		Assert.assertEquals("Result size of 1 expected but was " + result.size(), 1, result.size());
		query = s.createQuery("FROM Phase WHERE Comment LIKE 'This is a test, Phase 1'");
		result = query.list();
		Assert.assertEquals("Result size of 1 expected but was " + result.size(), 1, result.size());

		tx.rollback();
		tx = s.beginTransaction();

		phase = new Phase();
		phase.setComment("This is a test, " + Phase.class.getName() + " 1");
		s.persist(phase);

		query = s.createQuery("FROM Phase");
		result = query.list();
		Assert.assertEquals("Result size of 1 expected but was " + result.size(), 1, result.size());

		query = s.createQuery("FROM Phase WHERE Comment = 'This is a test, Phase 1'");
		result = query.list();
		Assert.assertTrue("Empty result expected but was " + result.size(), result.isEmpty());
		query = s.createQuery("FROM Phase WHERE Comment LIKE 'This is a test, Phase 1'");
		result = query.list();
		Assert.assertTrue("Empty result expected but was " + result.size(), result.isEmpty());

		tx.rollback();
		s.close();
	}

}
