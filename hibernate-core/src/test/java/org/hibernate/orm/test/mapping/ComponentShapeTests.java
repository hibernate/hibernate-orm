/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ComponentShapeState;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentShapeTests {
	@Test
	void completionFinalizesPropertyOrderAndIsIdempotent() {
		final Component component = component();
		component.addProperty( property( "zeta" ) );
		component.addProperty( property( "alpha" ) );

		assertEquals( ComponentShapeState.BUILDING, component.getShapeState() );
		assertFalse( component.isShapeComplete() );
		assertThrows( IllegalStateException.class, component::requireShapeComplete );

		component.completeShape();

		assertEquals( ComponentShapeState.COMPLETE, component.getShapeState() );
		assertTrue( component.isShapeComplete() );
		component.requireShapeComplete();
		assertEquals( "alpha", component.getProperties().get( 0 ).getName() );
		assertEquals( "zeta", component.getProperties().get( 1 ).getName() );
		component.completeShape();
	}

	@Test
	void completionPreventsShapeMutation() {
		final Component component = component();
		component.completeShape();

		assertThrows( IllegalStateException.class, () -> component.addProperty( property( "name" ) ) );
		assertThrows( IllegalStateException.class, component::clearProperties );
		assertThrows( IllegalStateException.class, () -> component.replaceProperties( property -> property ) );
		assertThrows( IllegalStateException.class, () -> component.setFlattened( true ) );
		assertThrows( IllegalStateException.class, () -> component.setPreservePropertyOrder( true ) );
		assertThrows( IllegalStateException.class, () -> component.setComponentClassDetails( null ) );
		assertThrows( IllegalStateException.class, () -> component.setCustomInstantiator( null ) );
		assertThrows( IllegalStateException.class, () -> component.setInstantiator( null, null ) );
		component.setAlternateUniqueKey( true );
		assertTrue( component.isAlternateUniqueKey() );
		assertThrows( IllegalStateException.class, () -> component.setDiscriminator( null ) );
		assertThrows( UnsupportedOperationException.class, () -> component.getProperties().clear() );
	}

	@Test
	void copyStartsBuilding() {
		final Component component = component();
		component.addProperty( property( "name" ) );
		component.completeShape();

		final Component copy = component.copy();

		assertEquals( ComponentShapeState.BUILDING, copy.getShapeState() );
		copy.clearProperties();
		copy.addProperty( property( "replacement" ) );
		copy.completeShape();
		assertTrue( copy.isShapeComplete() );
	}

	private static Component component() {
		final MetadataBuildingContext context = mock( MetadataBuildingContext.class );
		when( context.getMetadataCollector() ).thenReturn( mock( InFlightMetadataCollector.class ) );
		return new Component( context, new Table( "component_shape" ), null );
	}

	private static Property property(String name) {
		final Property property = new Property();
		property.setName( name );
		return property;
	}
}
