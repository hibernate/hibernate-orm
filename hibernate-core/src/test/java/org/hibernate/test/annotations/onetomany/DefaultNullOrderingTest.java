package org.hibernate.test.annotations.onetomany;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class DefaultNullOrderingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.DEFAULT_NULL_ORDERING, "last" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Monkey.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-465")
	@RequiresDialect(H2Dialect.class)
	public void testHqlDefaultNullOrdering() {
		Session session = openSession();

		// Populating database with test data.
		session.getTransaction().begin();
		Monkey monkey1 = new Monkey();
		monkey1.setName( null );
		Monkey monkey2 = new Monkey();
		monkey2.setName( "Warsaw ZOO" );
		session.persist( monkey1 );
		session.persist( monkey2 );
		session.getTransaction().commit();

		session.getTransaction().begin();
		List<Zoo> orderedResults = (List<Zoo>) session.createQuery( "from Monkey m order by m.name" ).list(); // Should order by NULLS LAST.
		Assert.assertEquals( Arrays.asList( monkey2, monkey1 ), orderedResults );
		session.getTransaction().commit();

		session.clear();

		// Cleanup data.
		session.getTransaction().begin();
		session.delete( monkey1 );
		session.delete( monkey2 );
		session.getTransaction().commit();

		session.close();
	}
}
