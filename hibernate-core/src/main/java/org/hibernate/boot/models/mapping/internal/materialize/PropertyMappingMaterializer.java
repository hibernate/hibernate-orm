/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.materialize;

import org.hibernate.mapping.Property;
import org.hibernate.mapping.SyntheticProperty;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Access;

import static org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies.EMBEDDED;
import static org.hibernate.spi.NavigablePath.IDENTIFIER_MAPPER_PROPERTY;

/// Materializes legacy `Property` mapping objects from binding/view facts.
///
/// This is intentionally small for the first attribute-materialization slice:
/// value creation still belongs to the existing value-specific binders and
/// materializers, while this class centralizes the repeated property object
/// creation, accessor setup, and synthetic identifier-mapper property setup.
///
/// @since 9.0
/// @author Steve Ebersole
public class PropertyMappingMaterializer {
	public Property createProperty(String name, MemberDetails member) {
		final Property property = new Property();
		property.setName( name );
		bindPropertyAccessor( member, property );
		return property;
	}

	public Property createProperty(String name, Value value, MemberDetails member) {
		final Property property = createProperty( name, member );
		property.setValue( value );
		return property;
	}

	public Property createReadOnlyProperty(String name, Value value, MemberDetails member) {
		final Property property = createProperty( name, value, member );
		property.setInsertable( false );
		property.setUpdatable( false );
		return property;
	}

	public SyntheticProperty createIdentifierMapperProperty(Value identifierMapper) {
		final SyntheticProperty property = new SyntheticProperty();
		property.setName( IDENTIFIER_MAPPER_PROPERTY );
		property.setUpdatable( false );
		property.setInsertable( false );
		property.setPropertyAccessorName( EMBEDDED.getExternalName() );
		property.setValue( identifierMapper );
		return property;
	}

	public void bindPropertyAccessor(MemberDetails member, Property property) {
		final Access access = member.getDirectAnnotationUsage( Access.class );
		if ( access != null ) {
			property.setPropertyAccessorName( access.value() == jakarta.persistence.AccessType.FIELD ? "field" : "property" );
		}
		else {
			property.setPropertyAccessorName( member.isField() ? "field" : "property" );
		}
	}
}
