/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.mapping.DeclarationRole;
import org.hibernate.mapping.MappingRole;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MappingRoleTests {
	@Test
	void canonicalRenderingRetainsTypedStructure() {
		final MappingRole attribute = MappingRole.entity( "Customer" )
				.appendAttribute( "box" )
				.appendAttribute( "value" );
		final MappingRole collectionElement = MappingRole.collection( "Customer.orders" )
				.append( MappingRole.PartKind.ELEMENT )
				.appendAttribute( "address" )
				.appendAttribute( "city" );

		assertThat( attribute.getFullPath() ).isEqualTo( "entity:Customer#attribute:box.value" );
		assertThat( collectionElement.getFullPath() )
				.isEqualTo( "collection:Customer.orders#element.address.city" );
		assertThat( MappingRole.mappedSuperclass( "Base" ).appendAttribute( "name" ).getFullPath() )
				.isEqualTo( "mapped-superclass:Base#attribute:name" );
		assertThat( collectionElement.getParent() )
				.isEqualTo( MappingRole.collection( "Customer.orders" )
						.append( MappingRole.PartKind.ELEMENT )
						.appendAttribute( "address" ) );
		assertThat( collectionElement.getLocalPart() )
				.isEqualTo( new MappingRole.Part( MappingRole.PartKind.ATTRIBUTE, "city" ) );
		assertThat( MappingRole.entity( "Customer" ).getParent() ).isNull();
		assertThat( MappingRole.entity( "Customer" ).getLocalPart() ).isNull();
	}

	@Test
	void equalityUsesTypedStructure() {
		final MappingRole identifier = MappingRole.entity( "Customer" )
				.append( MappingRole.PartKind.IDENTIFIER );
		final MappingRole attribute = MappingRole.entity( "Customer" )
				.appendAttribute( "identifier" );

		assertThat( identifier ).isNotEqualTo( attribute );
		assertThat( identifier ).isEqualTo(
				MappingRole.entity( "Customer" ).append( MappingRole.PartKind.IDENTIFIER )
		);
	}

	@Test
	void rolesAreSerializableValues() {
		final MappingRole role = MappingRole.collection( "Customer.orders" )
				.append( MappingRole.PartKind.INDEX )
				.appendAttribute( "code" );
		final DeclarationRole declarationRole = new DeclarationRole( "Order", "code" );

		assertThat( SerializationHelper.clone( role ) ).isEqualTo( role );
		assertThat( SerializationHelper.clone( declarationRole ) ).isEqualTo( declarationRole );
	}

	@Test
	void validatesTypedParts() {
		assertThatThrownBy( () -> MappingRole.entity( "Customer" ).append( MappingRole.PartKind.ATTRIBUTE ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "requires a name" );
		assertThatThrownBy( () -> MappingRole.entity( "Customer" )
				.append( MappingRole.PartKind.IDENTIFIER, "id" ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "does not accept a name" );
	}
}
