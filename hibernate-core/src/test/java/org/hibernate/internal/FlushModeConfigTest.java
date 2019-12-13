package org.hibernate.internal;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.TestForIssue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

/**
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-13677" )
@RunWith( Parameterized.class )
public class FlushModeConfigTest {

	@Parameters
	public static FlushMode[] parameters() {
		return FlushMode.values();
	}

	@Parameter
	public FlushMode flushMode;

	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.FLUSH_MODE, flushMode.name() )
				.build();
	}

	@Test
	public void testFlushModeSettingTakingEffect() {
		try ( final SessionFactory sessionFactory = new MetadataSources(serviceRegistry).buildMetadata().buildSessionFactory() ) {
			try ( final Session session = sessionFactory.openSession() ) {
				assertEquals( flushMode, session.getHibernateFlushMode() );
			}
		}
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

}
