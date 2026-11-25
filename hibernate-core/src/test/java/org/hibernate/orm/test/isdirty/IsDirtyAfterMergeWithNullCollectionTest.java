package org.hibernate.orm.test.isdirty;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@Jpa(annotatedClasses = IsDirtyAfterMergeWithNullCollectionTest.ExampleEntity.class)
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@JiraKey("HHH-11310")
class IsDirtyAfterMergeWithNullCollectionTest {

	@Test
	@FailureExpected(jiraKey = "HHH-11310")
	void mergeInOriginalEntityManager(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final ExampleEntity original = new ExampleEntity();
			scope.inTransaction( em, txEm -> txEm.persist( original ) );

			final ExampleEntity found = em.find( ExampleEntity.class, original.id );
			assertSame( original, found );
			assertNull( found.children );
			assertCleanAfterDetachAndMerge( em, found );
		} );
	}

	@Test
	void mergeInFreshEntityManager(EntityManagerFactoryScope scope) {
		final ExampleEntity original = new ExampleEntity();
		scope.inTransaction( em -> em.persist( original ) );

		scope.inEntityManager( em -> {
			final ExampleEntity found = em.find( ExampleEntity.class, original.id );
			assertNotNull( found.children );
			assertFalse( Hibernate.isInitialized( found.children ) );
			assertCleanAfterDetachAndMerge( em, found );
		} );
	}

	private static void assertCleanAfterDetachAndMerge(EntityManager em, ExampleEntity found) {
		final Session session = em.unwrap( Session.class );
		em.detach( found );
		assertFalse( session.isDirty(), "Session is dirty after detach" );
		em.merge( found );
		assertFalse( session.isDirty(), "Session is dirty after merge" );
	}

	@AfterEach
	void cleanup(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Entity(name = "ExampleEntity")
	static class ExampleEntity {
		@Id
		Long id = 1L;
		@ManyToOne
		ExampleEntity parent;
		@OneToMany(mappedBy = "parent")
		Set<ExampleEntity> children;
	}
}
