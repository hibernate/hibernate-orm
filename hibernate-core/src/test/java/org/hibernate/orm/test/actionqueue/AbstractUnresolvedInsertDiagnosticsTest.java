package org.hibernate.orm.test.actionqueue;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.TransientPropertyValueException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.annotations.cascade.Child;
import org.hibernate.orm.test.annotations.cascade.Parent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.action.internal.UnresolvedEntityInsertActions.unresolvedInsertException;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies the diagnostics for entity inserts blocked by non-nullable associations.
/// Concrete subclasses select the action-queue implementation.
@JiraKey("HHH-3026")
@DomainModel(annotatedClasses = {
		Parent.class,
		Child.class,
		AbstractUnresolvedInsertDiagnosticsTest.Book.class,
		AbstractUnresolvedInsertDiagnosticsTest.Review.class,
		AbstractUnresolvedInsertDiagnosticsTest.Publisher.class,
		AbstractUnresolvedInsertDiagnosticsTest.Imprint.class
})
@SessionFactory
public abstract class AbstractUnresolvedInsertDiagnosticsTest {

	/// The mapping from the issue: `Parent.defaultChild` and `Child.parent` are both
	/// non-nullable, so neither row can be inserted first.
	@Test
	void cycleIsReported(SessionFactoryScope scope) {
		final var parent = new Parent();
		final var child = new Child( parent );
		parent.setChildren( new HashSet<>() );
		parent.getChildren().add( child );
		parent.setDefaultChild( child );

		final var failure = persistExpectingFailure( scope, parent );

		final String parentName = Parent.class.getName();
		final String childName = Child.class.getName();
		assertThat( failure )
				.hasMessageContaining( "due to a cycle of non-nullable associations: " )
				.hasMessageNotContaining( "unsaved transient instance" );
		// the queue reports whichever of the two blocked inserts it finds first
		if ( parentName.equals( failure.getPropertyOwnerEntityName() ) ) {
			assertThat( failure )
					.hasMessageStartingWith( "Cannot insert '" + parentName + "'" )
					.hasMessageContaining( parentName + ".defaultChild -> " + childName + ".parent -> " + parentName );
			assertThat( failure.getPropertyName() ).isEqualTo( "defaultChild" );
			assertThat( failure.getTransientEntityName() ).isEqualTo( childName );
		}
		else {
			assertThat( failure )
					.hasMessageStartingWith( "Cannot insert '" + childName + "'" )
					.hasMessageContaining( childName + ".parent -> " + parentName + ".defaultChild -> " + childName );
			assertThat( failure.getPropertyOwnerEntityName() ).isEqualTo( childName );
			assertThat( failure.getPropertyName() ).isEqualTo( "parent" );
			assertThat( failure.getTransientEntityName() ).isEqualTo( parentName );
		}
	}

	@Test
	void transientTargetIsReported(SessionFactoryScope scope) {
		final var review = new Review();
		// never persisted, and Review.book does not cascade
		review.book = new Book();

		final var failure = persistExpectingFailure( scope, review );

		final String reviewName = Review.class.getName();
		final String bookName = Book.class.getName();
		assertThat( failure )
				.hasMessageStartingWith( "Cannot insert '" + reviewName + "'" )
				.hasMessageContaining( "non-null association '" + reviewName + ".book'"
						+ " references an unsaved transient instance of '" + bookName + "'" )
				.hasMessageNotContaining( "cycle" );
		assertThat( failure.getPropertyOwnerEntityName() ).isEqualTo( reviewName );
		assertThat( failure.getPropertyName() ).isEqualTo( "book" );
		assertThat( failure.getTransientEntityName() ).isEqualTo( bookName );
	}

	/// The insert of the `Imprint` waits for its `Publisher`, whose own insert is blocked
	/// by a transient `Book`. A queue reports its unresolved inserts in no particular
	/// order, so this builds them by hand and reports the `Imprint`: the message must
	/// then follow the blocked `Publisher` through to the association at fault.
	@Test
	void transientTargetBehindBlockedInsertIsReported(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var publisher = new Publisher();
			final var imprint = new Imprint();
			imprint.publisher = publisher;
			publisher.bestseller = new Book();

			final var unresolvedInserts = new LinkedHashMap<AbstractEntityInsertAction, NonNullableTransientDependencies>();
			final var imprintInsert = unresolvedInsert( imprint, 1L, session, unresolvedInserts );
			unresolvedInsert( publisher, 2L, session, unresolvedInserts );

			final var failure = unresolvedInsertException( imprintInsert, unresolvedInserts );

			final String publisherName = Publisher.class.getName();
			final String imprintName = Imprint.class.getName();
			assertThat( failure )
					.hasMessageStartingWith( "Cannot insert '" + imprintName + "'" )
					.hasMessageContaining( "non-null association '" + publisherName + ".bestseller'"
							+ " references an unsaved transient instance of '" + Book.class.getName() + "'" )
					.hasMessageNotContaining( "cycle" );
			// the structured fields describe the first unresolved association of the reported insert
			assertThat( failure.getPropertyOwnerEntityName() ).isEqualTo( imprintName );
			assertThat( failure.getPropertyName() ).isEqualTo( "publisher" );
			assertThat( failure.getTransientEntityName() ).isEqualTo( publisherName );
		} );
	}

	private static AbstractEntityInsertAction unresolvedInsert(
			Object entity,
			Object id,
			SessionImplementor session,
			Map<AbstractEntityInsertAction, NonNullableTransientDependencies> unresolvedInserts) {
		final var persister = session.getEntityPersister( null, entity );
		final var insert = new EntityInsertAction(
				id, persister.getValues( entity ), entity, null, persister, session.asEventSource() );
		final var dependencies = insert.findNonNullableTransientEntities();
		assertThat( dependencies ).isNotNull();
		unresolvedInserts.put( insert, dependencies );
		return insert;
	}

	private static TransientPropertyValueException persistExpectingFailure(SessionFactoryScope scope, Object entity) {
		return scope.fromSession( session -> {
			session.beginTransaction();
			try {
				final var failure = assertThrows( IllegalStateException.class, () -> session.persist( entity ) );
				assertThat( failure ).hasCauseInstanceOf( TransientPropertyValueException.class );
				return (TransientPropertyValueException) failure.getCause();
			}
			finally {
				session.getTransaction().rollback();
			}
		} );
	}

	@Entity(name = "Book")
	static class Book {
		@Id @GeneratedValue Long id;
	}

	@Entity(name = "Review")
	static class Review {
		@Id @GeneratedValue Long id;
		@ManyToOne(optional = false) Book book;
	}

	@Entity(name = "Publisher")
	static class Publisher {
		@Id @GeneratedValue Long id;
		@ManyToOne(optional = false) Book bestseller;
	}

	@Entity(name = "Imprint")
	static class Imprint {
		@Id @GeneratedValue Long id;
		@ManyToOne(optional = false) Publisher publisher;
	}
}
