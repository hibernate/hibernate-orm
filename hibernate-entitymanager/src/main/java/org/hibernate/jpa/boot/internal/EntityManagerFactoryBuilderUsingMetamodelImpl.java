/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.boot.internal;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.jboss.logging.Logger;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.spi.CacheRegionDefinition;
import org.hibernate.boot.spi.JaccDefinition;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.JpaUnifiedSettingsBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.Settings;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.jpa.internal.util.LogHelper;
import org.hibernate.jpa.internal.util.PersistenceUnitTransactionTypeHelper;
import org.hibernate.jpa.internal.util.SharedCacheModeHelper;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.MetadataSourceProcessingOrder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.BootstrapServiceRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.hibernate.internal.jaxb.cfg.JaxbHibernateConfiguration.JaxbSessionFactory.JaxbMapping;
import static org.hibernate.jpa.boot.spi.JpaBootstrapServiceRegistryBuilder.buildBootstrapServiceRegistry;
import static org.hibernate.jpa.boot.spi.JpaUnifiedSettingsBuilder.CfgXmlMappingArtifacts;

/**
 * This will eventually replace {@link EntityManagerFactoryBuilderImpl}
 *
 * @author Steve Ebersole
 *
 * @deprecated This class will go away before 5.0 even goes alpha and its functionality will replace that in
 * {@link EntityManagerFactoryBuilderImpl}.
 */
@Deprecated
public class EntityManagerFactoryBuilderUsingMetamodelImpl implements EntityManagerFactoryBuilder {
	private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(
			EntityManagerMessageLogger.class,
			EntityManagerFactoryBuilderImpl.class.getName()
	);

	private final PersistenceUnitDescriptor persistenceUnit;
	private final Map<?,?> configurationValues;

	private final BootstrapServiceRegistry bootstrapServiceRegistry;
	private final MetadataSources metadataSources;

	private final List<JaccDefinition> jaccDefinitions = new ArrayList<JaccDefinition>();	// todo : see HHH-7462
	private final List<CacheRegionDefinition> cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();

	public EntityManagerFactoryBuilderUsingMetamodelImpl(
			PersistenceUnitDescriptor persistenceUnit,
			Map integrationSettings) {
		LogHelper.logPersistenceUnitInformation( persistenceUnit );

		this.persistenceUnit = persistenceUnit;
		if ( integrationSettings == null ) {
			integrationSettings = Collections.emptyMap();
		}

		// build the boot-strap service registry, which mainly handles class loader interactions
		this.bootstrapServiceRegistry = buildBootstrapServiceRegistry(
				persistenceUnit,
				integrationSettings
		);

		final JpaUnifiedSettingsBuilder.Result mergedResult = JpaUnifiedSettingsBuilder.mergePropertySources(
				persistenceUnit,
				integrationSettings,
				bootstrapServiceRegistry
		);

		final CfgXmlMappingArtifacts cfgXmlMappingArtifacts = mergedResult.getCfgXmlMappingArtifacts();
		this.configurationValues = mergedResult.getSettings();

		// todo : add scanning...

		this.metadataSources = new MetadataSources( bootstrapServiceRegistry );
		for ( JaxbMapping jaxbMapping : cfgXmlMappingArtifacts.getMappings() ) {
			if ( jaxbMapping.getClazz() != null ) {
				metadataSources.addAnnotatedClassName( jaxbMapping.getClazz() );
			}
			else if ( jaxbMapping.getResource() != null ) {
				metadataSources.addResource( jaxbMapping.getResource() );
			}
			else if ( jaxbMapping.getJar() != null ) {
				metadataSources.addJar( new File( jaxbMapping.getJar() ) );
			}
			else if ( jaxbMapping.getPackage() != null ) {
				metadataSources.addPackage( jaxbMapping.getPackage() );
			}
		}

		// todo : add results of scanning to the MetadataSources

		metadataSources.addCacheRegionDefinitions( cacheRegionDefinitions );
	}

