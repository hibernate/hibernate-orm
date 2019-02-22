/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Remove;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.boot.AuditService;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.junit.jupiter.api.Tag;

import org.hibernate.testing.junit5.StandardTags;
import org.hibernate.testing.junit5.dynamictests.DynamicAfterAll;
import org.hibernate.testing.junit5.dynamictests.DynamicAfterEach;
import org.hibernate.testing.junit5.envers.EnversEntityManagerFactoryProducer;
import org.hibernate.testing.junit5.envers.EnversEntityManagerFactoryScope;

/**
 * Envers base test case that uses a JPA {@link EntityManagerFactory}.
 *
 * @author Chris Cranford
 */
@Tag(StandardTags.ENVERS)
public class EnversEntityManagerFactoryBasedFunctionalTest
		extends AbstractEnversDynamicTest
		implements EnversEntityManagerFactoryProducer {

	private EnversEntityManagerFactoryScope entityManagerFactoryScope;
	private AuditReader auditReader;
	private EntityManager entityManager;

	protected EntityManagerFactory entityManagerFactory() {
		return entityManagerFactoryScope.getEntityManagerFactory();
	}

	@Override
	public EntityManagerFactory produceEntityManagerFactory(String auditStrategyName) {
		this.auditStrategyName = auditStrategyName;

		final Map<String, Object> settings = new HashMap<>();

		settings.put( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		settings.put( AvailableSettings.HBM2DDL_AUTO, exportSchema() ? "create-drop" : "none" );

		addSettings( settings );

		final Class<?>[] classes = getAnnotatedClasses();
		if ( classes != null && classes.length > 0 ) {
			settings.put( AvailableSettings.LOADED_CLASSES, Arrays.asList( getAnnotatedClasses() ) );
		}

		final String[] mappings = getMappings();
		if ( mappings != null && mappings.length > 0 ) {
			for ( int i = 0; i < mappings.length; ++i ) {
				if ( !mappings[ i ].startsWith( getBaseForMappings() ) ) {
					mappings[ i ] = getBaseForMappings() + mappings[ i ];
				}
			}
			settings.put( AvailableSettings.HBXML_FILES, String.join( ",", mappings ) );
		}

		return Bootstrap.getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), settings ).build();
	}

	@DynamicAfterEach
	public void releaseResources() {
		if ( auditReader != null ) {
			auditReader.close();
			auditReader = null;
		}

		if ( entityManager != null ) {
			entityManager.close();
			entityManager = null;
		}
	}

	@DynamicAfterAll
	public void releaseEntityManagerFactory() {
		entityManagerFactoryScope.releaseEntityManagerFactory();
	}

	@Override
	protected void injectExecutionContext(EnversDynamicExecutionContext context) {
		entityManagerFactoryScope = new EnversEntityManagerFactoryScope( this, context.getStrategy() );
	}

	@Override
	protected Dialect getDialect() {
		return entityManagerFactoryScope().getDialect();
	}

	protected EnversEntityManagerFactoryScope entityManagerFactoryScope() {
		return entityManagerFactoryScope;
	}

	protected EntityManager openEntityManager() {
		return entityManagerFactoryScope.getEntityManagerFactory().createEntityManager();
	}

	@Remove
	@Deprecated
	protected EntityManager getEntityManager() {
		return getOrCreateEntityManager();
	}

	@Remove
	@Deprecated
	protected EntityManager getOrCreateEntityManager() {
		if ( entityManager == null ) {
			entityManager = entityManagerFactoryScope.getEntityManagerFactory().createEntityManager();
		}
		return entityManager;
	}

	protected AuditReader getAuditReader() {
		if ( auditReader == null ) {
			auditReader = entityManagerFactoryScope.getEntityManagerFactory()
					.unwrap( SessionFactory.class )
					.openAuditReader();
		}
		return auditReader;
	}

	protected MetamodelImplementor getMetamodel() {
		return entityManagerFactoryScope.getEntityManagerFactory()
				.unwrap( SessionFactoryImplementor.class )
				.getMetamodel();
	}

	protected void inJPA(Consumer<EntityManager> action) {
		entityManagerFactoryScope().inJPA( action );
	}

	protected <R> R inJPA(Function<EntityManager, R> action) {
		return entityManagerFactoryScope().inJPA( action );
	}

	protected void inTransaction(Consumer<EntityManager> action) {
		entityManagerFactoryScope().inTransaction( action );
	}

	protected <R> R inTransaction(Function<EntityManager, R> action) {
		return entityManagerFactoryScope().inTransaction( action );
	}

	@SafeVarargs
	protected final void inTransactions(Consumer<EntityManager>... actions) {
		entityManagerFactoryScope().inTransactions( actions );
	}

	protected AuditService getAuditService() {
		return entityManagerFactoryScope.getEntityManagerFactory()
				.unwrap( SessionFactoryImplementor.class )
				.getServiceRegistry()
				.getService( AuditService.class );
	}

	private class PersistenceUnitDescriptorAdapter implements PersistenceUnitDescriptor {
		private final String name = "persistenceUnitDescriptorAdapter@" + System.identityHashCode( this );
		private Properties properties;

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
			return Collections.emptyList();
		}

		@Override
		public List<String> getMappingFileNames() {
			return Collections.emptyList();
		}

		@Override
		public List<URL> getJarFileUrls() {
			return Collections.emptyList();
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
			if ( properties == null ) {
				properties = new Properties();
			}
			return properties;
		}

		@Override
		public ClassLoader getClassLoader() {
			return Thread.currentThread().getContextClassLoader();
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
