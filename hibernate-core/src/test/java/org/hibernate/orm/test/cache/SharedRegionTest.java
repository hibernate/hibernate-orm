/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 */

@DomainModel(
		annotatedClasses = {
				SharedRegionTest.StateCodes.class,
				SharedRegionTest.ZipCodes.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = Environment.CACHE_REGION_FACTORY,
						value = "org.hibernate.testing.cache.CachingRegionFactory"),
		}
)
@SessionFactory
public class SharedRegionTest {

	@Test
	public void test(SessionFactoryScope scope) {
		// create a StateCodes
		scope.inTransaction( s -> {
			s.persist( new StateCodes( 1 ) );
		} );

		// now try to load a ZipCodes using the same id : should just return null rather than blow up :)
		scope.inTransaction( s -> {
			ZipCodes zc = s.find( ZipCodes.class, 1 );
			assertNull( zc );
		} );

		scope.inTransaction( s -> {
			s.find( ZipCodes.class, 1 );
		} );
	}

	@Entity(name = "StateCodes")
	@Cache(region = "com.acme.referenceData", usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class StateCodes {
		@Id
		public Integer id;

		public StateCodes() {
		}

		public StateCodes(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "ZipCodes")
	@Cache(region = "com.acme.referenceData", usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class ZipCodes {
		@Id
		public Integer id;
	}
}
