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
import java.util.Properties;

import jakarta.persistence.spi.PersistenceUnitInfo;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.jpa.PersistenceUnitInfoImpl;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import org.jboss.logging.Logger;

import static org.hibernate.jpa.boot.spi.Bootstrap.getEntityManagerFactoryBuilder;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

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
			Object testScope, Optional<Jpa> optionalJpa, ExtensionContext context) {

		if ( optionalJpa.isEmpty() ) {
			// No annotation on the test class, should be on the test methods
			return null;
		}

		final ExtensionContext.Store store = locateExtensionStore( testScope, context );
		final EntityManagerFactoryScope existing = (EntityManagerFactoryScope) store.get( EMF_KEY );
		if ( existing != null ) {
			return existing;
		}
		if ( context.getElement().isEmpty() ) {
			throw new RuntimeException( "Unable to determine how to handle given ExtensionContext : "
					+ context.getDisplayName() );
		}

		final Jpa jpa = optionalJpa.get();
		final PersistenceUnitInfoImpl pui = createPersistenceUnitInfo( jpa );
		collectProperties( pui, jpa );
		managedClassesAndMappings( jpa, pui );
		final Map<String, Object> integrationSettings = collectIntegrationSettings( jpa );
		// statement inspector
		setupStatementInspector( jpa, integrationSettings );
		ServiceRegistryUtil.applySettings( integrationSettings );
		final EntityManagerFactoryScopeImpl scope =
				new EntityManagerFactoryScopeImpl( pui, integrationSettings );
		store.put( EMF_KEY, scope );
		return scope;
	}

	private static void collectProperties(PersistenceUnitInfoImpl pui, Jpa jpa) {
		final Properties properties = pui.getProperties();
		properties.putAll( Environment.getProperties() );
		// JpaCompliance
		setJpaComplianceProperties( properties, jpa );
		for ( Setting property : jpa.properties() ) {
			properties.setProperty( property.name(), property.value() );
		}
		properties.setProperty(
				AvailableSettings.GENERATE_STATISTICS,
				Boolean.toString( jpa.generateStatistics() )
		);
		if ( jpa.exportSchema() ) {
			properties.setProperty(
					AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION,
					Action.CREATE_DROP.getExternalHbm2ddlName()
			);
		}
	}

	private static PersistenceUnitInfoImpl createPersistenceUnitInfo(Jpa jpa) {
		final PersistenceUnitInfoImpl pui =
				new PersistenceUnitInfoImpl( jpa.persistenceUnitName() );
		pui.setTransactionType( jpa.transactionType() );
		pui.setCacheMode( jpa.sharedCacheMode() );
		pui.setValidationMode( jpa.validationMode() );
		pui.setExcludeUnlistedClasses( jpa.excludeUnlistedClasses() );
		return pui;
	}

	private static void managedClassesAndMappings(Jpa jpa, PersistenceUnitInfoImpl pui) {
		if ( jpa.annotatedPackageNames().length > 0 ) {
			pui.applyManagedClassNames( jpa.annotatedPackageNames() );
		}

		if ( jpa.annotatedClassNames().length > 0 ) {
			pui.applyManagedClassNames( jpa.annotatedClassNames() );
		}

		if ( jpa.annotatedClasses().length > 0 ) {
			for (int i = 0; i < jpa.annotatedClasses().length; i++ ) {
				pui.applyManagedClassNames( jpa.annotatedClasses()[i].getName() );
			}
		}

		if ( jpa.xmlMappings().length > 0 ) {
			pui.applyMappingFiles( jpa.xmlMappings() );
		}

		for ( StandardDomainModel standardDomainModel : jpa.standardModels() ) {
			for ( Class<?> annotatedClass : standardDomainModel.getDescriptor().getAnnotatedClasses() ) {
				pui.applyManagedClassNames( annotatedClass.getName() );
			}
		}

		for ( Class<? extends DomainModelDescriptor> modelDescriptorClass :
				jpa.modelDescriptorClasses() ) {
			final DomainModelDescriptor domainModelDescriptor =
					instantiateDomainModelDescriptor( modelDescriptorClass );
			final Class<?>[] annotatedClasses = domainModelDescriptor.getAnnotatedClasses();
			for ( Class<?> annotatedClass : annotatedClasses ) {
				pui.applyManagedClassNames( annotatedClass.getName() );
			}
		}
	}

	private static Map<String, Object> collectIntegrationSettings(Jpa jpa) {
		final Map<String, Object> integrationSettings = new HashMap<>();
		integrationSettings.put( PersistentTableStrategy.DROP_ID_TABLES, "true" );
		integrationSettings.put( GlobalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		integrationSettings.put( LocalTemporaryTableMutationStrategy.DROP_ID_TABLES, "true" );
		final Setting[] settings = jpa.integrationSettings();
		for ( Setting setting : settings ) {
			integrationSettings.put( setting.name(), setting.value() );
		}
		for ( SettingProvider providerAnn : jpa.settingProviders() ) {
			final Class<? extends SettingProvider.Provider<?>> providerImpl = providerAnn.provider();
			try {
				integrationSettings.put( providerAnn.settingName(),
						providerImpl.getConstructor().newInstance().getSetting() );
			}
			catch (Exception e) {
				log.error( "Error obtaining setting provider for " + providerImpl.getName(), e );
			}
		}
		return integrationSettings;
	}

	private static void setupStatementInspector(Jpa jpa, Map<String, Object> integrationSettings) {
		if ( jpa.useCollectingStatementInspector() ) {
			final String inspectorSetting = (String) integrationSettings.get( AvailableSettings.STATEMENT_INSPECTOR );
			if ( inspectorSetting != null && !inspectorSetting.isBlank() ) {
				log.warn( String.format( "Overriding the explicit \"%1s\" statement inspector setting", inspectorSetting ) );
			}
			integrationSettings.put( AvailableSettings.STATEMENT_INSPECTOR, new SQLStatementInspector() );
		}
	}

	private static void setJpaComplianceProperties(Properties properties, Jpa jpa) {
		properties.put( AvailableSettings.JPA_COMPLIANCE, jpa.jpaComplianceEnabled() );
		properties.put( AvailableSettings.JPA_QUERY_COMPLIANCE, jpa.queryComplianceEnabled() );
		properties.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, jpa.transactionComplianceEnabled() );
		properties.put( AvailableSettings.JPA_CLOSED_COMPLIANCE, jpa.closedComplianceEnabled() );
		properties.put( AvailableSettings.JPA_PROXY_COMPLIANCE, jpa.proxyComplianceEnabled() );
		properties.put( AvailableSettings.JPA_CACHING_COMPLIANCE, jpa.cacheComplianceEnabled() );
		properties.put( AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, jpa.generatorScopeComplianceEnabled() );
		properties.put( AvailableSettings.JPA_ORDER_BY_MAPPING_COMPLIANCE, jpa.orderByMappingComplianceEnabled() );
		properties.put( AvailableSettings.JPA_LOAD_BY_ID_COMPLIANCE, jpa.loadByIdComplianceEnabled() );
	}

	private static DomainModelDescriptor instantiateDomainModelDescriptor(
			Class<? extends DomainModelDescriptor> modelDescriptorClass) {
		// first, see if it has a static singleton reference and use that if so
		try {
			for ( Field field : modelDescriptorClass.getDeclaredFields() ) {
				if ( ReflectHelper.isStaticField(field) ) {
					final Object value = field.get( null );
					if ( value instanceof DomainModelDescriptor descriptor ) {
						return descriptor;
					}
				}
			}
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException( "Problem accessing DomainModelDescriptor fields : "
					+ modelDescriptorClass.getName(), e );
		}

		// no singleton field, try to instantiate it via reflection
		try {
			return modelDescriptorClass.getConstructor( null ).newInstance( null );
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException( "Problem instantiation DomainModelDescriptor : "
					+ modelDescriptorClass.getName(), e );
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		log.tracef( "#beforeEach(%s)", context.getDisplayName() );
		final Optional<Jpa> optionalJpa = findAnnotation( context.getRequiredTestMethod(), Jpa.class );
		if ( optionalJpa.isPresent() ) {
			findEntityManagerFactoryScope( context.getRequiredTestMethod(), optionalJpa, context );
		}
		// else assume the annotation is defined on the class-level...
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		log.tracef( "#postProcessTestInstance(%s, %s)", testInstance, context.getDisplayName() );
		final Optional<Jpa> optionalJpa = findAnnotation( context.getRequiredTestClass(), Jpa.class );
		findEntityManagerFactoryScope( testInstance, optionalJpa, context );
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		log.tracef( "#handleTestExecutionException(%s, %s)", context.getDisplayName(), throwable );
		try {
			final ExtensionContext.Store store = locateExtensionStore( context.getRequiredTestInstance(), context );
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
			final PersistenceUnitInfoDescriptor descriptor = new PersistenceUnitInfoDescriptor( persistenceUnitInfo );
			return getEntityManagerFactoryBuilder( descriptor, integrationSettings ).build();
		}
	}
}
