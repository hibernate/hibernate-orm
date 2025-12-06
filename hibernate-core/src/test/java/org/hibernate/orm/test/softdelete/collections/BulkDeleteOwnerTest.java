/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
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
			assertThat( sqlInspector.getSqlQueries().get( 0 ) ).doesNotContain( "delete from" );
			assertThat( sqlInspector.getSqlQueries().get( 0 ) ).contains( "update employee_accolades" );
			assertThat( sqlInspector.getSqlQueries().get( 0 ) ).contains( "set deleted_on=localtimestamp" );
			assertThat( sqlInspector.getSqlQueries().get( 0 ) ).contains( ".employee_fk in (select" );
		} );
	}

	@Entity(name = "Employee")
	@Table(name = "employees")
	@SoftDelete( strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at" )
	public static class Employee {
		@Id
		long id;
		@ElementCollection
		@CollectionTable( name = "employee_accolades", joinColumns = @JoinColumn( name = "employee_fk" ) )
		@SoftDelete( strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_on" )
		private List<String> accolades;
	}
}
