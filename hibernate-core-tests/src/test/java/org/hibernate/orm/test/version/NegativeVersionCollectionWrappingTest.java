package org.hibernate.orm.test.version;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies version seeding does not skip wrapping a to-many collection during persist.
///
/// @author Steve Ebersole
@JiraKey("HHH-9944")
@DomainModel(annotatedClasses = {
		NegativeVersionCollectionWrappingTest.Parent.class,
		NegativeVersionCollectionWrappingTest.Child.class
})
@SessionFactory
class NegativeVersionCollectionWrappingTest {
	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(ints = { -1, 0 })
	void testCollectionWrappedBeforeFlush(int initialVersion, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var parent = new Parent();
			parent.id = 1L;
			parent.version = initialVersion;
			final var first = new Child( 1L, parent );
			final var second = new Child( 2L, parent );
			parent.children.add( first );
			parent.children.add( second );

			session.persist( parent );

			// Check before flush: flushing must not be what eventually wraps the collection.
			assertThat( parent.version ).isZero();
			assertThat( parent.children ).isInstanceOf( PersistentCollection.class );
			assertThat( parent.children ).containsExactly( first, second );
			session.flush();

			parent.children.remove( first );
			session.flush();
			session.clear();
			assertThat( session.find( Child.class, first.id ) ).isNull();
			assertThat( session.find( Parent.class, parent.id ).children )
					.extracting( child -> child.id ).containsExactly( second.id );
		} );
		scope.inTransaction( session -> {
			assertThat( session.find( Parent.class, 1L ).children )
					.extracting( child -> child.id ).containsExactly( 2L );
			assertThat( session.createSelectionQuery( "from NegativeVersionChild", Child.class ).list() )
					.extracting( child -> child.id ).containsExactly( 2L );
		} );
	}

	@Entity(name = "NegativeVersionParent")
	static class Parent {
		@Id
		Long id;

		@Version
		int version;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		List<Child> children = new ArrayList<>();
	}

	@Entity(name = "NegativeVersionChild")
	static class Child {
		@Id
		Long id;

		@ManyToOne
		Parent parent;

		Child() {
		}

		Child(Long id, Parent parent) {
			this.id = id;
			this.parent = parent;
		}
	}
}
