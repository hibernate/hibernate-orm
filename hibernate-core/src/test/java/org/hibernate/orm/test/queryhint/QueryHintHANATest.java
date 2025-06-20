/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.queryhint;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.dialect.HANADialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.cfg.AvailableSettings.USE_SQL_COMMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jonathan Bregler
 */
@ServiceRegistry(
		settings = @Setting( name = USE_SQL_COMMENTS, value = "true" )
)
@DomainModel(
		annotatedClasses = { QueryHintHANATest.Employee.class, QueryHintHANATest.Department.class }
)
@SessionFactory( useCollectingStatementInspector = true )
@RequiresDialect(HANADialect.class)
public class QueryHintHANATest {

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (s) -> {
			Department department = new Department();
			department.name = "Sales";
			Employee employee1 = new Employee();
			employee1.department = department;
			Employee employee2 = new Employee();
			employee2.department = department;
			s.persist( department );
			s.persist( employee1 );
			s.persist( employee2 );
		} );
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testQueryHint(SessionFactoryScope scope) {
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction( (s) -> {
			Query<Employee> query = s.createQuery( "FROM QueryHintHANATest$Employee e WHERE e.department.name = :departmentName", Employee.class )
					.addQueryHint( "NO_CS_JOIN" )
					.setParameter( "departmentName", "Sales" );
			List<Employee> results = query.list();

			assertEquals( results.size(), 2 );
		} );

		scope.getCollectingStatementInspector().assertExecutedCount( 1 );
		assertThat( scope.getCollectingStatementInspector().getSqlQueries().get( 0 ), containsString( " with hint (NO_CS_JOIN)" ) );
		scope.getCollectingStatementInspector().clear();

		// test multiple hints
		scope.inTransaction( (s) -> {
			Query<Employee> query = s.createQuery( "FROM QueryHintHANATest$Employee e WHERE e.department.name = :departmentName", Employee.class )
					.addQueryHint( "NO_CS_JOIN" )
					.addQueryHint( "IGNORE_PLAN_CACHE" )
					.setParameter( "departmentName", "Sales" );
			List<Employee> results = query.list();

			assertEquals( results.size(), 2 );
		} );

		scope.getCollectingStatementInspector().assertExecutedCount( 1 );
		assertThat( scope.getCollectingStatementInspector().getSqlQueries().get( 0 ), containsString( " with hint (NO_CS_JOIN,IGNORE_PLAN_CACHE)" ) );
		scope.getCollectingStatementInspector().clear();

		// ensure the insertion logic can handle a comment appended to the front
		scope.inTransaction( (s) -> {
			Query<Employee> query = s.createQuery( "FROM QueryHintHANATest$Employee e WHERE e.department.name = :departmentName", Employee.class )
					.setComment( "this is a test" )
					.addQueryHint( "NO_CS_JOIN" )
					.setParameter( "departmentName", "Sales" );
			List<Employee> results = query.list();

			assertEquals( results.size(), 2 );
		} );

		scope.getCollectingStatementInspector().assertExecutedCount( 1 );
		assertThat( scope.getCollectingStatementInspector().getSqlQueries().get( 0 ), containsString( " with hint (NO_CS_JOIN)" ) );
		scope.getCollectingStatementInspector().clear();

		// test Criteria
		scope.inTransaction( (s) -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Employee> criteria = criteriaBuilder.createQuery( Employee.class );
			Root<Employee> root = criteria.from( Employee.class );
			Join<Employee, Department> department = root.join( "department", JoinType.INNER );
			criteria.select( root ).where( criteriaBuilder.equal( department.<String>get("name"), "Sales" ) );

			Query<Employee> query = s.createQuery( criteria );
			query.addQueryHint(  "NO_CS_JOIN" );

			List<Employee> results = query.list();

//			Criteria criteria = s.createCriteria( Employee.class )
//					.addQueryHint( "NO_CS_JOIN" )
//					.createCriteria( "department" ).add( Restrictions.eq( "name", "Sales" ) );
//			List<?> results = criteria.list();

			assertEquals( results.size(), 2 );
		} );

		scope.getCollectingStatementInspector().assertExecutedCount( 1 );
		assertThat( scope.getCollectingStatementInspector().getSqlQueries().get( 0 ), containsString( " with hint (NO_CS_JOIN)" ) );
		scope.getCollectingStatementInspector().clear();
	}

	@Test
	@JiraKey( "HHH-12362")
	public void testQueryHintAndComment(SessionFactoryScope scope) {
		scope.getCollectingStatementInspector().clear();

		scope.inTransaction( (s) -> {
			Query<Employee> query = s.createQuery( "FROM QueryHintHANATest$Employee e WHERE e.department.name = :departmentName", Employee.class )
					.addQueryHint( "NO_CS_JOIN" )
					.setComment( "My_Query" )
					.setParameter( "departmentName", "Sales" );
			List<Employee> results = query.list();

			assertEquals( results.size(), 2 );
		} );

		scope.getCollectingStatementInspector().assertExecutedCount( 1 );
		assertThat( scope.getCollectingStatementInspector().getSqlQueries().get( 0 ), containsString( " with hint (NO_CS_JOIN)" ) );
		assertThat( scope.getCollectingStatementInspector().getSqlQueries().get( 0 ), containsString( "/* My_Query */ select" ) );
		scope.getCollectingStatementInspector().clear();
	}

	@Entity
	public static class Employee {

		@Id
		@GeneratedValue
		public long id;

		@ManyToOne(fetch = FetchType.LAZY)
		public Department department;
	}

	@Entity
	public static class Department {

		@Id
		@GeneratedValue
		public long id;

		public String name;
	}
}
