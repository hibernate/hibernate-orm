/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;

import org.junit.After;
import org.junit.Before;

import org.hibernate.testing.junit4.BaseFunctionalTestCase;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * A base class for all ejb tests.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class BaseEntityManagerFunctionalTestCase extends BaseFunctionalTestCase {
	private static final Logger log = Logger.getLogger( BaseEntityManagerFunctionalTestCase.class );

	// IMPL NOTE : Here we use @Before and @After (instead of @BeforeClassOnce and @AfterClassOnce like we do in
	// BaseCoreFunctionalTestCase) because the old HEM test methodology was to create an EMF for each test method.


	private StandardServiceRegistryImpl serviceRegistry;
	private EntityManagerFactoryImpl entityManagerFactory;
	private EntityManager em;
	private ArrayList<EntityManager> isolatedEms = new ArrayList<EntityManager>();


	protected EntityManagerFactory entityManagerFactory() {
		return entityManagerFactory;
	}

	protected StandardServiceRegistryImpl serviceRegistry() {
		return serviceRegistry;
	}

	@Before
	@SuppressWarnings({ "UnusedDeclaration" })
	public void buildEntityManagerFactory() throws Exception {
		log.trace( "Building EntityManagerFactory" );
		EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				buildSettings()
		);
		entityManagerFactory = (EntityManagerFactoryImpl) entityManagerFactoryBuilder.build();

		serviceRegistry = (StandardServiceRegistryImpl) entityManagerFactory.getSessionFactory()
				.getServiceRegistry()
				.getParentServiceRegistry();
		configuration = entityManagerFactoryBuilder.getHibernateConfiguration();
		afterEntityManagerFactoryBuilt();
	}

	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	public static class TestingPersistenceUnitDescriptorImpl implements PersistenceUnitDescriptor {
		private final String name;

		public TestingPersistenceUnitDescriptorImpl(String name) {
			this.name = name;
		}

		@Override
		public URL getPersistenceUnitRootUrl() {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getProviderClassName() {
			return HibernatePersistenceProvider.class.getName();
		}

		@Override
		public boolean isUseQuotedIdentifiers() {
			return false;
		}

		@Override
		public boolean isExcludeUnlistedClasses() {
			return false;
		}

		@Override
		public PersistenceUnitTransactionType getTransactionType() {
			return null;
		}

		@Override
		public ValidationMode getValidationMode() {
			return null;
		}

		@Override
		public SharedCacheMode getSharedCacheMode() {
			return null;
		}

		@Override
		public List<String> getManagedClassNames() {
			return null;
		}

		@Override
		public List<String> getMappingFileNames() {
			return null;
		}

		@Override
		public List<URL> getJarFileUrls() {
			return null;
		}

		@Override
		public Object getNonJtaDataSource() {
			return null;
		}

		@Override
		public Object getJtaDataSource() {
			return null;
		}

		@Override
		public Properties getProperties() {
			return null;
		}

		@Override
		public ClassLoader getClassLoader() {
			return null;
		}

		@Override
		public void pushClassTransformer(List<String> entityClassNames) {
		}
	}

	@SuppressWarnings("unchecked")
	protected Map buildSettings() {

		Map settings = Environment.getProperties();
		addConfigOptions( settings );
		addMappings( settings );

		if ( createSchema() ) {
			settings.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		}
		settings.put( org.hibernate.cfg.AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		settings.put( org.hibernate.cfg.AvailableSettings.DIALECT, getDialect().getClass().getName() );
		return settings;
	}

	@SuppressWarnings("unchecked")
	protected void addMappings(Map settings) {
		String[] mappings = getMappings();
		if ( mappings != null && mappings.length > 0 ) {
			settings.put( AvailableSettings.HBXML_FILES, StringHelper.join( ",", mappings ) );
		}
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null && annotatedClasses.length > 0 ) {
			List<Class<?>> classes = Arrays.asList( annotatedClasses );
			settings.put( AvailableSettings.LOADED_CLASSES, classes );
		}

		Map<Class, String> cachedClasses = getCachedClasses();
		if ( CollectionHelper.isNotEmpty( cachedClasses ) ) {
			for ( Map.Entry<Class, String> entry : cachedClasses.entrySet() ) {
				settings.put( AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(), entry.getValue() );
			}
		}
		Map<String, String> cachedCollections = getCachedCollections();
		if ( CollectionHelper.isNotEmpty( cachedCollections ) ) {
			for ( Map.Entry<String, String> entry : cachedCollections.entrySet() ) {
				settings.put( AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(), entry.getValue() );
			}
		}

		String[] dds = getEjb3DD();
		if ( dds != null && dds.length > 0 ) {
			List<String> list = Arrays.asList( dds );
			settings.put( AvailableSettings.XML_FILE_NAMES, list );
		}
	}

	protected void addConfigOptions(Map options) {
	}

	public Map<Class, String> getCachedClasses() {
		return Collections.emptyMap();
	}

	public Map<String, String> getCachedCollections() {
		return Collections.emptyMap();
	}

	public String[] getEjb3DD() {
		return NO_MAPPINGS;
	}

	protected void afterEntityManagerFactoryBuilt() {
	}


	@After
	@SuppressWarnings( {"UnusedDeclaration"})
	public void releaseResources() {
		try {
			releaseUnclosedEntityManagers();
		}
		finally {
			if ( entityManagerFactory != null && entityManagerFactory.isOpen()) {
				entityManagerFactory.close();
			}
		}
		// Note we don't destroy the service registry as we are not the ones creating it
	}

	private void releaseUnclosedEntityManagers() {
		releaseUnclosedEntityManager( this.em );

		for ( EntityManager isolatedEm : isolatedEms ) {
			releaseUnclosedEntityManager( isolatedEm );
		}
	}

	private void releaseUnclosedEntityManager(EntityManager em) {
		if ( em == null ) {
			return;
		}
		if ( em.getTransaction().isActive() ) {
			em.getTransaction().rollback();
            log.warn("You left an open transaction! Fix your test case. For now, we are closing it for you.");
		}
		if ( em.isOpen() ) {
			// as we open an EM before the test runs, it will still be open if the test uses a custom EM.
			// or, the person may have forgotten to close. So, do not raise a "fail", but log the fact.
			em.close();
            log.warn("The EntityManager is not closed. Closing it.");
		}
	}

	protected EntityManager getOrCreateEntityManager() {
		if ( em == null || !em.isOpen() ) {
			em = entityManagerFactory.createEntityManager();
		}
		return em;
	}

	protected EntityManager createIsolatedEntityManager() {
		EntityManager isolatedEm = entityManagerFactory.createEntityManager();
		isolatedEms.add( isolatedEm );
		return isolatedEm;
	}

	protected EntityManager createIsolatedEntityManager(Map props) {
		EntityManager isolatedEm = entityManagerFactory.createEntityManager(props);
		isolatedEms.add( isolatedEm );
		return isolatedEm;
	}

	protected EntityManager createEntityManager(Map properties) {
		// always reopen a new EM and close the existing one
		if ( em != null && em.isOpen() ) {
			em.close();
		}
		em = entityManagerFactory.createEntityManager( properties );
		return em;
	}
}
