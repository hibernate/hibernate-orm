/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.lang.reflect.Proxy;
import java.sql.Types;
import java.util.Map;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that community Dialects retain the function descriptors which
/// established the supported Core descriptor-base contracts.
///
/// @author Steve Ebersole
public class CommunityFunctionProviderContractsTest {
	@Test
	void derbyRetainsPaddingEmulations() {
		assertRegistrations(
				new DerbyDialect(),
				Map.of(
						"lpad", "DerbyLpadEmulation",
						"rpad", "DerbyRpadEmulation"
				)
		);
	}

	@Test
	void gaussDbRetainsGeneralArrayAndJsonDescriptors() {
		assertRegistrations(
				new GaussDBDialect(),
				Map.ofEntries(
						Map.entry( "min", "GaussDBMinMaxFunction" ),
						Map.entry( "max", "GaussDBMinMaxFunction" ),
						Map.entry( "round", "GaussDBTruncRoundFunction" ),
						Map.entry( "trunc", "GaussDBTruncFunction" ),
						Map.entry( "array", "GaussDBArrayConstructorFunction" ),
						Map.entry( "array_list", "GaussDBArrayConstructorFunction" ),
						Map.entry( "array_contains_nullable", "GaussDBArrayContainsOperatorFunction" ),
						Map.entry( "array_concat", "GaussDBArrayConcatFunction" ),
						Map.entry( "array_prepend", "GaussDBArrayConcatElementFunction" ),
						Map.entry( "array_append", "GaussDBArrayConcatElementFunction" ),
						Map.entry( "array_set", "GaussDBArraySetFunction" ),
						Map.entry( "array_remove", "GaussDBArrayRemoveFunction" ),
						Map.entry( "array_remove_index", "GaussDBArrayRemoveIndexFunction" ),
						Map.entry( "array_replace", "GaussDBArrayReplaceFunction" ),
						Map.entry( "array_fill", "GaussDBArrayFillFunction" ),
						Map.entry( "json_object", "GaussDBJsonObjectFunction" )
				)
		);
	}

	@Test
	void informixAndIrisRetainSpecializedDescriptors() {
		assertRegistrations(
				new InformixDialect(),
				Map.of( "regexp_like", "InformixRegexpLikeFunction" )
		);
		assertRegistrations(
				new InterSystemsIRISDialect(),
				Map.of( "log", "InterSystemsIRISLogFunction" )
		);
	}

	@Test
	void singleStoreRetainsJsonDescriptors() {
		assertRegistrations(
				new SingleStoreDialect(),
				Map.ofEntries(
						Map.entry( "json_object", "SingleStoreJsonObjectFunction" ),
						Map.entry( "json_array", "SingleStoreJsonArrayFunction" ),
						Map.entry( "json_value", "SingleStoreJsonValueFunction" ),
						Map.entry( "json_exists", "SingleStoreJsonExistsFunction" ),
						Map.entry( "json_query", "SingleStoreJsonQueryFunction" ),
						Map.entry( "json_arrayagg", "SingleStoreJsonArrayAggFunction" ),
						Map.entry( "json_objectagg", "SingleStoreJsonObjectAggFunction" ),
						Map.entry( "json_set", "SingleStoreJsonSetFunction" ),
						Map.entry( "json_remove", "SingleStoreJsonRemoveFunction" ),
						Map.entry( "json_mergepatch", "SingleStoreJsonMergepatchFunction" ),
						Map.entry( "json_array_append", "SingleStoreJsonArrayAppendFunction" ),
						Map.entry( "json_array_insert", "SingleStoreJsonArrayInsertFunction" )
				)
		);
	}

	private static void assertRegistrations(Dialect dialect, Map<String, String> expectedDescriptors) {
		final var registry = new SqmFunctionRegistry();
		final var typeConfiguration = new TypeConfiguration();
		typeConfiguration.scope( metadataBuildingContext() );
		dialect.contributeTypes( () -> typeConfiguration, null );
		dialect.initializeFunctionRegistry( new FunctionContributions() {
			@Override
			public SqmFunctionRegistry getFunctionRegistry() {
				return registry;
			}

			@Override
			public TypeConfiguration getTypeConfiguration() {
				return typeConfiguration;
			}

			@Override
			public ServiceRegistry getServiceRegistry() {
				return null;
			}

			@Override
			public Dialect getDialect() {
				return dialect;
			}
		} );

		expectedDescriptors.forEach( (name, expectedType) -> {
			final SqmFunctionDescriptor descriptor = registry.findFunctionDescriptor( name );
			assertThat( descriptor )
					.as( "descriptor registered under %s", name )
					.isNotNull();
			assertThat( descriptor.getClass().getSimpleName() )
					.as( "descriptor type registered under %s", name )
					.isEqualTo( expectedType );
		} );
	}

	private static MetadataBuildingContext metadataBuildingContext() {
		return (MetadataBuildingContext) Proxy.newProxyInstance(
				CommunityFunctionProviderContractsTest.class.getClassLoader(),
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
				CommunityFunctionProviderContractsTest.class.getClassLoader(),
				new Class<?>[] { MetadataBuildingOptions.class },
				(proxy, method, arguments) -> defaultValue( method.getReturnType() )
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
