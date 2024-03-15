/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ql;

import java.util.Set;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;

@TestForIssue(jiraKey = "HHH-16691")
@DomainModel(
		annotatedClasses = {
				JoinTableOptimizationTest.Document.class, JoinTableOptimizationTest.Person.class
		})
@SessionFactory(useCollectingStatementInspector = true)
public class JoinTableOptimizationTest {

	@Test
	public void testOnlyCollectionTableJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.id from Document d left join d.people p where p.id is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					// Assert only the collection table is joined
					statementInspector.assertNumberOfJoins( 0, 1 );
				}
		);
	}

	@Test
	public void testInnerJoin(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.name from Document d join d.people p" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_1.name " +
									"from Document d1_0 " +
									"join people p1_0 on d1_0.id=p1_0.Document_id " +
									"join Person p1_1 on p1_1.id=p1_0.people_id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	public void testLeftJoin(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.name from Document d left join d.people p" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_1.name " +
									"from Document d1_0 " +
									"left join people p1_0 on d1_0.id=p1_0.Document_id " +
									"left join Person p1_1 on p1_1.id=p1_0.people_id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	public void testInnerJoinCustomOnClause(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.name from Document d join d.people p on p.id > 1" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_1.name " +
									"from Document d1_0 " +
									"join people p1_0 on d1_0.id=p1_0.Document_id and p1_0.people_id>1 " +
									"join Person p1_1 on p1_1.id=p1_0.people_id",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was not optimized away"
					);
				}
		);
	}

	@Test
	public void testLeftJoinCustomOnClause(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.name from Document d left join d.people p on p.id > 1" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_1.name " +
									"from Document d1_0 " +
									"left join (people p1_0 " +
									"join Person p1_1 on p1_1.id=p1_0.people_id) on d1_0.id=p1_0.Document_id and p1_0.people_id>1",
							statementInspector.getSqlQueries().get( 0 ),
							"Nested join was wrongly optimized away"
					);
				}
		);
	}

	@Test
	@JiraKey("HHH-17830")
	public void testElementCollectionJoinCustomOnClause(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select p.text from Document d join d.pages p on p.text is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					Assertions.assertEquals(
							"select p1_0.text " +
									"from Document d1_0 " +
									"join document_pages p1_0 on d1_0.id=p1_0.Document_id and p1_0.text is not null",
							statementInspector.getSqlQueries().get( 0 ),
							"Join condition was wrongly removed"
					);
				}
		);
	}

	@Entity(name = "Document")
	public static class Document {
		@Id
		Long id;
		String name;
		@OneToMany
		@JoinTable(name = "people")
		Set<Person> people;
		@ElementCollection
		@CollectionTable(name = "document_pages")
		Set<Page> pages;
	}
	@Entity(name = "Person")
	public static class Person {
		@Id
		Long id;
		String name;
	}
	@Embeddable
	public static class Page {
		String text;
	}
}
