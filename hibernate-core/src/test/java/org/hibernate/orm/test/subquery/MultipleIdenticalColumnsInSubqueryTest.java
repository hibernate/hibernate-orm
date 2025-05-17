/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subquery;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = MultipleIdenticalColumnsInSubqueryTest.Something.class)
@SessionFactory
@JiraKey("HHH-19396")
class MultipleIdenticalColumnsInSubqueryTest {

	@BeforeEach
	void init(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Something() ) );
	}

	@AfterEach
	void clean(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Something" ).executeUpdate() );
	}

	@Test
	void CTE_with_same_column_selected_twice(SessionFactoryScope scope) {
		var r = scope.fromSession( session ->
				session.createSelectionQuery(
						"WITH S0 AS (SELECT foo AS foo2, foo AS foo FROM Something)" +
						"SELECT S0_0.foo AS foo FROM S0 AS S0_0",
						String.class ).getSingleResult() );
		assertEquals( "a", r );
	}

	@Test
	void CTE_with_same_column_selected_twice_some_aliases_removed(SessionFactoryScope scope) {
		var r = scope.fromSession( session ->
				session.createSelectionQuery(
						"WITH S0 AS (SELECT foo AS foo, foo AS foo2 FROM Something)" +
						"SELECT foo AS foo FROM S0",
						String.class ).getSingleResult() );
		assertEquals( "a", r );
	}

	@Test
	void simple_query_with_same_column_selected_twice(SessionFactoryScope scope) {
		var tuple = scope.fromSession( session ->
				session.createSelectionQuery(
						"SELECT foo AS foo, foo as foo2 FROM Something",
						Tuple.class ).getSingleResult() );
		assertThat( tuple.getElements().stream().map( TupleElement::getAlias ).collect( toSet() ) )
				.containsExactlyInAnyOrder( "foo", "foo2" );
	}

	@Entity(name = "Something")
	static class Something {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String foo = "a";
	}
}
