/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the DDL descriptors registered by the standalone provider fixture.
///
/// @author Steve Ebersole
public class ExampleDdlTypeConstructionTest {
	private static final int SIMPLE_TYPE = 60_001;
	private static final int CAPACITY_TYPE = 60_002;

	@Test
	void registersSimpleAndCapacityDependentDescriptors() {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		new ExampleDialect().contributeTypes( () -> typeConfiguration, null );
		final DdlTypeRegistry registry = typeConfiguration.getDdlTypeRegistry();

		final DdlType simple = registry.getDescriptor( SIMPLE_TYPE );
		assertEquals( "fixture_simple", registry.getTypeName( SIMPLE_TYPE, Size.nil(), null ) );
		assertEquals(
				"fixture_simple_cast",
				simple.getCastTypeName(
						Size.nil(),
						typeConfiguration.getBasicTypeForJavaType( String.class ),
						registry
				)
		);

		final DdlType capacity = registry.getDescriptor( CAPACITY_TYPE );
		assertEquals( "fixture_varchar(64)", registry.getTypeName( CAPACITY_TYPE, Size.length( 64 ), null ) );
		assertEquals( "fixture_text", registry.getTypeName( CAPACITY_TYPE, Size.length( 65 ), null ) );
		assertEquals( "fixture_lob", registry.getTypeName( CAPACITY_TYPE, Size.length( 1_025 ), null ) );
		assertFalse( capacity.isLob( Size.length( 1_024 ) ) );
		assertTrue( capacity.isLob( Size.length( 1_025 ) ) );
	}
}
