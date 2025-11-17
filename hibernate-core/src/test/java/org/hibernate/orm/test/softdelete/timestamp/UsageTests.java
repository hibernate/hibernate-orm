/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.timestamp;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = Employee.class)
@SessionFactory(useCollectingStatementInspector = true)
public class UsageTests {
	@Test
	void testUsage(SessionFactoryScope sessions) {
		final SQLStatementInspector sqlCollector = sessions.getCollectingStatementInspector();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create some rows
		sqlCollector.clear();
		sessions.inTransaction( (session) -> {
			final Employee john = new Employee( 1, "John", null );
			final Employee steve = new Employee( 2, "Steve", john );
			final Employee ralph = new Employee( 3, "Ralph", steve );

			session.persist( john );
			session.persist( steve );
			session.persist( ralph );
		} );
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( "values (?,?,null,?)" );
		assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( "values (?,?,null,?)" );
		assertThat( sqlCollector.getSqlQueries().get( 2 ) ).contains( "values (?,?,null,?)" );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Remove the "middle" employee
		sqlCollector.clear();
		sessions.inTransaction( (session) -> {
			session.remove( session.find( Employee.class, 2 ) );
		} );
		// the SELECT + the 2 UPDATES
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
		assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( "update employee_accolades " );
		assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( "set deleted_on=" );
		assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( "and deleted_on is null" );
		assertThat( sqlCollector.getSqlQueries().get( 2 ) ).contains( "update employees " );
		assertThat( sqlCollector.getSqlQueries().get( 2 ) ).contains( "set deleted_at=" );
		assertThat( sqlCollector.getSqlQueries().get( 2 ) ).contains( "deleted_at is null" );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Make sure the "middle" employee is removed (load)
		sqlCollector.clear();
		sessions.inTransaction( (session) -> {
			final Employee steve = session.find( Employee.class, 2 );
			assertThat( steve ).isNull();
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Make sure the "middle" employee is removed (HQL)
		sqlCollector.clear();
		sessions.inTransaction( (session) -> {
			final Employee ralph = session.createQuery(
					"from Employee e where e.id = 3",
					Employee.class
			).getSingleResultOrNull();

			assertThat( ralph ).isNotNull();
			assertThat( ralph.getManager() ).isNull();

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Make sure the "middle" employee is removed (HQL w/ join)
		sqlCollector.clear();
		sessions.inTransaction( (session) -> {
			final Employee ralph = session.createQuery(
					"from Employee e left join fetch e.manager where e.id = 3",
					Employee.class
			).getSingleResultOrNull();

			assertThat( ralph ).isNotNull();
			assertThat( ralph.getManager() ).isNull();

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// delete Employee by HQL
		// 		NOTE : this one does not work as it "should" - https://hibernate.atlassian.net/browse/HHH-19192
//		sqlCollector.clear();
//		sessions.inTransaction( (session) -> {
//			final int affected = session.createMutationQuery( "delete Employee where id = 1" ).executeUpdate();
//			assertThat( affected ).isEqualTo( 1 );
//
//			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
//			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( "update employee_accolades " );
//			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( "set deleted_on=" );
//			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( "and deleted_on is null" );
//			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( "update employees " );
//			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( "set deleted_at=" );
//			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( "deleted_at is null" );
//		} );

	}

	@AfterEach
	void dropTestData(SessionFactoryScope sessions) {
		sessions.dropData();
	}
}
