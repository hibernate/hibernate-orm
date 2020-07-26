/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.queryhint;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Criteria;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Jonathan Bregler
 */
@RequiresDialect(AbstractHANADialect.class)
public class QueryHintHANATest extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.USE_SQL_COMMENTS, "true" );
		sqlStatementInterceptor = new SQLStatementInterceptor( settings );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Employee.class, Department.class };
	}

	@Override
	protected void afterSessionFactoryBuilt(SessionFactoryImplementor sessionFactory) {
		Department department = new Department();
		department.name = "Sales";
		Employee employee1 = new Employee();
		employee1.department = department;
		Employee employee2 = new Employee();
		employee2.department = department;

		doInHibernate( this::sessionFactory, s -> {
			s.persist( department );
			s.persist( employee1 );
			s.persist( employee2 );
		} );
	}

	@Test
	public void testQueryHint() {

		sqlStatementInterceptor.clear();

		doInHibernate( this::sessionFactory, s -> {
			Query<Employee> query = s.createQuery( "FROM QueryHintHANATest$Employee e WHERE e.department.name = :departmentName", Employee.class )
					.addQueryHint( "NO_CS_JOIN" )
					.setParameter( "departmentName", "Sales" );
			List<Employee> results = query.list();

			assertEquals( results.size(), 2 );
		} );

		sqlStatementInterceptor.assertExecutedCount( 1 );
		assertThat( sqlStatementInterceptor.getSqlQueries().get( 0 ), containsString( " with hint (NO_CS_JOIN)" ) );
		sqlStatementInterceptor.clear();

		// test multiple hints
		doInHibernate( this::sessionFactory, s -> {
			Query<Employee> query = s.createQuery( "FROM QueryHintHANATest$Employee e WHERE e.department.name = :departmentName", Employee.class )
					.addQueryHint( "NO_CS_JOIN" )
					.addQueryHint( "IGNORE_PLAN_CACHE" )
					.setParameter( "departmentName", "Sales" );
			List<Employee> results = query.list();

			assertEquals( results.size(), 2 );
		} );

		sqlStatementInterceptor.assertExecutedCount( 1 );

		assertThat( sqlStatementInterceptor.getSqlQueries().get( 0 ), containsString( " with hint (NO_CS_JOIN,IGNORE_PLAN_CACHE)" ) );
		sqlStatementInterceptor.clear();

		// ensure the insertion logic can handle a comment appended to the front
		doInHibernate( this::sessionFactory, s -> {
			Query<Employee> query = s.createQuery( "FROM QueryHintHANATest$Employee e WHERE e.department.name = :departmentName", Employee.class )
					.setComment( "this is a test" )
					.addQueryHint( "NO_CS_JOIN" )
					.setParameter( "departmentName", "Sales" );
			List<Employee> results = query.list();

			assertEquals( results.size(), 2 );
		} );

		sqlStatementInterceptor.assertExecutedCount( 1 );

		assertThat( sqlStatementInterceptor.getSqlQueries().get( 0 ), containsString( " with hint (NO_CS_JOIN)" ) );
		sqlStatementInterceptor.clear();

		// test Criteria
		doInHibernate( this::sessionFactory, s -> {
			Criteria criteria = s.createCriteria( Employee.class )
					.addQueryHint( "NO_CS_JOIN" )
					.createCriteria( "department" ).add( Restrictions.eq( "name", "Sales" ) );
			List<?> results = criteria.list();

			assertEquals( results.size(), 2 );
		} );

		sqlStatementInterceptor.assertExecutedCount( 1 );

		assertThat( sqlStatementInterceptor.getSqlQueries().get( 0 ), containsString( " with hint (NO_CS_JOIN)" ) );
		sqlStatementInterceptor.clear();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12362")
	public void testQueryHintAndComment() {
		sqlStatementInterceptor.clear();

		doInHibernate( this::sessionFactory, s -> {
			Query<Employee> query = s.createQuery( "FROM QueryHintHANATest$Employee e WHERE e.department.name = :departmentName", Employee.class )
					.addQueryHint( "NO_CS_JOIN" )
					.setComment( "My_Query" )
					.setParameter( "departmentName", "Sales" );
			List<Employee> results = query.list();

			assertEquals( results.size(), 2 );
		} );

		sqlStatementInterceptor.assertExecutedCount( 1 );

		assertThat( sqlStatementInterceptor.getSqlQueries().get( 0 ), containsString( " with hint (NO_CS_JOIN)" ) );
		assertThat( sqlStatementInterceptor.getSqlQueries().get( 0 ), containsString( "/* My_Query */ select" ) );
		sqlStatementInterceptor.clear();
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
