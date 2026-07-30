/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.attributebinder;

import java.lang.annotation.Annotation;

import org.hibernate.AnnotationException;
import org.hibernate.binder.EmbeddableBindingContext;
import org.hibernate.binder.EntityBindingContext;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.mapping.spi.CategorizedDomainModel;
import org.hibernate.boot.mapping.spi.EmbeddableUsageMetadata;
import org.hibernate.boot.mapping.spi.EntityTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.spi.ClassDetails;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BinderContextContractTests {
	private final Annotation annotation = mock( Annotation.class );
	private final TypeBinder<Annotation> binder = new TypeBinder<>() {
	};

	@Test
	void defaultEntityCallbackRejectsUnsupportedPlacement() {
		final EntityTypeMetadata entityType = mock( EntityTypeMetadata.class );
		final ClassDetails classDetails = mock( ClassDetails.class );
		when( entityType.getClassDetails() ).thenReturn( classDetails );
		when( classDetails.getName() ).thenReturn( "example.Customer" );
		when( annotation.toString() ).thenReturn( "@Example" );

		assertThatThrownBy( () -> binder.bind( annotation, new EntityContext( entityType ) ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessage( "Annotation '@Example' may not be applied to entity type 'example.Customer'" );
	}

	@Test
	void defaultEmbeddableCallbackRejectsUnsupportedPlacement() {
		final EmbeddableUsageMetadata usage = mock( EmbeddableUsageMetadata.class );
		final org.hibernate.boot.mapping.spi.EmbeddableTypeMetadata type =
				mock( org.hibernate.boot.mapping.spi.EmbeddableTypeMetadata.class );
		final ClassDetails classDetails = mock( ClassDetails.class );
		when( usage.type() ).thenReturn( type );
		when( type.getClassDetails() ).thenReturn( classDetails );
		when( classDetails.getName() ).thenReturn( "example.Address" );
		when( annotation.toString() ).thenReturn( "@Example" );

		assertThatThrownBy( () -> binder.bind( annotation, new EmbeddableContext( usage ) ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessage( "Annotation '@Example' may not be applied to embeddable type 'example.Address'" );
	}

	private record EntityContext(EntityTypeMetadata getEntityType) implements EntityBindingContext {
		@Override
		public CategorizedDomainModel getDomainModel() {
			return null;
		}

		@Override
		public PersistentClass getPersistentClass() {
			return null;
		}

		@Override
		public MetadataBuildingContext getMetadataBuildingContext() {
			return null;
		}
	}

	private record EmbeddableContext(EmbeddableUsageMetadata getEmbeddableUsage)
			implements EmbeddableBindingContext {
		@Override
		public CategorizedDomainModel getDomainModel() {
			return null;
		}

		@Override
		public PersistentClass getPersistentClass() {
			return null;
		}

		@Override
		public Component getComponent() {
			return null;
		}

		@Override
		public MetadataBuildingContext getMetadataBuildingContext() {
			return null;
		}
	}
}
