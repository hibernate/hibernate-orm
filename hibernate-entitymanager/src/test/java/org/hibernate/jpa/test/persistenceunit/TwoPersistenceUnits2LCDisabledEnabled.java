/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.persistenceunit;

import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SharedCacheMode;

import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.hibernate.ejb.test.ejb3configuration.PersistenceUnitInfoAdapter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.BootstrapServiceRegistryBuilder;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
public class TwoPersistenceUnits2LCDisabledEnabled {

	@Test
	@TestForIssue( jiraKey = "HHH-11516" )
	public void testDisabledEnabled() {
		final Map<Object, Object> config = Environment.getProperties();
		config.put( AvailableSettings.CACHE_REGION_FACTORY, NoCachingRegionFactory.class.getName() );
		config.put( org.hibernate.ejb.AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE );
		config.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );

		testIt( config );

		config.put( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		config.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );

		testIt( config );
	}

	private void testIt(Map config) {
		Ejb3Configuration ejb3Configuration = new Ejb3Configuration();
		ejb3Configuration.addAnnotatedClass( AnEntity.class );
		ejb3Configuration.configure( new PersistenceUnitInfoAdapter(), config );
		EntityManagerFactoryImpl entityManagerFactory = (EntityManagerFactoryImpl) ejb3Configuration.buildEntityManagerFactory(
				new BootstrapServiceRegistryBuilder()
		);

		SessionFactoryImplementor sf = entityManagerFactory.getSessionFactory();
		final EntityPersister persister = sf.getEntityPersister( AnEntity.class.getName() );

		try {
			if ( config.get( AvailableSettings.USE_SECOND_LEVEL_CACHE ).equals( "true" ) ) {
				assertNotNull( persister.getCacheAccessStrategy() );
			}
			else {
				assertNull( persister.getCacheAccessStrategy() );
			}
		}
		finally {
			sf.close();
		}
	}


	@Cacheable
	@Entity( name = "AnEntity" )
	public static class AnEntity {
		@Id
		private Long id;
	}
}
