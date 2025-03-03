/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.queryhint;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Brett Meyer
 */
@RequiresDialect(value = OracleDialect.class)
@DomainModel(
		annotatedClasses = { OracleQueryHintTest.Employee.class, OracleQueryHintTest.Department.class }
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.USE_SQL_COMMENTS, value = "true")
)
public class OracleQueryHintTest {

	@BeforeAll
	protected void setUp(SessionFactoryScope scope) {
		Department department = new Department();
		department.name = "Sales";
		Employee employee1 = new Employee();
		employee1.department = department;
		Employee employee2 = new Employee();
		employee2.department = department;

		scope.inTransaction( s -> {
			s.persist( department );
			s.persist( employee1 );
			s.persist( employee2 );
		} );
	}

	@Test
	public void testQueryHint(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		// test Query w/ a simple Oracle optimizer hint
		scope.inTransaction( s -> {
			Query query = s.createQuery( "FROM Employee e WHERE e.department.name = :departmentName" )
					.addQueryHint( "ALL_ROWS" )
					.setParameter( "departmentName", "Sales" );
			List results = query.list();

			assertEquals( 2, results.size() );
		} );

		statementInspector.assertExecutedCount( 1 );
		assertTrue( statementInspector.getSqlQueries().get( 0 ).contains( "select /*+ ALL_ROWS */" ) );
		statementInspector.clear();

		// test multiple hints
		scope.inTransaction( s -> {
			Query query = s.createQuery( "FROM Employee e WHERE e.department.name = :departmentName" )
					.addQueryHint( "ALL_ROWS" )
					.addQueryHint( "USE_CONCAT" )
					.setParameter( "departmentName", "Sales" );
			List results = query.list();

			assertEquals( results.size(), 2 );
		} );

		statementInspector.assertExecutedCount( 1 );
		assertTrue( statementInspector.getSqlQueries().get( 0 ).contains( "select /*+ ALL_ROWS, USE_CONCAT */" ) );
		statementInspector.clear();

		// ensure the insertion logic can handle a comment appended to the front
		scope.inTransaction( s -> {
			Query query = s.createQuery( "FROM Employee e WHERE e.department.name = :departmentName" )
					.setComment( "this is a test" )
					.addQueryHint( "ALL_ROWS" )
					.setParameter( "departmentName", "Sales" );
			List results = query.list();

			assertEquals( results.size(), 2 );
		} );

		statementInspector.assertExecutedCount( 1 );

		assertTrue( statementInspector.getSqlQueries().get( 0 ).contains( "select /*+ ALL_ROWS */" ) );
		statementInspector.clear();

		// test Criteria
		scope.inTransaction( s -> {
			final CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Employee> criteria = criteriaBuilder.createQuery( Employee.class );
			Root<Employee> root = criteria.from( Employee.class );
			Join<Object, Object> departmentJoin = root.join( "department" );
			criteria.select( root ).where( criteriaBuilder.equal( departmentJoin.get( "name" ), "Sales" ) );
//			Criteria criteria = s.createCriteria( Employee.class )
//					.addQueryHint( "ALL_ROWS" )
//					.createCriteria( "department" ).add( Restrictions.eq( "name", "Sales" ) );
			Query<Employee> query = s.createQuery( criteria );
			query.addQueryHint( "ALL_ROWS" );
			List results = query.list();

			assertEquals( results.size(), 2 );
		} );

		statementInspector.assertExecutedCount( 1 );

		assertTrue( statementInspector.getSqlQueries().get( 0 ).contains( "select /*+ ALL_ROWS */" ) );
		statementInspector.clear();
	}

	@Test
	@JiraKey(value = "HHH-12362")
	public void testQueryHintAndComment(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( s -> {
			Query query = s.createQuery( "FROM Employee e WHERE e.department.name = :departmentName" )
					.addQueryHint( "ALL_ROWS" )
					.setComment( "My_Query" )
					.setParameter( "departmentName", "Sales" );
			List results = query.list();

			assertEquals( results.size(), 2 );
		} );

		statementInspector.assertExecutedCount( 1 );

		assertTrue( statementInspector.getSqlQueries()
							.get( 0 )
							.contains( "/* My_Query */ select /*+ ALL_ROWS */" ) );
		statementInspector.clear();
	}

	@Test
	@JiraKey(value = "HHH-13608")
	public void testQueryHintCaseInsensitive(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( s -> {
			List results = s.createNativeQuery(
							"SELECT e.id as id " +
									"FROM Employee e " +
									"JOIN Department d ON e.department_id = d.id " +
									"WHERE d.name = :departmentName" )
					.addQueryHint( "ALL_ROWS" )
					.setComment( "My_Query" )
					.setParameter( "departmentName", "Sales" )
					.getResultList();

			assertEquals( results.size(), 2 );
		} );

		statementInspector.assertExecutedCount( 1 );

		assertTrue( statementInspector.getSqlQueries()
							.get( 0 )
							.contains( "/* My_Query */ SELECT /*+ ALL_ROWS */" ) );
		statementInspector.clear();
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		@GeneratedValue
		public long id;

		@ManyToOne(fetch = FetchType.LAZY)
		public Department department;
	}

	@Entity(name = "Department")
	public static class Department {
		@Id
		@GeneratedValue
		public long id;

		public String name;
	}
}
