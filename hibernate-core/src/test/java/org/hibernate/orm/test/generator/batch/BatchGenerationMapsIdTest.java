package org.hibernate.orm.test.generator.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Merging a new child whose {@code @MapsId} association points to a detached parent must not try to
 * persist that parent again, and persisting a child with a transient parent cascades to the parent.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		BatchGenerationMapsIdTest.Parent.class,
		BatchGenerationMapsIdTest.Child.class,
		BatchGenerationMapsIdTest.GParent.class,
		BatchGenerationMapsIdTest.GChild.class,
})
class BatchGenerationMapsIdTest {

	@Test
	void mergeNewChildWithDetachedParent(SessionFactoryScope scope) {
		final Parent detached = scope.fromTransaction( session -> {
			final Parent p = new Parent();
			p.id = 1L;
			session.persist( p );
			return p;
		} );
		scope.inTransaction( session -> {
			final Child child = new Child();
			child.parent = detached;
			final Child merged = session.merge( child );
			session.flush();
			assertThat( merged.id ).isEqualTo( 1L );
		} );
	}

	@Test
	void mergeNewChildWithDetachedGeneratedIdParent(SessionFactoryScope scope) {
		final GParent detached = scope.fromTransaction( session -> {
			final GParent p = new GParent();
			session.persist( p );
			return p;
		} );
		scope.inTransaction( session -> {
			final GChild child = new GChild();
			child.parent = detached;
			final GChild merged = session.merge( child );
			session.flush();
			assertThat( merged.id ).isEqualTo( detached.id );
		} );
	}

	@Test
	void flushTransientMapsIdParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent p = new Parent();
			p.id = 2L;
			final Child child = new Child();
			child.parent = p;
			session.persist( child );
			session.flush();
		} );
	}

	@Test
	void flushTransientMapsIdParentWithGeneratedId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final GParent p = new GParent();
			final GChild child = new GChild();
			child.parent = p;
			session.persist( child );
			session.flush();
		} );
	}

	@Entity(name = "MapsIdGParent")
	static class GParent {
		@Id
		@jakarta.persistence.GeneratedValue
		Long id;
		String name;
	}

	@Entity(name = "MapsIdGChild")
	static class GChild {
		@Id
		Long id;
		String name;
		@OneToOne
		@MapsId
		GParent parent;
	}

	@Entity(name = "MapsIdParent")
	static class Parent {
		@Id
		Long id;
		String name;
	}

	@Entity(name = "MapsIdChild")
	static class Child {
		@Id
		Long id;
		String name;
		@OneToOne
		@MapsId
		Parent parent;
	}
}
