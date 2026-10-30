package org.hibernate.orm.test.collection.map;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.MapKeyFormula;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// A map of entities whose key is derived from a SQL formula.
/// Subclasses supply the mapping, through annotations or `orm.xml`.
public abstract class AbstractMapKeyFormulaTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = new Parent( 1L );
			session.persist( parent );
			session.persist( new Child( 1L, "abc", parent ) );
			session.persist( new Child( 2L, "xyz", parent ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testMapKeyFormula(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = session.find( Parent.class, 1L );
			assertThat( parent.children ).containsOnlyKeys( "ABC", "XYZ" );
			assertThat( parent.children.get( "ABC" ).code ).isEqualTo( "abc" );
			assertThat( parent.children.get( "XYZ" ).code ).isEqualTo( "xyz" );
		} );
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select key(c) from Parent p join p.children c order by key(c)", String.class
			).getResultList() ).containsExactly( "ABC", "XYZ" );
		} );
	}

	//tag::collections-map-key-formula-example[]
	@Entity(name = "Parent")
	@Table(name = "map_formula_parent")
	public static class Parent {
		@Id
		Long id;

		@OneToMany(mappedBy = "parent")
		@MapKeyFormula("upper(code)")
		Map<String, Child> children = new HashMap<>();

		//Constructors are omitted for brevity

	//end::collections-map-key-formula-example[]
		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
		}
	//tag::collections-map-key-formula-example[]
	}

	@Entity(name = "Child")
	@Table(name = "map_formula_child")
	public static class Child {
		@Id
		Long id;

		String code;

		@ManyToOne
		@JoinColumn(name = "parent_fk")
		Parent parent;

		//Constructors are omitted for brevity

	//end::collections-map-key-formula-example[]
		public Child() {
		}

		public Child(Long id, String code, Parent parent) {
			this.id = id;
			this.code = code;
			this.parent = parent;
		}
	//tag::collections-map-key-formula-example[]
	}
	//end::collections-map-key-formula-example[]
}
