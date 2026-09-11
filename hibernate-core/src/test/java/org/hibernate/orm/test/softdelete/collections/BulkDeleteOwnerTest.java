/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.softdelete.collections;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.YesNoConverter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel( annotatedClasses = { BulkDeleteOwnerTest.Employee.class } )
@Jira( "https://hibernate.atlassian.net/browse/HHH-19192" )
public class BulkDeleteOwnerTest {
	@Test void test(SessionFactoryScope scope) {
		final SQLStatementInspector sqlInspector = scope.getCollectingStatementInspector();
		sqlInspector.clear();
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete Employee where id = 1" ).executeUpdate();
			assertThat( sqlInspector.getSqlQueries() ).hasSize( 2 );
			assertThat( sqlInspector.getSqlQueries().get( 0 ) ).containsAnyOf( "deleted='Y'", "deleted=N'Y'" );
		} );
	}

	@Entity(name = "Employee")
	@SoftDelete
	public static class Employee {
		@Id
		long id;
		@ElementCollection
		@SoftDelete( columnName = "deleted", converter = YesNoConverter.class )
		private List<String> accolades;
	}
}
