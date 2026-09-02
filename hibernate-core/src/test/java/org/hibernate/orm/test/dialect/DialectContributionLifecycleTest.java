/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.hibernate.SPI;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.type.spi.StandardDdlTypes;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Verifies the fixed Dialect contribution positions relative to independently
/// ordered application contributors.
///
/// @author Steve Ebersole
@DomainModel
@ServiceRegistry(settingProviders = @SettingProvider(
		settingName = AvailableSettings.DIALECT,
		provider = DialectContributionLifecycleTest.DialectSettingProvider.class
))
@SessionFactory
@BootstrapServiceRegistry(javaServices = {
		@BootstrapServiceRegistry.JavaService(
				role = TypeContributor.class,
				impl = DialectContributionLifecycleTest.LowTypeContributor.class
		),
		@BootstrapServiceRegistry.JavaService(
				role = TypeContributor.class,
				impl = DialectContributionLifecycleTest.HighTypeContributor.class
		),
		@BootstrapServiceRegistry.JavaService(
				role = FunctionContributor.class,
				impl = DialectContributionLifecycleTest.LowFunctionContributor.class
		),
		@BootstrapServiceRegistry.JavaService(
				role = FunctionContributor.class,
				impl = DialectContributionLifecycleTest.HighFunctionContributor.class
		)
})
public class DialectContributionLifecycleTest {
	private static final int CUSTOM_DDL_TYPE = 60_047;
	private static final List<String> EVENTS = new CopyOnWriteArrayList<>();
	private static TypeContributions dialectTypeContributions;
	private static org.hibernate.service.ServiceRegistry dialectServiceRegistry;
	private static TypeContributions independentTypeContributions;
	private static org.hibernate.service.ServiceRegistry independentServiceRegistry;

	@Test
	void preservesDialectAndIndependentContributorOrdering(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getQueryEngine() ).isNotNull();
		assertThat( EVENTS ).containsSubsequence( "dialect-type", "type-low", "type-high" );
		assertThat( EVENTS ).containsSubsequence( "function-low", "function-high", "dialect-function" );
		assertThat( EVENTS.stream().filter( "dialect-type"::equals ) ).hasSize( 1 );
		assertThat( EVENTS.stream().filter( "dialect-function"::equals ) ).hasSize( 1 );
		assertThat( independentTypeContributions ).isSameAs( dialectTypeContributions );
		assertThat( independentServiceRegistry ).isSameAs( dialectServiceRegistry );
	}

	@Test
	void superDelegationExtendsAndOmissionReplacesInheritedColumnTypes() {
		final TypeConfiguration extendingTypes = new TypeConfiguration();
		new ExtendingColumnTypesDialect().contributeTypes( () -> extendingTypes, null );
		assertThat( extendingTypes.getDdlTypeRegistry().getDescriptor( SqlTypes.INTEGER ) ).isNotNull();
		assertThat( extendingTypes.getDdlTypeRegistry().getDescriptor( CUSTOM_DDL_TYPE ) ).isNotNull();

		final TypeConfiguration replacementTypes = new TypeConfiguration();
		new ReplacingColumnTypesDialect().contributeTypes( () -> replacementTypes, null );
		assertThat( replacementTypes.getDdlTypeRegistry().getDescriptor( SqlTypes.INTEGER ) ).isNull();
		assertThat( replacementTypes.getDdlTypeRegistry().getDescriptor( CUSTOM_DDL_TYPE ) ).isNotNull();
	}

	public static class DialectSettingProvider implements SettingProvider.Provider<Dialect> {
		@Override
		public Dialect getSetting() {
			return new RecordingDialect();
		}
	}

	public static class RecordingDialect extends H2Dialect {
		@Override
		@SPI({ IMPLEMENT, SUPPLY })
		public void contributeTypes(
				TypeContributions typeContributions,
				org.hibernate.service.ServiceRegistry serviceRegistry) {
			dialectTypeContributions = typeContributions;
			dialectServiceRegistry = serviceRegistry;
			EVENTS.add( "dialect-type" );
			super.contributeTypes( typeContributions, serviceRegistry );
		}

		@Override
		@SPI({ IMPLEMENT, SUPPLY })
		public void initializeFunctionRegistry(FunctionContributions functionContributions) {
			EVENTS.add( "dialect-function" );
			super.initializeFunctionRegistry( functionContributions );
		}
	}

	public static class LowTypeContributor implements TypeContributor {
		@Override
		public void contribute(
				TypeContributions typeContributions,
				org.hibernate.service.ServiceRegistry serviceRegistry) {
			independentTypeContributions = typeContributions;
			independentServiceRegistry = serviceRegistry;
			EVENTS.add( "type-low" );
		}

		@Override
		public int ordinal() {
			return 600;
		}
	}

	public static class HighTypeContributor implements TypeContributor {
		@Override
		public void contribute(
				TypeContributions typeContributions,
				org.hibernate.service.ServiceRegistry serviceRegistry) {
			EVENTS.add( "type-high" );
		}

		@Override
		public int ordinal() {
			return 700;
		}
	}

	public static class LowFunctionContributor implements FunctionContributor {
		@Override
		public void contributeFunctions(FunctionContributions functionContributions) {
			EVENTS.add( "function-low" );
		}

		@Override
		public int ordinal() {
			return 600;
		}
	}

	public static class HighFunctionContributor implements FunctionContributor {
		@Override
		public void contributeFunctions(FunctionContributions functionContributions) {
			EVENTS.add( "function-high" );
		}

		@Override
		public int ordinal() {
			return 700;
		}
	}

	private static class ExtendingColumnTypesDialect extends H2Dialect {
		@Override
		@SPI({ IMPLEMENT, SUPPLY })
		protected void registerColumnTypes(
				TypeContributions typeContributions,
				org.hibernate.service.ServiceRegistry serviceRegistry) {
			super.registerColumnTypes( typeContributions, serviceRegistry );
			registerCustomDdlType( typeContributions, this );
		}
	}

	private static class ReplacingColumnTypesDialect extends H2Dialect {
		@Override
		@SPI({ IMPLEMENT, SUPPLY })
		protected void registerColumnTypes(
				TypeContributions typeContributions,
				org.hibernate.service.ServiceRegistry serviceRegistry) {
			registerCustomDdlType( typeContributions, this );
		}
	}

	private static void registerCustomDdlType(TypeContributions typeContributions, Dialect dialect) {
		typeContributions.getTypeConfiguration().getDdlTypeRegistry().addDescriptor(
				StandardDdlTypes.simple( CUSTOM_DDL_TYPE, "custom", dialect )
		);
	}
}
