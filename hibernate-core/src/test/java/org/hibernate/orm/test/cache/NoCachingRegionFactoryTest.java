/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;


import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;


/**
 * @author Gail Badner.
 */
@DomainModel(
		annotatedClasses = {
				ReferenceCacheTest.MyReferenceData.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.CACHE_REGION_FACTORY, value = "org.hibernate.cache.internal.NoCachingRegionFactory"),
		}
)
@SessionFactory
public class NoCachingRegionFactoryTest {

	@Test
	@JiraKey( value = "HHH-12508" )
	public void testSessionFactoryOptionsConsistent(SessionFactoryScope scope) {
		assertFalse( scope.getSessionFactory().getSessionFactoryOptions().isSecondLevelCacheEnabled() );
	}
}
