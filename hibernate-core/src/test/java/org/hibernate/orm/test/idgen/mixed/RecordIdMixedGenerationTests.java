package org.hibernate.orm.test.idgen.mixed;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test identity generation in a composite id mapped as a record,
 * when the other attributes of the id are assigned.
 */
@JiraKey("HHH-21003")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@Jpa(annotatedClasses = {
		RecordIdMixedGenerationTests.IdClassAnimal.class,
		RecordIdMixedGenerationTests.EmbeddedIdAnimal.class
})
class RecordIdMixedGenerationTests {
	@Test
	void testIdClass(EntityManagerFactoryScope scope) {
		var id = scope.fromTransaction( em -> {
			IdClassAnimal animal = new IdClassAnimal( 5L );
			em.persist( animal );
			assertNotNull( animal.generatedId );
			assertEquals( 5L, animal.assignedId );
			return new IdClassId( animal.generatedId, animal.assignedId );
		} );
		scope.inTransaction( em -> {
			IdClassAnimal animal = em.find( IdClassAnimal.class, id );
			assertNotNull( animal );
			assertEquals( id.generatedId(), animal.generatedId );
			assertEquals( 5L, animal.assignedId );
		} );
	}

	@Test
	void testEmbeddedId(EntityManagerFactoryScope scope) {
		var id = scope.fromTransaction( em -> {
			EmbeddedIdAnimal animal = new EmbeddedIdAnimal( new AnimalId( null, 7L ) );
			em.persist( animal );
			assertNotNull( animal.id.generatedId() );
			assertEquals( 7L, animal.id.assignedId() );
			return animal.id;
		} );
		scope.inTransaction( em -> {
			EmbeddedIdAnimal animal = em.find( EmbeddedIdAnimal.class, id );
			assertNotNull( animal );
			assertEquals( id, animal.id );
		} );
	}

	@Entity(name = "IdClassAnimal")
	// MySQL requires the identity column to be the first one of the primary key
	@Table(name = "record_idclass_animals",
			uniqueConstraints = @UniqueConstraint(columnNames = {"generatedId", "assigned_id"}))
	@IdClass(IdClassId.class)
	static class IdClassAnimal {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long generatedId;
		@Id
		@Column(name = "assigned_id")
		private Long assignedId;

		IdClassAnimal(Long assignedId) {
			this.assignedId = assignedId;
		}

		IdClassAnimal() {
		}
	}

	record IdClassId(Long generatedId, Long assignedId) {
	}

	@Entity(name = "EmbeddedIdAnimal")
	// MySQL requires the identity column to be the first one of the primary key
	@Table(name = "record_embeddedid_animals",
			uniqueConstraints = @UniqueConstraint(columnNames = {"generatedId", "assigned_id"}))
	static class EmbeddedIdAnimal {
		@EmbeddedId
		private AnimalId id;

		EmbeddedIdAnimal(AnimalId id) {
			this.id = id;
		}

		EmbeddedIdAnimal() {
		}
	}

	@Embeddable
	record AnimalId(
			@GeneratedValue(strategy = GenerationType.IDENTITY) Long generatedId,
			@Column(name = "assigned_id") Long assignedId) {
	}
}
