package org.hibernate.orm.test.generator.batch;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.MapsId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for review findings on batch id generation.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		BatchGenerationReviewTest.Parent.class,
		BatchGenerationReviewTest.Child.class,
		BatchGenerationReviewTest.GParent.class,
		BatchGenerationReviewTest.GChild.class,
		BatchGenerationReviewTest.Assigned.class,
})
class BatchGenerationReviewTest {

	// Finding 5: generators that don't generate on insert must keep the assigned id
	@Test
	void assignedIdIsAvailableImmediately(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Assigned a = new Assigned();
			a.id = 77L;
			session.persist( a );
			assertThat( session.getIdentifier( a ) ).isEqualTo( 77L );
			assertThat( session.contains( a ) ).isTrue();
			assertThat( session.find( Assigned.class, 77L ) ).isSameAs( a );
		} );
	}

	// Finding 9: merge must not cascade to a detached @MapsId parent without CascadeType.MERGE
	@Test
	void mergeDoesNotOverwriteDetachedMapsIdParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent p = new Parent();
			p.id = 10L;
			p.name = "original";
			session.persist( p );
		} );
		final Parent stale = new Parent();
		stale.id = 10L;
		stale.name = "stale";
		final Child child = new Child();
		child.parent = stale;
		child.name = "c";
		scope.inTransaction( session -> {
			session.merge( child );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( Parent.class, 10L ).name ).isEqualTo( "original" );
			assertThat( session.find( Child.class, 10L ) ).isNotNull();
		} );
	}

	// Finding 7: insertMultiple must not insert a parent twice
	@Test
	void insertMultipleParentAndChild(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final GParent p = new GParent();
			p.name = "p";
			final GChild c = new GChild();
			c.parent = p;
			c.name = "c";
			session.insertMultiple( List.of( p, c ) );
		} );
		scope.inTransaction( session -> {
			assertThat( session.createQuery( "select count(p) from BRGParent p", Long.class ).getSingleResult() )
					.isEqualTo( 1L );
			assertThat( session.createQuery( "select count(c) from BRGChild c", Long.class ).getSingleResult() )
					.isEqualTo( 1L );
		} );
	}

	@Entity(name = "BRParent")
	static class Parent {
		@Id
		Long id;
		String name;
	}

	@Entity(name = "BRChild")
	static class Child {
		@Id
		Long id;
		String name;
		@OneToOne
		@MapsId
		Parent parent;
	}

	@Entity(name = "BRGParent")
	static class GParent {
		@Id
		@GeneratedValue
		Long id;
		String name;
	}

	@Entity(name = "BRGChild")
	static class GChild {
		@Id
		Long id;
		String name;
		@OneToOne
		@MapsId
		GParent parent;
	}

	@Entity(name = "BRAssigned")
	static class Assigned {
		@Id
		Long id;
	}
}