	@Override
	public void cancel() {
		// currently nothing to do...
	}

	@Override
	public EntityManagerFactory buildEntityManagerFactory() {
		final ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder( bootstrapServiceRegistry );
		final SpecialProperties specialProperties = processProperties( serviceRegistryBuilder );

		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
		prepareMetadataBuilder( metadataBuilder, specialProperties );
		final Metadata metadata = metadataBuilder.buildMetadata();

		final SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
		prepareSessionFactoryBuilder( sessionFactoryBuilder, specialProperties );
		sessionFactoryBuilder.add( new ServiceRegistryCloser() );
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) sessionFactoryBuilder.buildSessionFactory();

		final Settings emfCreationSettings = prepareEntitytManagerFactoryCreationSettings( specialProperties );

		// IMPL NOTE : the last param (passed as null) is the Configuration which we pass in at the moment solely to
		// get access to the mapping information in order to build the JPA javax.persistence.metamodel.Metamodel
		// We need to change that to leverage the new Hibernate Metadata metamodel package anyway..

		return new EntityManagerFactoryImpl(
				persistenceUnit.getName(),
				sessionFactory,
				(SettingsImpl) emfCreationSettings,
				configurationValues,
				null
		);
	}

	private SpecialProperties processProperties(ServiceRegistryBuilder serviceRegistryBuilder) {
		final SpecialProperties specialProperties = new SpecialProperties();

		applyJdbcConnectionProperties( serviceRegistryBuilder );
		applyTransactionProperties( serviceRegistryBuilder, specialProperties );

		final Object validationFactory = configurationValues.get( AvailableSettings.VALIDATION_FACTORY );
		if ( validationFactory != null ) {
			BeanValidationIntegrator.validateFactory( validationFactory );
		}

		// flush before completion validation
		if ( "true".equals( configurationValues.get( Environment.FLUSH_BEFORE_COMPLETION ) ) ) {
			serviceRegistryBuilder.applySetting( Environment.FLUSH_BEFORE_COMPLETION, "false" );
			LOG.definingFlushBeforeCompletionIgnoredInHem( Environment.FLUSH_BEFORE_COMPLETION );
		}

		for ( Map.Entry entry : configurationValues.entrySet() ) {
			if ( entry.getKey() instanceof String ) {
				final String keyString = (String) entry.getKey();

				//noinspection deprecation
				if ( AvailableSettings.INTERCEPTOR.equals( keyString )
						|| org.hibernate.cfg.AvailableSettings.INTERCEPTOR.equals( keyString ) ) {
					specialProperties.sessionFactoryInterceptor = instantiateCustomClassFromConfiguration(
							entry.getValue(),
							Interceptor.class,
							bootstrapServiceRegistry
					);
				}
				else if ( AvailableSettings.SESSION_INTERCEPTOR.equals( keyString ) ) {
					specialProperties.sessionInterceptorClass = loadSessionInterceptorClass(
							entry.getValue(),
							bootstrapServiceRegistry
					);
				}
				else if ( AvailableSettings.NAMING_STRATEGY.equals( keyString ) ) {
					specialProperties.namingStrategy = instantiateCustomClassFromConfiguration(
							entry.getValue(),
							NamingStrategy.class,
							bootstrapServiceRegistry
					);
				}
				else if ( AvailableSettings.SESSION_FACTORY_OBSERVER.equals( keyString ) ) {
					specialProperties.sessionFactoryObserver = instantiateCustomClassFromConfiguration(
							entry.getValue(),
							SessionFactoryObserver.class,
							bootstrapServiceRegistry
					);
				}
				else if ( org.hibernate.cfg.AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY.equals( keyString ) ) {
					specialProperties.customEntityDirtinessStrategy = instantiateCustomClassFromConfiguration(
							entry.getValue(),
							CustomEntityDirtinessStrategy.class,
							bootstrapServiceRegistry
					);
				}
				else if ( org.hibernate.cfg.AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER.equals( keyString ) ) {
					specialProperties.currentTenantIdentifierResolver = instantiateCustomClassFromConfiguration(
							entry.getValue(),
							CurrentTenantIdentifierResolver.class,
							bootstrapServiceRegistry
					);
				}
				else if ( AvailableSettings.DISCARD_PC_ON_CLOSE.equals( keyString ) ) {
					specialProperties.releaseResourcesOnClose = ( "true".equals( entry.getValue() ) );
				}
				else if ( AvailableSettings.SHARED_CACHE_MODE.equals( keyString ) ) {
					specialProperties.sharedCacheMode = SharedCacheModeHelper.asSharedCacheMode( entry.getValue() );
				}
				else if ( org.hibernate.cfg.AvailableSettings.METADATA_PROCESSING_ORDER.equals( keyString ) ) {
					specialProperties.sourceProcessingOrder = interpretSourceProcessingOrder( entry.getValue() );
				}
				else if ( org.hibernate.cfg.AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY.equals( keyString ) ) {
					specialProperties.defaultCacheAccessType = interpretCacheAccessStrategy( entry.getValue() );
				}
				else if ( org.hibernate.cfg.AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS.equals( keyString ) ) {
					specialProperties.useEnhancedGenerators = ConfigurationHelper.asBoolean( entry.getValue() );
				}
				else if ( keyString.startsWith( AvailableSettings.CLASS_CACHE_PREFIX ) ) {
					addCacheRegionDefinition(
							keyString.substring( AvailableSettings.CLASS_CACHE_PREFIX.length() + 1 ),
							(String) entry.getValue(),
							CacheRegionDefinition.CacheRegionType.ENTITY
					);
				}
				else if ( keyString.startsWith( AvailableSettings.COLLECTION_CACHE_PREFIX ) ) {
					addCacheRegionDefinition(
							keyString.substring( AvailableSettings.COLLECTION_CACHE_PREFIX.length() + 1 ),
							(String) entry.getValue(),
							CacheRegionDefinition.CacheRegionType.COLLECTION
					);
				}
				else if ( keyString.startsWith( AvailableSettings.JACC_PREFIX )
						&& ! ( keyString.equals( AvailableSettings.JACC_CONTEXT_ID )
						|| keyString.equals( AvailableSettings.JACC_ENABLED ) ) ) {
					addJaccDefinition( (String) entry.getKey(), entry.getValue() );
				}
			}

		}

		return specialProperties;
	}

	private void applyJdbcConnectionProperties(ServiceRegistryBuilder serviceRegistryBuilder) {
		if ( persistenceUnit.getJtaDataSource() != null ) {
			serviceRegistryBuilder.applySetting( Environment.DATASOURCE, persistenceUnit.getJtaDataSource() );
		}
		else if ( persistenceUnit.getNonJtaDataSource() != null ) {
			serviceRegistryBuilder.applySetting( Environment.DATASOURCE, persistenceUnit.getNonJtaDataSource() );
		}
		else {
			final String driver = (String) configurationValues.get( AvailableSettings.JDBC_DRIVER );
			if ( StringHelper.isNotEmpty( driver ) ) {
				serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.DRIVER, driver );
			}
			final String url = (String) configurationValues.get( AvailableSettings.JDBC_URL );
			if ( StringHelper.isNotEmpty( url ) ) {
				serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.URL, url );
			}
			final String user = (String) configurationValues.get( AvailableSettings.JDBC_USER );
			if ( StringHelper.isNotEmpty( user ) ) {
				serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.USER, user );
			}
			final String pass = (String) configurationValues.get( AvailableSettings.JDBC_PASSWORD );
			if ( StringHelper.isNotEmpty( pass ) ) {
				serviceRegistryBuilder.applySetting( org.hibernate.cfg.AvailableSettings.PASS, pass );
			}
		}
	}

	private void applyTransactionProperties(ServiceRegistryBuilder serviceRegistryBuilder, SpecialProperties specialProperties) {
		PersistenceUnitTransactionType txnType = PersistenceUnitTransactionTypeHelper.interpretTransactionType(
				configurationValues.get( AvailableSettings.TRANSACTION_TYPE )
		);
		if ( txnType == null ) {
			txnType = persistenceUnit.getTransactionType();
		}
		if ( txnType == null ) {
			// is it more appropriate to have this be based on bootstrap entry point (EE vs SE)?
			txnType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		specialProperties.jpaTransactionType = txnType;
		boolean hasTxStrategy = configurationValues.containsKey( Environment.TRANSACTION_STRATEGY );
		if ( hasTxStrategy ) {
			LOG.overridingTransactionStrategyDangerous( Environment.TRANSACTION_STRATEGY );
		}
		else {
			if ( txnType == PersistenceUnitTransactionType.JTA ) {
				serviceRegistryBuilder.applySetting( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class );
			}
			else if ( txnType == PersistenceUnitTransactionType.RESOURCE_LOCAL ) {
				serviceRegistryBuilder.applySetting( Environment.TRANSACTION_STRATEGY, JdbcTransactionFactory.class );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T instantiateCustomClassFromConfiguration(
			Object value,
			Class<T> type,
			ServiceRegistry bootstrapServiceRegistry) {
		if ( value == null ) {
			return null;
		}

		if ( type.isInstance( value ) ) {
			return (T) value;
		}

		final Class<? extends T> implementationClass;

		if ( Class.class.isInstance( value ) ) {
			try {
				implementationClass = (Class<? extends T>) value;
			}
			catch (ClassCastException e) {
				throw persistenceException(
						String.format(
								"Specified implementation class [%s] was not of expected type [%s]",
								((Class) value).getName(),
								type.getName()
						)
				);
			}
		}
		else {
			final String implementationClassName = value.toString();
			try {
				implementationClass = bootstrapServiceRegistry.getService( ClassLoaderService.class )
						.classForName( implementationClassName );
			}
			catch (ClassCastException e) {
				throw persistenceException(
						String.format(
								"Specified implementation class [%s] was not of expected type [%s]",
								implementationClassName,
								type.getName()
						)
				);
			}
		}

		try {
			return implementationClass.newInstance();
		}
		catch (Exception e) {
			throw persistenceException(
					String.format(
							"Unable to instantiate specified implementation class [%s]",
							implementationClass.getName()
					),
					e
			);
		}
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Interceptor> loadSessionInterceptorClass(
			Object value,
			BootstrapServiceRegistry bootstrapServiceRegistry) {
		if ( value == null ) {
			return null;
		}

		Class theClass;
		if ( Class.class.isInstance( value ) ) {
			theClass = (Class) value;
		}
		else {
			theClass = bootstrapServiceRegistry.getService( ClassLoaderService.class ).classForName( value.toString() );
		}

		try {
			return (Class<? extends Interceptor>) theClass;
		}
		catch (ClassCastException e) {
			throw persistenceException(
					String.format(
							"Specified Interceptor implementation class [%s] was not castable to Interceptor",
							theClass.getName()
					)
			);
		}
	}

	private MetadataSourceProcessingOrder interpretSourceProcessingOrder(Object value) {
		if ( value == null ) {
			return null;
		}

		if ( MetadataSourceProcessingOrder.class.isInstance( value ) ) {
			return (MetadataSourceProcessingOrder) value;
		}
		else {
			final String s = value.toString();
			final StringTokenizer tokenizer = new StringTokenizer( s, ",; ", false );
			final MetadataSourceType metadataSourceType = MetadataSourceType.parsePrecedence( tokenizer.nextToken() );
			return metadataSourceType == MetadataSourceType.CLASS
					? MetadataSourceProcessingOrder.ANNOTATIONS_FIRST
					: MetadataSourceProcessingOrder.HBM_FIRST;
		}
	}

	private AccessType interpretCacheAccessStrategy(Object value) {
		if ( value == null ) {
			return null;
		}

		if ( AccessType.class.isInstance( value ) ) {
			return (AccessType) value;
		}
		else {
			return AccessType.fromExternalName( value.toString() );
		}
	}

	private String jaccContextId;

	private void addJaccDefinition(String key, Object value) {
		if ( jaccContextId == null ) {
			jaccContextId = (String) configurationValues.get( AvailableSettings.JACC_CONTEXT_ID );
			if ( jaccContextId == null ) {
				throw persistenceException(
						"Entities have been configured for JACC, but "
								+ AvailableSettings.JACC_CONTEXT_ID + " has not been set"
				);
			}
		}

		try {
			final int roleStart = AvailableSettings.JACC_PREFIX.length() + 1;
			final String role = key.substring( roleStart, key.indexOf( '.', roleStart ) );
			final int classStart = roleStart + role.length() + 1;
			final String clazz = key.substring( classStart, key.length() );

			final JaccDefinition def = new JaccDefinition( jaccContextId, role, clazz, (String) value );

			jaccDefinitions.add( def );

		}
		catch ( IndexOutOfBoundsException e ) {
			throw persistenceException( "Illegal usage of " + AvailableSettings.JACC_PREFIX + ": " + key );
		}
	}

	private void addCacheRegionDefinition(String role, String value, CacheRegionDefinition.CacheRegionType cacheType) {
		final StringTokenizer params = new StringTokenizer( value, ";, " );
		if ( !params.hasMoreTokens() ) {
			StringBuilder error = new StringBuilder( "Illegal usage of " );
			if ( cacheType == CacheRegionDefinition.CacheRegionType.ENTITY ) {
				error.append( AvailableSettings.CLASS_CACHE_PREFIX )
						.append( ": " )
						.append( AvailableSettings.CLASS_CACHE_PREFIX );
			}
			else {
				error.append( AvailableSettings.COLLECTION_CACHE_PREFIX )
						.append( ": " )
						.append( AvailableSettings.COLLECTION_CACHE_PREFIX );
			}
			error.append( '.' )
					.append( role )
					.append( ' ' )
					.append( value )
					.append( ".  Was expecting configuration, but found none" );
			throw persistenceException( error.toString() );
		}

		String usage = params.nextToken();
		String region = null;
		if ( params.hasMoreTokens() ) {
			region = params.nextToken();
		}
		boolean lazyProperty = true;
		if ( cacheType == CacheRegionDefinition.CacheRegionType.ENTITY ) {
			if ( params.hasMoreTokens() ) {
				lazyProperty = "all".equalsIgnoreCase( params.nextToken() );
			}
		}
		else {
			lazyProperty = false;
		}

		final CacheRegionDefinition def = new CacheRegionDefinition( cacheType, role, usage, region, lazyProperty );
		cacheRegionDefinitions.add( def );
	}

	@SuppressWarnings("UnnecessaryUnboxing")
	private void prepareMetadataBuilder(
			MetadataBuilder metadataBuilder,
			SpecialProperties specialProperties) {
		if ( specialProperties.namingStrategy != null ) {
			metadataBuilder.with( specialProperties.namingStrategy );
		}

		if ( specialProperties.sourceProcessingOrder != null ) {
			metadataBuilder.with( specialProperties.sourceProcessingOrder );
		}

		if ( specialProperties.useEnhancedGenerators != null ) {
			metadataBuilder.withNewIdentifierGeneratorsEnabled( specialProperties.useEnhancedGenerators.booleanValue() );
		}

		if ( specialProperties.sharedCacheMode != null ) {
			metadataBuilder.with( specialProperties.sharedCacheMode );
		}

		if ( specialProperties.defaultCacheAccessType != null ) {
			metadataBuilder.with( specialProperties.defaultCacheAccessType );
		}
	}

	private void prepareSessionFactoryBuilder(SessionFactoryBuilder builder, SpecialProperties specialProperties) {
		if ( specialProperties.sessionFactoryInterceptor != null ) {
			builder.with( specialProperties.sessionFactoryInterceptor );
		}
		if ( specialProperties.entityNameResolver != null ) {
			builder.add( specialProperties.entityNameResolver );
		}
		if ( specialProperties.entityNotFoundDelegate != null ) {
			builder.with( specialProperties.entityNotFoundDelegate );
		}
		if ( specialProperties.sessionFactoryObserver != null ) {
			builder.add( specialProperties.sessionFactoryObserver );
		}
		if ( specialProperties.customEntityDirtinessStrategy != null ) {
			builder.with( specialProperties.customEntityDirtinessStrategy );
		}
		if ( specialProperties.currentTenantIdentifierResolver != null ) {
			builder.with( specialProperties.currentTenantIdentifierResolver );
		}
	}

	@SuppressWarnings("UnnecessaryUnboxing")
	private Settings prepareEntitytManagerFactoryCreationSettings(SpecialProperties specialProperties) {
		final SettingsImpl settings = new SettingsImpl();
		if ( specialProperties.releaseResourcesOnClose != null ) {
			settings.setReleaseResourcesOnCloseEnabled( specialProperties.releaseResourcesOnClose.booleanValue() );
		}
		if ( specialProperties.sessionInterceptorClass != null ) {
			settings.setSessionInterceptorClass( specialProperties.sessionInterceptorClass );
		}
		if ( specialProperties.jpaTransactionType != null ) {
			settings.setTransactionType( specialProperties.jpaTransactionType );
		}
		return settings;
	}


	private PersistenceException persistenceException(String message) {
		return persistenceException( message, null );
	}

	private PersistenceException persistenceException(String message, Exception cause) {
		return new PersistenceException(
				getExceptionHeader() + message,
				cause
		);
	}

	private String getExceptionHeader() {
		return "[PersistenceUnit: " + persistenceUnit.getName() + "] ";
	}

	/**
	 * Aggregated return structure
	 */
	private static class SpecialProperties {
		// affecting MetadataBuilder...
		private NamingStrategy namingStrategy;
		private MetadataSourceProcessingOrder sourceProcessingOrder;
		private SharedCacheMode sharedCacheMode;
		private AccessType defaultCacheAccessType;
		private Boolean useEnhancedGenerators;

		// affecting SessionFactoryBuilder...
		private Interceptor sessionFactoryInterceptor;
		private SessionFactoryObserver sessionFactoryObserver;
		private EntityNameResolver entityNameResolver;
		private EntityNotFoundDelegate entityNotFoundDelegate = new JpaEntityNotFoundDelegate();
		private CustomEntityDirtinessStrategy customEntityDirtinessStrategy;
		private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

		// affecting EntityManagerFactory building
		private Boolean releaseResourcesOnClose;
		private Class<? extends Interceptor> sessionInterceptorClass;
		private PersistenceUnitTransactionType jpaTransactionType;
	}

	private static class JpaEntityNotFoundDelegate implements EntityNotFoundDelegate, Serializable {
		public void handleEntityNotFound(String entityName, Serializable id) {
			throw new EntityNotFoundException( "Unable to find " + entityName  + " with id " + id );
		}
	}

	private static class ServiceRegistryCloser implements SessionFactoryObserver {
		@Override
		public void sessionFactoryCreated(SessionFactory sessionFactory) {
			// nothing to do
		}

		@Override
		public void sessionFactoryClosed(SessionFactory sessionFactory) {
			SessionFactoryImplementor sfi = ( (SessionFactoryImplementor) sessionFactory );
			sfi.getServiceRegistry().destroy();
			ServiceRegistry basicRegistry = sfi.getServiceRegistry().getParentServiceRegistry();
			( (ServiceRegistryImplementor) basicRegistry ).destroy();
		}
	}
}
