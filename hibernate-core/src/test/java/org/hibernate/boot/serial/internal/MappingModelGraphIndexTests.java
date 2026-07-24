/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.mapping.internal.jpa.JpaStaticMetamodelInjectionSource;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.AppliedMappingPart;
import org.hibernate.mapping.MappingRole;
import org.hibernate.metamodel.internal.AttributeUsageHandoff;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = MappingModelGraphIndexTests.IndexedEntity.class)
class MappingModelGraphIndexTests {
	@Test
	void duplicateStableRoleIsRejected() {
		final var index = new LinkedHashMap<MappingRole, Object>();
		final MappingRole role = MappingRole.entity( "Book" ).appendAttribute( "title" );
		MappingModelGraphIndex.putUnique( index, role, new Object(), "basic value" );

		assertThatThrownBy( () -> MappingModelGraphIndex.putUnique(
				index,
				role,
				new Object(),
				"basic value"
		) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( role.getFullPath() );
	}

	@Test
	void indexesBasicValuesAndPropertiesByTypedRole(DomainModelScope scope) {
		final MetadataImplementor metadata = scope.getDomainModel();
		final var entity = metadata.getEntityBinding( IndexedEntity.class.getName() );
		final var property = entity.getProperty( "name" );
		final MappingRole role = property.getMappingRole();
		final var index = MappingModelGraphIndex.from( metadata );

		assertThat( role ).isNotNull();
		assertThat( index.property( role ) ).isSameAs( property );
		assertThat( index.basicValuesByRole() ).containsEntry(
				role,
				List.of( (org.hibernate.mapping.BasicValue) property.getValue() )
		);
		assertThat( index.role( (org.hibernate.mapping.BasicValue) property.getValue() ) ).isEqualTo( role );
	}

	@Test
	void missingIntrinsicPropertyRoleIsRejected(DomainModelScope scope) {
		final MetadataImplementor metadata = scope.getDomainModel();
		final var property = metadata.getEntityBinding( IndexedEntity.class.getName() ).getIdentifierProperty();
		final var value = (AppliedMappingPart) property.getValue();
		final MappingRole propertyRole = property.getMappingRole();
		final MappingRole valueRole = value.getMappingRole();
		property.setMappingRole( null );
		value.setMappingRole( null );
		try {
			assertThatThrownBy( () -> MappingModelGraphIndex.from( metadata ) )
					.isInstanceOf( IllegalStateException.class )
					.hasMessageContaining( "missing intrinsic mapping role" );
		}
		finally {
			property.setMappingRole( propertyRole );
			value.setMappingRole( valueRole );
		}
	}

	@Test
	void inconsistentIntrinsicPropertyRoleIsRejected(DomainModelScope scope) {
		final MetadataImplementor metadata = scope.getDomainModel();
		final var property = metadata.getEntityBinding( IndexedEntity.class.getName() ).getProperty( "name" );
		final MappingRole role = property.getMappingRole();
		property.setMappingRole( MappingRole.entity( "other" ).appendAttribute( "name" ) );
		try {
			assertThatThrownBy( () -> MappingModelGraphIndex.from( metadata ) )
					.isInstanceOf( IllegalStateException.class )
					.hasMessageContaining( "Structurally inconsistent property" )
					.hasMessageContaining( "entity:other#attribute:name" );
		}
		finally {
			property.setMappingRole( role );
		}
	}

	@Test
	void unknownRuntimeAttributeRoleIsRejected(DomainModelScope scope) {
		final MappingRole role = MappingRole.entity( "missing" ).appendAttribute( "name" );
		final var snapshot = new RuntimeMappingHandoffSnapshot(
				Map.of( role, new AttributeUsageHandoff( null, null, null ) ),
				Set.of(),
				new JpaStaticMetamodelInjectionSource( List.of() )
		);

		assertThatThrownBy( () -> snapshot.resolveAgainst( scope.getDomainModel() ) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( "attribute handoff" )
				.hasMessageContaining( role.getFullPath() );
	}

	@Test
	void unknownRuntimeEmbeddableRoleIsRejected(DomainModelScope scope) {
		final MappingRole role = MappingRole.entity( "missing" ).appendAttribute( "details" );
		final var snapshot = new RuntimeMappingHandoffSnapshot(
				Map.of(),
				Set.of( role ),
				new JpaStaticMetamodelInjectionSource( List.of() )
		);

		assertThatThrownBy( () -> snapshot.resolveAgainst( scope.getDomainModel() ) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( "embeddable handoff" )
				.hasMessageContaining( role.getFullPath() );
	}

	@Entity(name = "IndexedEntity")
	static class IndexedEntity {
		@Id
		private Long id;
		private String name;
	}
}
