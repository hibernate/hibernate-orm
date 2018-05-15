package org.hibernate.test.cache;

import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Gail Badner.
 */
public class NoCachingRegionFactoryTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.CACHE_REGION_FACTORY, NoCachingRegionFactory.class.getName()  );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12508" )
	public void testSessionFactoryOptionsConsistent() {
		assertFalse( sessionFactory().getSessionFactoryOptions().isSecondLevelCacheEnabled() );
	}
}
