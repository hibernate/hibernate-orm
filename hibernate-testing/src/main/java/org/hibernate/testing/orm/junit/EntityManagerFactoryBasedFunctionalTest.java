/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;

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
		final EntityManagerFactory entityManagerFactory = Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				buildSettings()
		).build();

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

	protected static final String[] NO_MAPPINGS = new String[0];

	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	protected void addConfigOptions(Map options) {
	}

	protected static final Class<?>[] NO_CLASSES = new Class[0];

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected Map<Object, Object> buildSettings() {
		Map<Object, Object> settings = getConfig();
		applySettings( settings );

		if ( exportSchema() ) {
			settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		}

		return settings;
	}

	protected Map<Object, Object> getConfig() {
		Map<Object, Object> config = Environment.getProperties();
		ArrayList<Class<?>> classes = new ArrayList<>();

		classes.addAll( Arrays.asList( getAnnotatedClasses() ) );
		config.put( AvailableSettings.LOADED_CLASSES, classes );
		for ( Map.Entry<Class, String> entry : getCachedClasses().entrySet() ) {
			config.put(
					AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(),
					entry.getValue()
			);
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			config.put(
					AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(),
					entry.getValue()
			);
		}
		if ( getEjb3DD().length > 0 ) {
			ArrayList<String> dds = new ArrayList<>();
			dds.addAll( Arrays.asList( getEjb3DD() ) );
			config.put( AvailableSettings.ORM_XML_FILES, dds );
		}

		config.put( PersistentTableStrategy.DROP_ID_TABLES, "true" );
		config.put( GlobalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		config.put( LocalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		ServiceRegistryUtil.applySettings( config );
		addConfigOptions( config );
		return config;
	}

	protected void applySettings(Map<Object, Object> settings) {
		String[] mappings = getMappings();
		if ( mappings != null ) {
			settings.put( AvailableSettings.HBM_XML_FILES, String.join( ",", mappings ) );
		}
	}

	public Map<Class, String> getCachedClasses() {
		return new HashMap<>();
	}

	public Map<String, String> getCachedCollections() {
		return new HashMap<>();
	}

	public String[] getEjb3DD() {
		return new String[] {};
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

		@Override @SuppressWarnings("removal")
		public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType() {
			return null;
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
