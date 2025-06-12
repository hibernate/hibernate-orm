/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.persistence.EntityManager;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.PersistenceUnitTransactionType;

import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;

import static java.util.Collections.emptyMap;

/**
 * A base class for all ejb tests.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class BaseEntityManagerFunctionalTestCase extends BaseUnitTestCase {

	// IMPL NOTE : Here we use @Before and @After (instead of @BeforeClassOnce and @AfterClassOnce like we do in
	// BaseCoreFunctionalTestCase) because the old HEM test methodology was to create an EMF for each test method.

	private static final Dialect dialect = DialectContext.getDialect();

	private StandardServiceRegistryImpl serviceRegistry;
	private SessionFactoryImplementor entityManagerFactory;

	private EntityManager entityManager;
	private final List<EntityManager> isolatedEntityManagers = new ArrayList<>();

	protected Dialect getDialect() {
		return dialect;
	}

	protected SessionFactoryImplementor entityManagerFactory() {
		return entityManagerFactory;
	}

	protected StandardServiceRegistryImpl serviceRegistry() {
		return serviceRegistry;
	}

	@Before
	public void buildEntityManagerFactory() {
		log.trace( "Building EntityManagerFactory" );
		final EntityManagerFactoryBuilder entityManagerFactoryBuilder =
				Bootstrap.getEntityManagerFactoryBuilder( buildPersistenceUnitDescriptor(), buildSettings() );
		applyMetadataImplementor( entityManagerFactoryBuilder.metadata() );
		entityManagerFactory = entityManagerFactoryBuilder.build().unwrap( SessionFactoryImplementor.class );
		serviceRegistry = (StandardServiceRegistryImpl)
				entityManagerFactory.getServiceRegistry().getParentServiceRegistry();
		afterEntityManagerFactoryBuilt();
	}

	protected void applyMetadataImplementor(MetadataImplementor metadataImplementor) {
	}

	protected PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
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
		public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
			return null;
		}

		@Override @SuppressWarnings("removal")
		public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType() {
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
		public ClassLoader getTempClassLoader() {
			return null;
		}

		@Override
		public void pushClassTransformer(EnhancementContext enhancementContext) {
		}

		@Override
		public ClassTransformer getClassTransformer() {
			return null;
		}
	}

	protected Map<Object,Object> buildSettings() {
		final Map<Object,Object> settings = getConfig();
		addMappings( settings );
		if ( createSchema() ) {
			settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		}
		ServiceRegistryUtil.applySettings( settings );
		settings.put( AvailableSettings.DIALECT, getDialect().getClass().getName() );
		return settings;
	}

	protected void addMappings(Map<Object,Object> settings) {
		final String[] mappings = getMappings();
		if ( mappings != null ) {
			settings.put( AvailableSettings.HBM_XML_FILES, String.join( ",", mappings ) );
		}
	}

	protected static final String[] NO_MAPPINGS = new String[0];

	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	protected Map<Object,Object> getConfig() {
		final Map<Object, Object> config = Environment.getProperties();

		config.put( AvailableSettings.CLASSLOADERS, getClass().getClassLoader() );

		List<Class<?>> classes = new ArrayList<>( Arrays.asList( getAnnotatedClasses() ) );
		config.put( AvailableSettings.LOADED_CLASSES, classes );
		for ( Map.Entry<Class<?>, String> entry : getCachedClasses().entrySet() ) {
			config.put( AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(), entry.getValue() );
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			config.put( AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(), entry.getValue() );
		}
		if ( getEjb3DD().length > 0 ) {
			config.put( AvailableSettings.ORM_XML_FILES,
					new ArrayList<>( Arrays.asList( getEjb3DD() ) ) );
		}

		config.put( PersistentTableStrategy.DROP_ID_TABLES, "true" );
		config.put( GlobalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		config.put( LocalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		ServiceRegistryUtil.applySettings( config );
		addConfigOptions( config );
		return config;
	}

	protected void addConfigOptions(Map<Object,Object> options) {
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	public Map<Class<?>, String> getCachedClasses() {
		return new HashMap<>();
	}

	public Map<String, String> getCachedCollections() {
		return new HashMap<>();
	}

	public String[] getEjb3DD() {
		return new String[] { };
	}

	protected void afterEntityManagerFactoryBuilt() {
	}

	protected boolean createSchema() {
		return true;
	}


	@After
	@SuppressWarnings("unused")
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
		releaseUnclosedEntityManager( this.entityManager);

		for ( EntityManager isolatedEm : isolatedEntityManagers) {
			releaseUnclosedEntityManager( isolatedEm );
		}
	}

	private void releaseUnclosedEntityManager(EntityManager em) {
		if ( em == null ) {
			return;
		}
		try {
			if ( !em.isOpen() ) {
				return;
			}
			try {
				if ( em.getTransaction().isActive() ) {
					log.warn( "You left an open transaction! Fix your test case. For now, we are closing it for you." );
					em.getTransaction().rollback();
				}
			}
			finally {
				if ( em.isOpen() ) {
					// as we open an EM before the test runs, it will still be open if the test uses a custom EM.
					// or, the person may have forgotten to close. So, do not raise a "fail", but log the fact.
					log.warn( "The EntityManager is not closed. Closing it." );
					em.close();
				}
			}
		}
		catch (RuntimeException e) {
			log.debug( "Ignoring exception during clean up", e );
		}
	}

	protected EntityManager getOrCreateEntityManager() {
		if ( entityManager == null || !entityManager.isOpen() ) {
			entityManager = entityManagerFactory.createEntityManager();
		}
		return entityManager;
	}

	protected EntityManager createIsolatedEntityManager() {
		final EntityManager isolatedEntityManager = entityManagerFactory.createEntityManager();
		isolatedEntityManagers.add( isolatedEntityManager );
		return isolatedEntityManager;
	}

	protected EntityManager createIsolatedEntityManager(Map<?,?> properties) {
		final EntityManager isolatedEntityManager = entityManagerFactory.createEntityManager( properties );
		isolatedEntityManagers.add( isolatedEntityManager );
		return isolatedEntityManager;
	}

	protected EntityManager createEntityManager() {
		return createEntityManager( emptyMap() );
	}

	protected EntityManager createEntityManager(Map<?,?> properties) {
		// always reopen a new EM and close the existing one
		if ( entityManager != null && entityManager.isOpen() ) {
			entityManager.close();
		}
		entityManager = entityManagerFactory.createEntityManager( properties );
		return entityManager;
	}
}
