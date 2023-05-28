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
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

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

	@Entity(name = "Document")
	public static class Document {
		@Id
		Long id;
		String name;
		@OneToMany
		@JoinTable(name = "people")
		Set<Person> people;
	}
	@Entity(name = "Person")
	public static class Person {
		@Id
		Long id;
		String name;
	}
}
