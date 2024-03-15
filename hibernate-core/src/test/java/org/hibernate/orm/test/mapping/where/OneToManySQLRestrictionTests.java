/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.where;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.AvailableHints;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { OneToManySQLRestrictionTests.Parent.class, OneToManySQLRestrictionTests.Child.class })
@SessionFactory(useCollectingStatementInspector = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-17854")
public class OneToManySQLRestrictionTests {

	@Test
	public void testLoad(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session.find( Parent.class, 1 );
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) )
					.isEqualTo(
							"select p1_0.id,cs1_0.parent_id,cs1_0.id,cs1_0.deleted_at " +
									"from Parent p1_0 " +
									"left join Child cs1_0 " +
									"on p1_0.id=cs1_0.parent_id " +
									"and (cs1_0.deleted_at IS NULL) " +
									"where p1_0.id=?"
					);
		} );
	}

	@Test
	public void testLoad2(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			session.createQuery( "from Parent p join fetch p.childSet" ).getResultList();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) )
					.isEqualTo(
							"select p1_0.id,cs1_0.parent_id,cs1_0.id,cs1_0.deleted_at " +
									"from Parent p1_0 " +
									"join Child cs1_0 " +
									"on p1_0.id=cs1_0.parent_id " +
									"and (cs1_0.deleted_at IS NULL)"
					);
		} );
	}

	@Test
	public void testLoad3(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			RootGraphImplementor<Parent> entityGraph = session.createEntityGraph( Parent.class );
			entityGraph.addAttributeNode( "childSet" );
			session.createQuery( "from Parent p" )
					.setHint( AvailableHints.HINT_SPEC_LOAD_GRAPH, entityGraph )
					.getResultList();
			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( statementInspector.getSqlQueries().get( 0 ) )
					.isEqualTo(
							"select p1_0.id,cs1_0.parent_id,cs1_0.id,cs1_0.deleted_at " +
									"from Parent p1_0 " +
									"left join Child cs1_0 " +
									"on p1_0.id=cs1_0.parent_id " +
									"and (cs1_0.deleted_at IS NULL)"
					);
		} );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;
		@SQLRestriction("deleted_at IS NULL")
		@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
		private Set<Child> childSet = new HashSet<>();
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;
		@ManyToOne
		private Parent parent;
		@Column(name = "deleted_at")
		private LocalDateTime deletedAt;
	}
}
