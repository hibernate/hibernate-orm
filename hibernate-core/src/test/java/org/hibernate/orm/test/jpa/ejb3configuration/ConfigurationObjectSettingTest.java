/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;

import org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test passing along various config settings that take objects other than strings as values.
 *
 * @author Steve Ebersole
 */
@BaseUnitTest
public class ConfigurationObjectSettingTest {
	@Test
	public void testSharedCacheMode() {
		verifyCacheMode( AvailableSettings.JAKARTA_SHARED_CACHE_MODE );
		verifyCacheMode( AvailableSettings.JPA_SHARED_CACHE_MODE );
	}

	private void verifyCacheMode(String settingName) {
		// first, via the integration vars
		PersistenceUnitInfoAdapter empty = new PersistenceUnitInfoAdapter();
		EntityManagerFactoryBuilderImpl builder = null;
		{
			// as object
			builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					empty,
					Collections.singletonMap( settingName, SharedCacheMode.DISABLE_SELECTIVE )
			);
			assertThat( builder.getConfigurationValues().get( settingName ) ).isEqualTo( SharedCacheMode.DISABLE_SELECTIVE );
		}
		builder.cancel();

		{
			// as string
			builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					empty,
					Collections.singletonMap( settingName, SharedCacheMode.DISABLE_SELECTIVE.name() )
			);
			assertThat( builder.getConfigurationValues().get( settingName ) ).isEqualTo( SharedCacheMode.DISABLE_SELECTIVE.name() );
		}
		builder.cancel();

		// next, via the PUI
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter() {
			@Override
			public SharedCacheMode getSharedCacheMode() {
				return SharedCacheMode.ENABLE_SELECTIVE;
			}
		};
		{
			builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					null
			);
			Object value = builder.getConfigurationValues().get( settingName );
			// PUI should only set non-deprecated setting
			if (AvailableSettings.JPA_SHARED_CACHE_MODE.equals( settingName )) {
				assertNull( value );
			}
			else {
				assertEquals( SharedCacheMode.ENABLE_SELECTIVE, value );
			}
		}
		builder.cancel();

		// via both, integration vars should take precedence
		{
			builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap( settingName, SharedCacheMode.DISABLE_SELECTIVE )
			);
			assertEquals( SharedCacheMode.DISABLE_SELECTIVE, builder.getConfigurationValues().get( settingName ) );
		}
		builder.cancel();
	}

	@Test
	public void testValidationMode() {
		verifyValidationMode( AvailableSettings.JAKARTA_VALIDATION_MODE );
		verifyValidationMode( AvailableSettings.JPA_VALIDATION_MODE );
	}

	private void verifyValidationMode(String settingName) {
		// first, via the integration vars
		PersistenceUnitInfoAdapter empty = new PersistenceUnitInfoAdapter();
		EntityManagerFactoryBuilderImpl builder = null;
		{
			// as object
			builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					empty,
					Collections.singletonMap( settingName, ValidationMode.CALLBACK )
			);
			assertThat( builder.getConfigurationValues().get( settingName ) ).isEqualTo( ValidationMode.CALLBACK );
		}
		builder.cancel();

		{
			// as string
			builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					empty,
					Collections.singletonMap( settingName, ValidationMode.CALLBACK.name() )
			);
			assertThat( builder.getConfigurationValues().get( settingName ) ).isEqualTo( ValidationMode.CALLBACK.name() );
		}
		builder.cancel();

		// next, via the PUI
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter() {
			@Override
			public ValidationMode getValidationMode() {
				return ValidationMode.CALLBACK;
			}
		};
		{
			builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					null
			);
			Object value = builder.getConfigurationValues().get( settingName );
			// PUI should only set non-deprecated settings
			if (AvailableSettings.JPA_VALIDATION_MODE.equals( settingName )) {
				assertNull( value );
			}
			else {
				assertThat( value ).isEqualTo( ValidationMode.CALLBACK );
			}
		}
		builder.cancel();

		// via both, integration vars should take precedence
		{
			builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap( settingName, ValidationMode.NONE )
			);
			assertThat( builder.getConfigurationValues().get( settingName ) ).isEqualTo( ValidationMode.NONE );
		}
		builder.cancel();
	}

	@Test
	public void testValidationFactory() {
		verifyValidatorFactory( AvailableSettings.JAKARTA_VALIDATION_FACTORY );
		verifyValidatorFactory( AvailableSettings.JPA_VALIDATION_FACTORY );
	}

	private void verifyValidatorFactory(String settingName) {
		final Object token = new Object();
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
		try {
			Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap( settingName, token )
			).cancel();
			fail( "Was expecting error as token did not implement ValidatorFactory" );
		}
		catch ( HibernateException e ) {
			// probably the condition we want but unfortunately the exception is not specific
			// and the pertinent info is in a cause
		}
	}

	@Test
