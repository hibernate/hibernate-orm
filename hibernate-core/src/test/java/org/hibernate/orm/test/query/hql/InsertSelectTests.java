/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@ServiceRegistry
@DomainModel( annotatedClasses = {
		InsertSelectTests.EntityEntry.class
})
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class InsertSelectTests {

	@Test
	@TestForIssue( jiraKey = "HHH-15527")
	public void testInsertSelectGeneratedAssigned(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createMutationQuery("insert into EntityEntry (id, name) select 1, 'abc' from EntityEntry e").executeUpdate();
					statementInspector.assertExecutedCount( 1 );
				}
		);
	}

	@Entity(name = "EntityEntry")
	public static class EntityEntry {
		@Id
		@GeneratedValue
		Integer id;
		String name;
	}
}
