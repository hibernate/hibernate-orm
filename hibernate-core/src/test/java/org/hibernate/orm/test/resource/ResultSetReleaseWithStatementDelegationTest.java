/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


import static org.hibernate.resource.jdbc.internal.ResourceRegistryLogger.RESOURCE_REGISTRY_LOGGER;

@DomainModel(
		annotatedClasses = {
				ResultSetReleaseWithStatementDelegationTest.Foo.class,
		}
)
@ServiceRegistry(
		settingProviders = @SettingProvider(settingName = AvailableSettings.CONNECTION_PROVIDER,
				provider = ResultSetReleaseWithStatementDelegationTest.ConnectionProviderDelegateProvider.class)
)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@JiraKey( "HHH-19280" )
class ResultSetReleaseWithStatementDelegationTest {

	public static class ConnectionProviderDelegateProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return ConnectionProviderDelegate.class.getName();
		}
	}

	private Triggerable triggerable;

	@RegisterExtension
	public LoggerInspectionExtension logger =
			LoggerInspectionExtension.builder().setLogger( RESOURCE_REGISTRY_LOGGER ).build();

	@BeforeEach
	public void setUp() {
		triggerable = logger.watchForLogMessages( "Exception clearing maxRows/queryTimeout" );
		triggerable.reset();
	}

	@Test
	void testStatementReleae(SessionFactoryScope scope) {
		scope.inTransaction( session ->
				session.persist( new Foo() )
		);
		Assertions.assertFalse(
				triggerable.wasTriggered(),
				"Warning message `Exception clearing maxRows/queryTimeout` has been raised releasing a ResultSet "
		);
	}

	@Entity(name = "Foo")
	static class Foo {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private long id;

		private String name;
	}
}
