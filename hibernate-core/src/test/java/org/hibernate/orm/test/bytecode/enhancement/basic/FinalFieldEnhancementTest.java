/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.Immutable;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
			FinalFieldEnhancementTest.EntityWithFinalField.class,
				FinalFieldEnhancementTest.EntityWithEmbeddedIdWithFinalField.class, FinalFieldEnhancementTest.EntityWithEmbeddedIdWithFinalField.EmbeddableId.class,
				FinalFieldEnhancementTest.EntityWithEmbeddedNonIdWithFinalField.class, FinalFieldEnhancementTest.EntityWithEmbeddedNonIdWithFinalField.EmbeddableNonId.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class FinalFieldEnhancementTest {

	@Test
	public void entityWithFinalField_constructor() {
		EntityWithFinalField entity = new EntityWithFinalField( "foo" );
		assertThat( entity.immutableProperty ).isEqualTo( "foo" );
	}

	// Just test that the embedded non-ID works correctly over a persist/retrieve cycle
	@Test
	public void entityWithFinalField_smokeTest(SessionFactoryScope scope) {
		EntityWithFinalField persistedEntity = new EntityWithFinalField( "foo" );
		persistedEntity.setName( "Some name" );
		scope.inTransaction( s -> {
			s.persist( persistedEntity );
		} );

		scope.inTransaction( s -> {
			EntityWithFinalField entity = s.find( EntityWithFinalField.class, persistedEntity.getId() );
			assertThat( entity ).extracting( EntityWithFinalField::getImmutableProperty )
					.isEqualTo( persistedEntity.getImmutableProperty() );
		} );
	}

	// Just test that the embedded ID works correctly over a persist/retrieve cycle
	@Test
	public void embeddableIdWithFinalField_smokeTest(SessionFactoryScope scope) {
		EntityWithEmbeddedIdWithFinalField persistedEntity = new EntityWithEmbeddedIdWithFinalField();
		persistedEntity.setName( "Some name" );
		scope.inTransaction( s -> {
			s.persist( persistedEntity );
		} );

		// Read with the same ID instance
		scope.inTransaction( s -> {
			EntityWithEmbeddedIdWithFinalField entity = s.find( EntityWithEmbeddedIdWithFinalField.class, persistedEntity.getId() );
			assertThat( entity ).extracting( EntityWithEmbeddedIdWithFinalField::getId ).extracting( i -> i.id )
					.isEqualTo( persistedEntity.getId().id );
		} );

		// Read with a new ID instance
		scope.inTransaction( s -> {
			EntityWithEmbeddedIdWithFinalField entity = s.find( EntityWithEmbeddedIdWithFinalField.class, EntityWithEmbeddedIdWithFinalField.EmbeddableId.of( persistedEntity.getId().id ) );
			assertThat( entity ).extracting( EntityWithEmbeddedIdWithFinalField::getId ).extracting( i -> i.id )
					.isEqualTo( persistedEntity.getId().id );
		} );

		// Read with a query
		// This is special because in this particular test,
		// we know Hibernate ORM *has to* instantiate the EmbeddableIdType itself:
		// it cannot reuse the ID we passed.
		// And since the EmbeddableIdType has a final field, instantiation will not be able to use a no-arg constructor...
		scope.inTransaction( s -> {
			EntityWithEmbeddedIdWithFinalField entity =
					s.createQuery( "from embidwithfinal e where e.name = :name", EntityWithEmbeddedIdWithFinalField.class )
							.setParameter( "name", persistedEntity.getName() )
							.uniqueResult();
			assertThat( entity ).extracting( EntityWithEmbeddedIdWithFinalField::getId ).extracting( i -> i.id )
					.isEqualTo( persistedEntity.getId().id );
		} );
	}

	@Test
	public void embeddableNonIdWithFinalField_constructor() {
		EntityWithEmbeddedNonIdWithFinalField.EmbeddableNonId embeddable =
				new EntityWithEmbeddedNonIdWithFinalField.EmbeddableNonId( "foo" );
		assertThat( embeddable.immutableProperty ).isEqualTo( "foo" );
	}

	// Just test that the embedded non-ID works correctly over a persist/retrieve cycle
	@Test
	public void embeddableNonIdWithFinalField_smokeTest(SessionFactoryScope scope) {
		EntityWithEmbeddedNonIdWithFinalField persistedEntity = new EntityWithEmbeddedNonIdWithFinalField();
		persistedEntity.setName( "Some name" );
		persistedEntity.setEmbedded( new EntityWithEmbeddedNonIdWithFinalField.EmbeddableNonId( "foo" ) );
		scope.inTransaction( s -> {
			s.persist( persistedEntity );
		} );

		scope.inTransaction( s -> {
			EntityWithEmbeddedNonIdWithFinalField entity = s.find( EntityWithEmbeddedNonIdWithFinalField.class, persistedEntity.getId() );
			assertThat( entity ).extracting( EntityWithEmbeddedNonIdWithFinalField::getEmbedded )
					.extracting( EntityWithEmbeddedNonIdWithFinalField.EmbeddableNonId::getImmutableProperty )
					.isEqualTo( persistedEntity.getEmbedded().getImmutableProperty() );
		} );
	}

	@Entity(name = "withfinal")
	public static class EntityWithFinalField {

		@Id
		@GeneratedValue
		private Long id;

		private final String immutableProperty;

		private String name;

		// For Hibernate ORM
		protected EntityWithFinalField() {
			this.immutableProperty = null;
		}

		private EntityWithFinalField(String id) {
			this.immutableProperty = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getImmutableProperty() {
			return immutableProperty;
		}
	}

	@Entity(name = "embidwithfinal")
	public static class EntityWithEmbeddedIdWithFinalField {

		@EmbeddedId
		private EmbeddableId id;

		private String name;

		public EntityWithEmbeddedIdWithFinalField() {
			this.id = EmbeddableId.of( SafeRandomUUIDGenerator.safeRandomUUIDAsString() );
		}

		public EmbeddableId getId() {
			return id;
		}

		public void setId(EmbeddableId id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Immutable
		@Embeddable
		public static class EmbeddableId implements Serializable {
			private final String id;

			// For Hibernate ORM
			protected EmbeddableId() {
				this.id = null;
			}

			private EmbeddableId(String id) {
				this.id = id;
			}

			public static EmbeddableId of(String string) {
				return new EmbeddableId( string );
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( !( o instanceof EmbeddableId ) ) {
					return false;
				}
				EmbeddableId embeddableIdType = (EmbeddableId) o;
				return Objects.equals( id, embeddableIdType.id );
			}

			@Override
			public int hashCode() {
				return Objects.hash( id );
			}
		}
	}

	@Entity(name = "embwithfinal")
	public static class EntityWithEmbeddedNonIdWithFinalField {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		private EmbeddableNonId embedded;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public EmbeddableNonId getEmbedded() {
			return embedded;
		}

		public void setEmbedded(
				EmbeddableNonId embedded) {
			this.embedded = embedded;
		}

		@Embeddable
		public static class EmbeddableNonId {
			private final String immutableProperty;

			private String mutableProperty;

			protected EmbeddableNonId() {
				// For Hibernate ORM only - it will change the property value through reflection
				this.immutableProperty = null;
			}

			private EmbeddableNonId(String immutableProperty) {
				this.immutableProperty = immutableProperty;
			}

			public String getImmutableProperty() {
				return immutableProperty;
			}

			public String getMutableProperty() {
				return mutableProperty;
			}

			public void setMutableProperty(String mutableProperty) {
				this.mutableProperty = mutableProperty;
			}
		}
	}

}