//	@FailureExpected(
//			reason = "this is unfortunate to not be able to test.  it fails because the values from `hibernate.properties` " +
//					"name the Hibernate-specific settings, which always take precedence.  testing this needs to be able to erase " +
//					"those entries in the ConfigurationService Map"
//	)
	public void testJdbcSettings() {
		verifyJdbcSettings(
				AvailableSettings.JAKARTA_JDBC_URL,
				AvailableSettings.JAKARTA_JDBC_DRIVER,
				AvailableSettings.JAKARTA_JDBC_USER,
				AvailableSettings.JAKARTA_JDBC_PASSWORD
		);
		verifyJdbcSettings(
				AvailableSettings.JPA_JDBC_URL,
				AvailableSettings.JPA_JDBC_DRIVER,
				AvailableSettings.JPA_JDBC_USER,
				AvailableSettings.JPA_JDBC_PASSWORD
		);
	}

	private void verifyJdbcSettings(String jdbcUrl, String jdbcDriver, String jdbcUser, String jdbcPassword) {
		final String urlValue = "some:url";
		final String driverValue = "some.jdbc.Driver";
		final String userValue = "goofy";
		final String passwordValue = "goober";

		// first, via the integration vars
		PersistenceUnitInfoAdapter empty = new PersistenceUnitInfoAdapter();
		EntityManagerFactoryBuilderImpl builder = null;
		{
			builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					empty,
					CollectionHelper.toMap(
							jdbcUrl, urlValue,
							jdbcDriver, driverValue,
							jdbcUser, userValue,
							jdbcPassword, passwordValue
					),
					mergedSettings -> mergedSettings.getConfigurationValues().clear()
			);

			assertThat( builder.getConfigurationValues().get( jdbcUrl ) ).isEqualTo( urlValue );
			assertThat( builder.getConfigurationValues().get( jdbcDriver ) ).isEqualTo( driverValue );
			assertThat( builder.getConfigurationValues().get( jdbcUser ) ).isEqualTo( userValue );
			assertThat( builder.getConfigurationValues().get( jdbcPassword ) ).isEqualTo( passwordValue );

			builder.cancel();
		}

		PersistenceUnitInfoAdapter pui = new PersistenceUnitInfoAdapter();
		CollectionHelper.applyToProperties(
				pui.getProperties(),
				jdbcUrl, urlValue,
				jdbcDriver, driverValue,
				jdbcUser, userValue,
				jdbcPassword, passwordValue
		);
		{
			builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					pui,
					null,
					mergedSettings -> mergedSettings.getConfigurationValues().clear()
			);

			assertThat( builder.getConfigurationValues().get( jdbcUrl ) ).isEqualTo( urlValue );
			assertThat( builder.getConfigurationValues().get( jdbcDriver ) ).isEqualTo( driverValue );
			assertThat( builder.getConfigurationValues().get( jdbcUser ) ).isEqualTo( userValue );
			assertThat( builder.getConfigurationValues().get( jdbcPassword ) ).isEqualTo( passwordValue );

			builder.cancel();
		}
	}

	@Test
	public void testSchemaGenSettings() {
		verifySchemaGenSettings(
				AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION,
				AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION,
				AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS,
				AvailableSettings.JAKARTA_HBM2DDL_DB_NAME
		);
		verifySchemaGenSettings(
				AvailableSettings.HBM2DDL_DATABASE_ACTION,
				AvailableSettings.HBM2DDL_SCRIPTS_ACTION,
				AvailableSettings.HBM2DDL_CREATE_SCHEMAS,
				AvailableSettings.DIALECT_DB_NAME
		);
//		verifySchemaGenSettingsPrecedence();
	}

	private void verifySchemaGenSettings(
			String dbActionSettingName,
			String scriptActionSettingName,
			String createSchemasSettingName,
			String dbNameSettingName) {
		final Action dbAction = Action.CREATE_ONLY;
		final Action scriptAction = Action.CREATE_ONLY;
		final boolean createSchemas = true;
		final String dbName = "H2";

		final Map<String, String> settings = CollectionHelper.toMap(
				dbActionSettingName, dbAction.getExternalJpaName(),
				scriptActionSettingName, scriptAction.getExternalJpaName(),
				createSchemasSettingName, Boolean.toString( createSchemas ),
				dbNameSettingName, dbName
		);

		// first verify the individual interpretations
		final Action interpretedDbAction = ActionGrouping.determineJpaDbActionSetting( settings, null, null );
		assertThat( interpretedDbAction ).isEqualTo( dbAction );
		final Action interpretedScriptAction = ActionGrouping.determineJpaScriptActionSetting( settings, null, null );
		assertThat( interpretedScriptAction ).isEqualTo( scriptAction );

		// next verify the action-group determination
		final ActionGrouping actionGrouping = ActionGrouping.interpret( settings );
		assertThat( actionGrouping.getDatabaseAction() ).isEqualTo( dbAction );
		assertThat( actionGrouping.getScriptAction() ).isEqualTo( scriptAction );

		// the check above uses a "for testing only" form of what happens for "real".
		// verify the "real" path as well
		try (StandardServiceRegistryImpl servicedRegistry = ServiceRegistryUtil.serviceRegistry()) {
			final Metadata metadata = new MetadataSources( servicedRegistry )
					.addAnnotatedClass( Bell.class )
					.buildMetadata();
			final Set<ActionGrouping> actionGroupings = ActionGrouping.interpret( metadata, settings );
			assertThat( actionGroupings ).hasSize( 1 );
			final ActionGrouping grouping = actionGroupings.iterator().next();
			assertThat( grouping.getContributor() ).isEqualTo( "orm" );
			assertThat( grouping.getDatabaseAction() ).isEqualTo( dbAction );
			assertThat( grouping.getScriptAction() ).isEqualTo( scriptAction );

			// verify also interpreting the db-name, etc... they are used by SF/EMF to resolve Dialect
			final DialectResolver dialectResolver = new DialectResolverInitiator()
					.initiateService(
							new HashMap<>( settings ),
							servicedRegistry
					);
			final Dialect dialect = dialectResolver.resolveDialect( TestingDialectResolutionInfo.forDatabaseInfo( dbName ) );
			assertThat( dialect ).isInstanceOf( H2Dialect.class );
		}
	}

}
