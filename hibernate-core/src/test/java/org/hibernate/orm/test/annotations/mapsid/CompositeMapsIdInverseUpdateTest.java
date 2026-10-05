package org.hibernate.orm.test.annotations.mapsid;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Regression for updating a child loaded through the inverse association when
/// both entities share a composite identifier using `@MapsId`.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		CompositeMapsIdInverseUpdateTest.Parent.class,
		CompositeMapsIdInverseUpdateTest.Child.class,
		CompositeMapsIdInverseUpdateTest.Key.class
})
@SessionFactory
@JiraKey("HHH-9509")
public class CompositeMapsIdInverseUpdateTest {
	@Test
	void testUpdateChildThroughParent(SessionFactoryScope scope) {
		Key key = new Key( 1L, 2L );
		scope.inTransaction( session -> {
			Parent parent = new Parent();
			parent.id = key;
			Child child = new Child();
			child.parent = parent;
			child.value = "original";
			parent.child = child;
			session.persist( parent );
		} );

		scope.inTransaction( session -> {
			Parent parent = session.find( Parent.class, new Key( 1L, 2L ) );
			assertNotNull( parent );
			Child child = parent.child;
			assertNotNull( child );
			assertEquals( 1L, child.id.id1 );
			assertEquals( 2L, child.id.id2 );
			assertSame( parent, child.parent );
			assertEquals( "original", child.value );
			child.value = "updated";
			session.flush();
		} );

		scope.inTransaction( session -> {
			Child child = session.find( Child.class, new Key( 1L, 2L ) );
			assertNotNull( child );
			assertEquals( key, child.id );
			assertEquals( "updated", child.value );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "CompositeMapsIdParent")
	@Table(name = "composite_mapsid_parent")
	static class Parent {
		@EmbeddedId
		private Key id;

		@OneToOne(mappedBy = "parent", cascade = CascadeType.ALL)
		private Child child;
	}

	@Entity(name = "CompositeMapsIdChild")
	@Table(name = "composite_mapsid_child")
	static class Child {
		@EmbeddedId
		private Key id;

		@OneToOne
		@MapsId
		@JoinColumn(name = "id1", referencedColumnName = "id1")
		@JoinColumn(name = "id2", referencedColumnName = "id2")
		private Parent parent;

		@Column(name = "child_value")
		private String value;
	}

	@Embeddable
	static class Key implements Serializable {
		@Column(name = "id1")
		private Long id1;

		@Column(name = "id2")
		private Long id2;

		public Key() {
		}

		Key(Long id1, Long id2) {
			this.id1 = id1;
			this.id2 = id2;
		}

		@Override
		public boolean equals(Object other) {
			return this == other || other instanceof Key key
					&& Objects.equals( id1, key.id1 ) && Objects.equals( id2, key.id2 );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id1, id2 );
		}
	}
}
