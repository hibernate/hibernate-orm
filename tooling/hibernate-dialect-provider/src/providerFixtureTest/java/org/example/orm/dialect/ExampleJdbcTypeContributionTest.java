/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.type.spi.H2JdbcTypes;
import org.hibernate.dialect.type.spi.SQLServerJdbcTypes;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies that an external Dialect can contribute Hibernate's stock
/// dialect-specific JDBC types without importing their implementations.
///
/// @author Steve Ebersole
public class ExampleJdbcTypeContributionTest {
	@Test
	void contributesStockJdbcTypeAndConstructorThroughSupportedFacades() {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		new ExampleDialect().contributeTypes( () -> typeConfiguration, null );

		assertSame(
				H2JdbcTypes.json(),
				typeConfiguration.getJdbcTypeRegistry().getDescriptor( SqlTypes.JSON )
		);
		assertSame(
				SQLServerJdbcTypes.castingXmlArrayConstructor(),
				typeConfiguration.getJdbcTypeRegistry().getConstructor( SqlTypes.XML_ARRAY )
		);
	}

	@Test
	void contributesProviderDefinedJavaAndJdbcDescriptorsThroughTheSpiContext() {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		new ExampleTypeContributor().contribute( () -> typeConfiguration, null );

		assertSame(
				ExampleJavaType.INSTANCE,
				typeConfiguration.getJavaTypeRegistry().findDescriptor( ExampleTypeValue.class )
		);
		assertSame(
				ExampleJdbcType.INSTANCE,
				typeConfiguration.getJdbcTypeRegistry().getDescriptor( ExampleJdbcType.TYPE_CODE )
		);
		assertSame(
				ExampleJdbcTypeConstructor.INSTANCE,
				typeConfiguration.getJdbcTypeRegistry().getConstructor( ExampleJdbcTypeConstructor.TYPE_CODE )
		);
	}
}
