package org.hibernate.internal;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.TestForIssue;
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

	@Parameters
	public static FlushMode[] parameters() {
		return FlushMode.values();
	}

	@Parameter
	public FlushMode flushMode;

	@Test
	public void testFlushMode() {
		try (final SessionFactory sessionFactory = new MetadataSources( new StandardServiceRegistryBuilder().build() ).buildMetadata().buildSessionFactory()) {
			try (final Session session = sessionFactory.withOptions().flushMode( flushMode ).openSession()) {
				assertEquals( flushMode, session.getHibernateFlushMode() );
			}
		}
	}
}
