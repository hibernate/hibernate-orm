/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import jakarta.persistence.spi.PersistenceUnitInfo;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.jpa.PersistenceUnitInfoImpl;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;

import org.jboss.logging.Logger;

/**
 * hibernate-testing implementation of a few JUnit5 contracts to support SessionFactory-based testing,
 * including argument injection (or see {@link SessionFactoryScopeAware})
 *
 * @author Steve Ebersole
 *
 * @see DomainModelExtension
 * @see SessionFactoryExtension
 */
public class EntityManagerFactoryExtension
		implements TestInstancePostProcessor, BeforeEachCallback, TestExecutionExceptionHandler {

	private static final Logger log = Logger.getLogger( EntityManagerFactoryExtension.class );
	private static final String EMF_KEY = EntityManagerFactoryScope.class.getName();

	private static ExtensionContext.Store locateExtensionStore(Object testScope, ExtensionContext context) {
		return JUnitHelper.locateExtensionStore( EntityManagerFactoryExtension.class, context, testScope );
	}

	public static EntityManagerFactoryScope findEntityManagerFactoryScope(
			Object testScope,
			Optional<Jpa> emfAnnWrapper,
			ExtensionContext context) {

		if ( emfAnnWrapper.isEmpty() ) {
			// No annotation on the test class, should be on the test methods
			return null;
		}

		final ExtensionContext.Store store = locateExtensionStore( testScope, context );
		final EntityManagerFactoryScope existing = (EntityManagerFactoryScope) store.get( EMF_KEY );
		if ( existing != null ) {
			return existing;
		}

		if ( !context.getElement().isPresent() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : " + context.getDisplayName() );
		}
		final Jpa emfAnn = emfAnnWrapper.get();

		final PersistenceUnitInfoImpl pui = new PersistenceUnitInfoImpl( emfAnn.persistenceUnitName() );
		( (Map<Object, Object>) Environment.getProperties() ).forEach(
				(key, value) ->
						pui.getProperties().put( key, value )
		);

		pui.setTransactionType( emfAnn.transactionType() );
		pui.setCacheMode( emfAnn.sharedCacheMode() );
		pui.setValidationMode( emfAnn.validationMode() );
		pui.setExcludeUnlistedClasses( emfAnn.excludeUnlistedClasses() );

		// JpaCompliance
		pui.getProperties().put( AvailableSettings.JPA_COMPLIANCE, emfAnn.jpaComplianceEnabled() );
		pui.getProperties().put( AvailableSettings.JPA_QUERY_COMPLIANCE, emfAnn.queryComplianceEnabled() );
		pui.getProperties().put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, emfAnn.transactionComplianceEnabled() );
		pui.getProperties().put( AvailableSettings.JPA_LIST_COMPLIANCE, emfAnn.listMappingComplianceEnabled() );
		pui.getProperties().put( AvailableSettings.JPA_CLOSED_COMPLIANCE, emfAnn.closedComplianceEnabled() );
		pui.getProperties().put( AvailableSettings.JPA_PROXY_COMPLIANCE, emfAnn.proxyComplianceEnabled() );
		pui.getProperties().put( AvailableSettings.JPA_CACHING_COMPLIANCE, emfAnn.cacheComplianceEnabled() );
		pui.getProperties().put( AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, emfAnn.generatorScopeComplianceEnabled() );
		pui.getProperties().put( AvailableSettings.JPA_ORDER_BY_MAPPING_COMPLIANCE, emfAnn.orderByMappingComplianceEnabled() );
		pui.getProperties().put( AvailableSettings.JPA_LOAD_BY_ID_COMPLIANCE, emfAnn.loadByIdComplianceEnabled() );

		final Setting[] properties = emfAnn.properties();
		for ( int i = 0; i < properties.length; i++ ) {
			final Setting property = properties[i];
			pui.getProperties().setProperty( property.name(), property.value() );
		}

		pui.getProperties().setProperty(
				AvailableSettings.GENERATE_STATISTICS,
				Boolean.toString( emfAnn.generateStatistics() )
		);

		if ( emfAnn.exportSchema() ) {
			pui.getProperties().setProperty(
					AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION,
					Action.CREATE_DROP.getExternalHbm2ddlName()
			);
		}

		if ( emfAnn.annotatedPackageNames().length > 0 ) {
			pui.applyManagedClassNames( emfAnn.annotatedPackageNames() );
		}

		if ( emfAnn.annotatedClassNames().length > 0 ) {
			pui.applyManagedClassNames( emfAnn.annotatedClassNames() );
		}

		if ( emfAnn.annotatedClasses().length > 0 ) {
			for ( int i = 0; i < emfAnn.annotatedClasses().length; i++ ) {
				pui.applyManagedClassNames( emfAnn.annotatedClasses()[i].getName() );
			}
		}

		if ( emfAnn.xmlMappings().length > 0 ) {
			pui.applyMappingFiles( emfAnn.xmlMappings() );
		}

		if ( emfAnn.standardModels().length > 0 ) {
			for ( int i = 0; i < emfAnn.standardModels().length; i++ ) {
				final StandardDomainModel standardDomainModel = emfAnn.standardModels()[i];
				for ( int i1 = 0; i1 < standardDomainModel.getDescriptor().getAnnotatedClasses().length; i1++ ) {
					final Class<?> annotatedClass = standardDomainModel.getDescriptor().getAnnotatedClasses()[i1];
					pui.applyManagedClassNames( annotatedClass.getName() );
				}
			}
		}

		if ( emfAnn.modelDescriptorClasses().length > 0 ) {
			for ( int i = 0; i < emfAnn.modelDescriptorClasses().length; i++ ) {
				final Class<? extends DomainModelDescriptor> modelDescriptorClass = emfAnn.modelDescriptorClasses()[i];
				final DomainModelDescriptor domainModelDescriptor = instantiateDomainModelDescriptor(
						modelDescriptorClass );
				for ( int i1 = 0; i1 < domainModelDescriptor.getAnnotatedClasses().length; i1++ ) {
					final Class<?> annotatedClass = domainModelDescriptor.getAnnotatedClasses()[i1];
					pui.applyManagedClassNames( annotatedClass.getName() );
				}
			}
		}

		final Map<String, Object> integrationSettings = new HashMap<>();

		integrationSettings.put( PersistentTableStrategy.DROP_ID_TABLES, "true" );
		integrationSettings.put( GlobalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		integrationSettings.put( LocalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		for ( int i = 0; i < emfAnn.integrationSettings().length; i++ ) {
			final Setting setting = emfAnn.integrationSettings()[i];
			integrationSettings.put( setting.name(), setting.value() );
		}

		for ( SettingProvider providerAnn : emfAnn.settingProviders() ) {
			final Class<? extends SettingProvider.Provider<?>> providerImpl = providerAnn.provider();
			try {
				final SettingProvider.Provider<?> provider = providerImpl.getConstructor().newInstance();
				integrationSettings.put( providerAnn.settingName(), provider.getSetting() );
			}
			catch (Exception e) {
				log.error( "Error obtaining setting provider for " + providerImpl.getName(), e );
			}
		}

		// statement inspector
		if ( emfAnn.useCollectingStatementInspector() ) {
			String inspectorSetting = (String) integrationSettings.get( AvailableSettings.STATEMENT_INSPECTOR );
			if ( !(inspectorSetting == null || inspectorSetting.isBlank()) ) {
				log.warn( String.format( "Overriding the explicit \"%1s\" statement inspector setting", inspectorSetting ) );
			}
			integrationSettings.put( AvailableSettings.STATEMENT_INSPECTOR, new SQLStatementInspector() );
		}

		ServiceRegistryUtil.applySettings( integrationSettings );
		final EntityManagerFactoryScopeImpl scope = new EntityManagerFactoryScopeImpl( pui, integrationSettings );

		store.put( EMF_KEY, scope );

		return scope;
	}

	private static DomainModelDescriptor instantiateDomainModelDescriptor(Class<? extends DomainModelDescriptor> modelDescriptorClass) {
		// first, see if it has a static singleton reference and use that if so
		try {
			final Field[] declaredFields = modelDescriptorClass.getDeclaredFields();
			for ( int i = 0; i < declaredFields.length; i++ ) {
				final Field field = declaredFields[i];
				if ( ReflectHelper.isStaticField( field ) ) {
					final Object value = field.get( null );
					if ( value instanceof DomainModelDescriptor ) {
						return (DomainModelDescriptor) value;
					}
				}
			}
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(
					"Problem accessing DomainModelDescriptor fields : " + modelDescriptorClass.getName(),
					e
			);
		}

		// no singleton field, try to instantiate it via reflection
		try {
			return modelDescriptorClass.getConstructor( null ).newInstance( null );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(
					"Problem instantiation DomainModelDescriptor : " + modelDescriptorClass.getName(),
					e
			);
		}
	}

	private static void prepareSchemaExport(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor model) {
		final Map<String, Object> baseProperties = sessionFactory.getProperties();

		final Set<ActionGrouping> groupings = ActionGrouping.interpret( model, baseProperties );
		if ( !groupings.isEmpty() ) {
			// the properties contained explicit settings for auto schema tooling - skip the annotation
			return;
		}

		final HashMap<String,Object> settings = new HashMap<>( baseProperties );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.CREATE_DROP );

		final StandardServiceRegistry serviceRegistry = model.getMetadataBuildingOptions().getServiceRegistry();


		SchemaManagementToolCoordinator.process(
				model,
				serviceRegistry,
				settings,
				action -> sessionFactory.addObserver(
						new SessionFactoryObserver() {
							@Override
							public void sessionFactoryClosing(org.hibernate.SessionFactory factory) {
								action.perform( serviceRegistry );
							}
						}
				)
		);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		log.tracef( "#beforeEach(%s)", context.getDisplayName() );
		final Optional<Jpa> emfAnnWrapper = AnnotationSupport.findAnnotation(
				context.getRequiredTestMethod(),
				Jpa.class
		);

		if ( emfAnnWrapper.isEmpty() ) {
			// assume the annotation is defined on the class-level...
			return;
		}

		findEntityManagerFactoryScope( context.getRequiredTestMethod(), emfAnnWrapper, context );
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		log.tracef( "#postProcessTestInstance(%s, %s)", testInstance, context.getDisplayName() );

		final Optional<Jpa> emfAnnWrapper = AnnotationSupport.findAnnotation(
				context.getRequiredTestClass(),
				Jpa.class
		);

		findEntityManagerFactoryScope( testInstance, emfAnnWrapper, context );
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		log.tracef( "#handleTestExecutionException(%s, %s)", context.getDisplayName(), throwable );

		try {
			final Object testInstance = context.getRequiredTestInstance();
			final ExtensionContext.Store store = locateExtensionStore( testInstance, context );
			final EntityManagerFactoryScopeImpl scope = (EntityManagerFactoryScopeImpl) store.get( EMF_KEY );
			scope.releaseEntityManagerFactory();
		}
		catch (Exception ignore) {
		}

		throw throwable;
	}

	private static class EntityManagerFactoryScopeImpl extends AbstractEntityManagerFactoryScope {
		private final PersistenceUnitInfo persistenceUnitInfo;
		private final Map<String, Object> integrationSettings;

		private EntityManagerFactoryScopeImpl(
				PersistenceUnitInfo persistenceUnitInfo,
				Map<String, Object> integrationSettings) {
			this.persistenceUnitInfo = persistenceUnitInfo;
			this.integrationSettings = integrationSettings;
		}

		protected jakarta.persistence.EntityManagerFactory createEntityManagerFactory() {
			final EntityManagerFactoryBuilder emfBuilder = Bootstrap.getEntityManagerFactoryBuilder(
					new PersistenceUnitInfoDescriptor( persistenceUnitInfo ),
					integrationSettings
			);

			return emfBuilder.build();
		}
	}
}
