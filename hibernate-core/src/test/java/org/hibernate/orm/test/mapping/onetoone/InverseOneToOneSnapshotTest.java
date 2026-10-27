package org.hibernate.orm.test.mapping.onetoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(annotatedClasses = {
		InverseOneToOneSnapshotTest.Parent.class,
		InverseOneToOneSnapshotTest.Child.class
})
@SessionFactory
class InverseOneToOneSnapshotTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parentWithoutChild = new Parent();
			parentWithoutChild.id = 1;
			session.persist( parentWithoutChild );

			final var parentWithChild = new Parent();
			parentWithChild.id = 2;
			final var child = new Child();
			child.id = 3;
			child.parent = parentWithChild;
			parentWithChild.child = child;
			session.persist( parentWithChild );
			session.persist( child );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void absentInverseAssociationHasNullSnapshotValue(SessionFactoryScope scope) {
		scope.inSession( session -> assertNull( session.find( Parent.class, 1 ).child ) );
		assertInverseSnapshotIsNull( scope, 1 );
	}

	@Test
	void presentInverseAssociationHasNullSnapshotValue(SessionFactoryScope scope) {
		scope.inSession( session -> assertEquals( 3, session.find( Parent.class, 2 ).child.id ) );
		// The snapshot omits the inverse association even when a child exists.
		assertInverseSnapshotIsNull( scope, 2 );
	}

	@Test
	void owningAssociationSnapshotContainsForeignKey(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var persister = session.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( Child.class );
			final Object[] snapshot = session.getPersistenceContextInternal().getDatabaseSnapshot( 3, persister );
			assertNotNull( snapshot );
			assertEquals( 2, snapshot[persister.getPropertyIndex( "parent" )] );
		} );
	}

	private void assertInverseSnapshotIsNull(SessionFactoryScope scope, int id) {
		scope.inSession( session -> {
			final var persister = session.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( Parent.class );
			final Object[] snapshot = session.getPersistenceContextInternal().getDatabaseSnapshot( id, persister );
			assertNotNull( snapshot );
			assertNull( snapshot[persister.getPropertyIndex( "child" )],
					"An unselected inverse association must not acquire the numeric default of its foreign key type" );
		} );
	}

	@Entity(name = "SnapshotParent")
	static class Parent {
		@Id
		Integer id;

		@OneToOne(mappedBy = "parent")
		Child child;
	}

	@Entity(name = "SnapshotChild")
	static class Child {
		@Id
		Integer id;

		@OneToOne
		Parent parent;
	}
}
