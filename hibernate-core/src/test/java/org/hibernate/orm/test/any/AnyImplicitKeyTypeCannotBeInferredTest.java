/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.hibernate.MappingException;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.boot.MetadataSources;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.annotations.AnyDiscriminatorImplicitValues.Strategy.CUSTOM;

/**
 * When the key type of an {@code @Any} cannot be inferred, the failure is reported at boot,
 * not when the association is first read or written.
 */
@ServiceRegistry
@JiraKey("HHH-20319")
class AnyImplicitKeyTypeCannotBeInferredTest {

	@Test
	void targetEntitiesWithDifferentIdentifierTypes(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> buildMetadata( scope, ClosedShelf.class, Book.class, Disc.class ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "is identified by" );
	}

	@Test
	void targetEntityWithEmbeddableIdentifier(ServiceRegistryScope scope) {
		// a single column, whose type must not be mistaken for the type of the identifier
		assertThatThrownBy( () -> buildMetadata( scope, EditionShelf.class, Edition.class ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "Edition' has an embeddable identifier" );
	}

	@Test
	void declaringEntityWithEmbeddableIdentifier(ServiceRegistryScope scope) {
		// a single column, whose type must not be mistaken for the type of the identifier
		assertThatThrownBy( () -> buildMetadata( scope, Manuscript.class ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "Manuscript' has an embeddable identifier" );
	}

	@Test
	void customImplicitDiscriminatorStrategy(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> buildMetadata( scope, CustomShelf.class ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "Could not determine key type for '@Any' mapping" );
	}

	private static void buildMetadata(ServiceRegistryScope scope, Class<?>... annotatedClasses) {
		new MetadataSources( scope.getRegistry() )
				.addAnnotatedClasses( annotatedClasses )
				.buildMetadata();
	}

	@Entity(name = "ClosedShelf")
	static class ClosedShelf {

		@Id
		Long id;

		@Any
		@JoinColumn(name = "CONTENT_ID")
		@Column(name = "CONTENT_TYPE")
		@AnyDiscriminatorValue(discriminator = "B", entity = Book.class)
		@AnyDiscriminatorValue(discriminator = "D", entity = Disc.class)
		Object content;
	}

	@Entity(name = "EditionShelf")
	static class EditionShelf {

		@Id
		Long id;

		@Any
		@JoinColumn(name = "CONTENT_ID")
		@Column(name = "CONTENT_TYPE")
		@AnyDiscriminatorValue(discriminator = "E", entity = Edition.class)
		Object content;
	}

	@Entity(name = "CustomShelf")
	static class CustomShelf {

		@Id
		Long id;

		@Any
		@JoinColumn(name = "CONTENT_ID")
		@Column(name = "CONTENT_TYPE")
		@AnyDiscriminatorImplicitValues(value = CUSTOM, implementation = CustomStrategy.class)
		Object content;
	}

	@Entity(name = "Book")
	static class Book {

		@Id
		String isbn;
	}

	@Entity(name = "Disc")
	static class Disc {

		@Id
		Long id;
	}

	@Entity(name = "Edition")
	static class Edition {

		@EmbeddedId
		EditionId id;
	}

	@Entity(name = "Manuscript")
	static class Manuscript {

		@EmbeddedId
		EditionId id;

		@Any
		@JoinColumn(name = "ORIGIN_ID")
		@Column(name = "ORIGIN_TYPE")
		Object origin;
	}

	@Embeddable
	static class EditionId implements Serializable {

		String isbn;
	}

	public static class CustomStrategy implements ImplicitDiscriminatorStrategy {

		@Override
		public Object toDiscriminatorValue(
				EntityMappingType entityMapping,
				NavigableRole discriminatorRole,
				MappingMetamodelImplementor mappingModel) {
			return entityMapping.getEntityName();
		}

		@Override
		public EntityMappingType toEntityMapping(
				Object discriminatorValue,
				NavigableRole discriminatorRole,
				MappingMetamodelImplementor mappingModel) {
			return mappingModel.getEntityDescriptor( (String) discriminatorValue );
		}
	}
}
