/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 */
public class SharedRegionTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, true );
		ssrb.applySetting( AvailableSettings.CACHE_REGION_FACTORY, new CachingRegionFactory() );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( StateCodes.class );
		sources.addAnnotatedClass( ZipCodes.class );
	}

	@Test
	public void test() {
		// create a StateCodes
		Session s = openSession();
		s.beginTransaction();
		s.save( new StateCodes( 1 ) );
		s.getTransaction().commit();
		s.close();

		// now try to load a ZipCodes using the same id : should just return null rather than blow up :)
		s = openSession();
		s.beginTransaction();
		ZipCodes zc = s.find( ZipCodes.class, 1 );
		assertNull( zc );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.find( ZipCodes.class, 1 );
		s.getTransaction().commit();
		s.close();
	}

	@Entity( name="StateCodes" )
	@Cache( region="com.acme.referenceData", usage = CacheConcurrencyStrategy.READ_WRITE )
	public static class StateCodes {
		@Id
		public Integer id;

		public StateCodes() {
		}

		public StateCodes(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "ZipCodes" )
	@Cache( region="com.acme.referenceData", usage = CacheConcurrencyStrategy.READ_WRITE )
	public static class ZipCodes {
		@Id
		public Integer id;
	}
}
