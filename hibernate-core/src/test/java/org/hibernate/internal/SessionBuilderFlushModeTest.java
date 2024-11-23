package org.hibernate.internal;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.TestForIssue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Spahn
 */
@TestForIssue(jiraKey = "HHH-13974")
@RunWith(Parameterized.class)
public class SessionBuilderFlushModeTest {

	private static SessionFactory sessionFactory;

	@Parameters
	public static FlushMode[] parameters() {
		return FlushMode.values();
	}

	@Parameter
	public FlushMode flushMode;

	@BeforeClass
	public static void setup() {
		sessionFactory = new MetadataSources( new StandardServiceRegistryBuilder().build() ).buildMetadata().buildSessionFactory();
	}

	@AfterClass
	public static void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void testFlushMode() {
		try (final Session session = sessionFactory.withOptions().flushMode( flushMode ).openSession()) {
			assertEquals( flushMode, session.getHibernateFlushMode() );
		}
	}

}
