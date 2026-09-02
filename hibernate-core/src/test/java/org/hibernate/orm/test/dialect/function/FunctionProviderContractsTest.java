/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests provider-facing function registration behavior.
///
/// @author Steve Ebersole
public class FunctionProviderContractsTest {
	@Test
	void commonFactoryRegistersRepresentativeStockFamilies() {
		final SqmFunctionRegistry registry = new SqmFunctionRegistry();
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		final FunctionContributions contributions = new FunctionContributions() {
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
		};

		final CommonFunctionFactory factory = new CommonFunctionFactory( contributions );
		factory.aggregates( new H2Dialect(), SqlAstNodeRenderingMode.DEFAULT );
		factory.cot();
		factory.array();
		factory.jsonObject();
		factory.xmlelement();

		assertThat( registry.findFunctionDescriptor( "count" ) ).isNotNull();
		assertThat( registry.findFunctionDescriptor( "cot" ) ).isNotNull();
		assertThat( registry.findFunctionDescriptor( "array" ) ).isNotNull();
		assertThat( registry.findFunctionDescriptor( "json_object" ) ).isNotNull();
		assertThat( registry.findFunctionDescriptor( "xmlelement" ) ).isNotNull();
	}

	@Test
	void registryPreservesReplacementAndAlternateKeyBehavior() {
		final var registry = new SqmFunctionRegistry();
		final var first = registry.namedDescriptorBuilder( "first" ).register();
		final var second = registry.namedDescriptorBuilder( "second" ).register();

		registry.register( "target", first );
		registry.registerAlternateKey( "alternate", "target" );
		assertThat( registry.findFunctionDescriptor( "alternate" ) ).isSameAs( first );

		registry.register( "target", second );
		assertThat( registry.findFunctionDescriptor( "target" ) ).isSameAs( second );
		assertThat( registry.findFunctionDescriptor( "alternate" ) ).isSameAs( second );
		assertThat( registry.wrapInJdbcEscape( "escaped", second ) ).isSameAs(
				registry.findFunctionDescriptor( "escaped" )
		);
	}
}
