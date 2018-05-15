/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.orm.test.BaseUnitTest;

import org.jboss.logging.Logger;

import org.hibernate.testing.junit5.EntityManagerFactoryProducer;
import org.hibernate.testing.junit5.EntityManagerFactoryScope;
import org.hibernate.testing.junit5.EntityManagerFactoryScopeContainer;
import org.hibernate.testing.junit5.FunctionalEntityManagerFactoryTesting;

/**
 * @author Chris Cranford
 */
@FunctionalEntityManagerFactoryTesting
public class EntityManagerFactoryBasedFunctionalTest
		extends BaseUnitTest
		implements EntityManagerFactoryProducer, EntityManagerFactoryScopeContainer {
	private static final Logger log = Logger.getLogger( EntityManagerFactoryBasedFunctionalTest.class );

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

	protected void applySettings(Map<String, Object> settings) {
	}

	protected boolean strictJpaCompliance() {
		return false;
	}

	protected boolean exportSchema() {
		return false;
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
		addMappings( settings );

		if ( exportSchema() ) {
			settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		}

		settings.put( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );

		return settings;
	}

	protected Map<Object, Object> getConfig() {
		Map<Object, Object> config = Environment.getProperties();
		ArrayList<Class<?>> classes = new ArrayList<>();

		classes.addAll( Arrays.asList( getAnnotatedClasses() ) );
		config.put( org.hibernate.jpa.AvailableSettings.LOADED_CLASSES, classes );
		for ( Map.Entry<Class, String> entry : getCachedClasses().entrySet() ) {
			config.put( org.hibernate.jpa.AvailableSettings.CLASS_CACHE_PREFIX + "." + entry.getKey().getName(), entry.getValue() );
		}
		for ( Map.Entry<String, String> entry : getCachedCollections().entrySet() ) {
			config.put( org.hibernate.jpa.AvailableSettings.COLLECTION_CACHE_PREFIX + "." + entry.getKey(), entry.getValue() );
		}
		if ( getEjb3DD().length > 0 ) {
			ArrayList<String> dds = new ArrayList<>();
			dds.addAll( Arrays.asList( getEjb3DD() ) );
			config.put( org.hibernate.jpa.AvailableSettings.XML_FILE_NAMES, dds );
		}

		addConfigOptions( config );
		return config;
	}

	protected void addMappings(Map<Object, Object> settings) {
		String[] mappings = getMappings();
		if ( mappings != null ) {
			settings.put( org.hibernate.jpa.AvailableSettings.HBXML_FILES, String.join( ",", mappings ) );
		}
	}

	public Map<Class, String> getCachedClasses() {
		return new HashMap<Class, String>();
	}

	public Map<String, String> getCachedCollections() {
		return new HashMap<String, String>();
	}

	public String[] getEjb3DD() {
		return new String[] { };
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
		public ClassLoader getTempClassLoader() {
			return null;
		}

		@Override
		public void pushClassTransformer(EnhancementContext enhancementContext) {
		}
	}
}
