/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FetchType;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.boot.pipeline.internal.SessionFactoryBootstrap;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Chris Cranford
 */
@FunctionalEntityManagerFactoryTesting
public class EntityManagerFactoryBasedFunctionalTest
		implements EntityManagerFactoryProducer, EntityManagerFactoryScopeContainer {

	private EntityManagerFactoryScope entityManagerFactoryScope;

	@Override
	public EntityManagerFactory produceEntityManagerFactory() {
		final EntityManagerFactory entityManagerFactory = SessionFactoryBootstrap.build(
				buildPersistenceUnitDescriptor(),
				buildSettings()
		);

		entityManagerFactoryBuilt( entityManagerFactory );

		return entityManagerFactory;
	}

	@Override
	public void injectEntityManagerFactoryScope(EntityManagerFactoryScope scope) {
		entityManagerFactoryScope = scope;
	}

	@Override
	public EntityManagerFactoryProducer getEntityManagerFactoryProducer() {
		return this;
	}

	protected EntityManagerFactoryScope entityManagerFactoryScope() {
		return entityManagerFactoryScope;
	}

	protected EntityManagerFactory entityManagerFactory() {
		return entityManagerFactoryScope.getEntityManagerFactory();
	}

	protected void entityManagerFactoryBuilt(EntityManagerFactory factory) {
	}

	protected boolean strictJpaCompliance() {
		return false;
	}

	protected boolean exportSchema() {
		return true;
	}

	protected void addConfigOptions(Map options) {
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected Map<String, Object> buildSettings() {
		Map<String, Object> settings = getConfig();
		settings.put( AvailableSettings.JPA_COMPLIANCE, strictJpaCompliance() );
		settings.put( AvailableSettings.JPA_QUERY_COMPLIANCE, strictJpaCompliance() );

		if ( exportSchema() ) {
			settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		}

		return settings;
	}

	@AfterAll
	public void afterAll() {
		if ( entityManagerFactoryScope != null ) {
			try {
				entityManagerFactoryScope.close();
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
	}


	protected Map<String, Object> getConfig() {
		Map<String, Object> config = PropertiesHelper.map( Environment.getProperties() );
		for ( Map.Entry<Class<?>, String> entry : getCachedClasses().entrySet() ) {
			config.put(
					AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(),
					entry.getValue()
			);
		}
		for ( var entry : getCachedCollections().entrySet() ) {
			config.put(
					AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(),
					entry.getValue()
			);
		}
		config.put( PersistentTableStrategy.DROP_ID_TABLES, "true" );
		config.put( GlobalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		config.put( LocalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		ServiceRegistryUtil.applySettings( config );
		addConfigOptions( config );
		return config;
	}

	public Map<Class<?>, String> getCachedClasses() {
		return new HashMap<>();
	}

	public Map<String, String> getCachedCollections() {
		return new HashMap<>();
	}

	public String[] getEjb3DD() {
		return new String[] {};
	}

	protected PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new TestingPersistenceUnitDescriptorImpl(
				getClass().getSimpleName(),
				Arrays.stream( getAnnotatedClasses() ).map( Class::getName ).toList(),
				Arrays.asList( getEjb3DD() )
		);
	}

	public static class TestingPersistenceUnitDescriptorImpl implements PersistenceUnitDescriptor {
		private final String name;
		private final List<String> managedClassNames;
		private final List<String> mappingFileNames;

		public TestingPersistenceUnitDescriptorImpl(String name) {
			this( name, List.of(), List.of() );
		}

		public TestingPersistenceUnitDescriptorImpl(
				String name,
				List<String> managedClassNames,
				List<String> mappingFileNames) {
			this.name = name;
			this.managedClassNames = List.copyOf( managedClassNames );
			this.mappingFileNames = List.copyOf( mappingFileNames );
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
		public FetchType getDefaultToOneFetchType() {
			return FetchType.EAGER;
		}

		@Override
		public PersistenceUnitTransactionType getPersistenceUnitTransactionType() {
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
			return managedClassNames;
		}

		@Override
		public List<String> getAllClassNames() {
			return getManagedClassNames();
		}

		@Override
		public List<String> getMappingFileNames() {
			return mappingFileNames;
		}

		@Override
		public List<URL> getJarFileUrls() {
			return List.of();
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
		public boolean isClassTransformerRegistrationDisabled() {
			return true;
		}

		@Override
		public ClassTransformer pushClassTransformer(EnhancementContext enhancementContext) {
			return null;
		}
	}

	@AfterEach
	public final void afterTest() {
		if ( isCleanupTestDataRequired() ) {
			cleanupTestData();
		}
	}

	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	/**
	 * Tests should ideally override this standard implementation; it may not work in all cases (e.g. with @Embeddable entities)
	 */
	protected void cleanupTestData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Arrays.stream(
					getAnnotatedClasses() ).forEach(
					annotatedClass ->
							entityManager.createQuery( "delete from " + annotatedClass
									.getSimpleName() ).executeUpdate()
			);
		} );
	}

	protected void inTransaction(Consumer<EntityManager> action) {
		entityManagerFactoryScope().inTransaction( action );
	}

	protected <T> T fromTransaction(Function<EntityManager, T> action) {
		return entityManagerFactoryScope().fromTransaction( action );
	}

	protected void inEntityManager(Consumer<EntityManager> action) {
		entityManagerFactoryScope().inEntityManager( action );
	}

	protected <T> T fromEntityManager(Function<EntityManager, T> action) {
		return entityManagerFactoryScope().fromEntityManager( action );
	}


}
