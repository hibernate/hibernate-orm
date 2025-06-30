/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.orm.test.subquery;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
	@DisplayName("Temporary table with same column selected twice, deduplication should be turned off")
	void CTE_with_same_column_selected_twice(SessionFactoryScope scope) {
		var r = scope.fromSession( session ->
				session.createSelectionQuery(
						"WITH S0 AS (SELECT foo AS foo, foo AS bar FROM Something) SELECT foo AS foo FROM S0",
						String.class ).getSingleResult() );
		assertEquals( "a", r );
	}

	@Test
	@DisplayName("Subquery with same column selected twice, deduplication should be turned off")
	void CTE_with_same_column_selected_twice_some_aliases_removed(SessionFactoryScope scope) {
		var r = scope.fromSession( session ->
				session.createSelectionQuery(
						"SELECT foo AS foo FROM (SELECT foo AS foo, foo AS foo2 FROM Something)",
						String.class ).getSingleResult() );
		assertEquals( "a", r );
	}

	@Test
	@DisplayName("Simple query with same column selected twice, deduplication should be turned on")
	void simple_query_with_same_column_selected_twice(SessionFactoryScope scope) {
		var tuple = scope.fromSession( session ->
				session.createSelectionQuery(
						"SELECT foo AS foo, foo as bar FROM Something",
						Tuple.class ).getSingleResult() );
		assertEquals( 2, tuple.getElements().size() );
		assertEquals( "a", tuple.get( "foo" ) );
		assertEquals( "a", tuple.get( "bar" ) );
	}

	@Entity(name = "Something")
	static class Something {
		@Id
		@GeneratedValue
		private Long id;
		private String foo = "a";
	}
}
