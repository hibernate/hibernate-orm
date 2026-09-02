/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;
import java.sql.Types;
import java.util.List;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.type.BindingContext;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// Verifies the standalone provider's focused function contribution without
/// treating its Dialect as an independent application contributor.
///
/// @author Steve Ebersole
public class ExampleFunctionContributionTest {
	@Test
	void contributesFunctionsThroughTheFocusedDialectLifecycle() {
		final ExampleDialect dialect = new ExampleDialect();
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		typeConfiguration.scope( metadataBuildingContext() );
		final SqmFunctionRegistry functionRegistry = new SqmFunctionRegistry();
		final FunctionContributions contributions = new FunctionContributions() {
			@Override
			public SqmFunctionRegistry getFunctionRegistry() {
				return functionRegistry;
			}

			@Override
			public TypeConfiguration getTypeConfiguration() {
				return typeConfiguration;
			}

			@Override
			public ServiceRegistry getServiceRegistry() {
				return null;
			}
		};

		dialect.initializeFunctionRegistry( contributions );

		assertNotNull( functionRegistry.findFunctionDescriptor( "fixture_concat" ) );
		assertNotNull( functionRegistry.findFunctionDescriptor( "cot" ) );
		final var customDescriptor = (ExampleSelfRenderingFunctionDescriptor)
				functionRegistry.findFunctionDescriptor( "fixture_self_rendering" );
		assertNotNull( customDescriptor );
		customDescriptor.getArgumentsValidator().validate(
				List.of(),
				"fixture_self_rendering",
				bindingContext( typeConfiguration )
		);
		final var sql = new StringBuilderSqlAppender();
		customDescriptor.render( sql, List.of(), null, null );
		assertEquals( "fixture_self_rendering()", sql.toString() );
		assertFalse( TypeContributor.class.isAssignableFrom( ExampleDialect.class ) );
		assertFalse( FunctionContributor.class.isAssignableFrom( ExampleDialect.class ) );
	}

	private static MetadataBuildingContext metadataBuildingContext() {
		return (MetadataBuildingContext) Proxy.newProxyInstance(
				ExampleFunctionContributionTest.class.getClassLoader(),
				new Class<?>[] { MetadataBuildingContext.class },
				(proxy, method, arguments) -> switch ( method.getName() ) {
					case "getPreferredSqlTypeCodeForBoolean" -> Types.BOOLEAN;
					case "getBuildingOptions" -> metadataBuildingOptions();
					default -> defaultValue( method.getReturnType() );
				}
		);
	}

	private static MetadataBuildingOptions metadataBuildingOptions() {
		return (MetadataBuildingOptions) Proxy.newProxyInstance(
				ExampleFunctionContributionTest.class.getClassLoader(),
				new Class<?>[] { MetadataBuildingOptions.class },
				(proxy, method, arguments) -> defaultValue( method.getReturnType() )
		);
	}

	private static BindingContext bindingContext(TypeConfiguration typeConfiguration) {
		return (BindingContext) Proxy.newProxyInstance(
				ExampleFunctionContributionTest.class.getClassLoader(),
				new Class<?>[] { BindingContext.class },
				(proxy, method, arguments) -> method.getName().equals( "getTypeConfiguration" )
						? typeConfiguration
						: defaultValue( method.getReturnType() )
		);
	}

	private static Object defaultValue(Class<?> type) {
		if ( type == boolean.class ) {
			return false;
		}
		if ( type == int.class ) {
			return 0;
		}
		if ( type == long.class ) {
			return 0L;
		}
		return null;
	}
}
