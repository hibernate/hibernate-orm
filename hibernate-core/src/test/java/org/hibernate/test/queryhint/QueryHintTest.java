/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.queryhint;

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
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.Query;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Brett Meyer
 */
@RequiresDialect( Oracle8iDialect.class )
public class QueryHintTest extends BaseNonConfigCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider;

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.USE_SQL_COMMENTS, "true" );
		settings.put(
				org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@Override
	protected void buildResources() {
		connectionProvider = new PreparedStatementSpyConnectionProvider();
		super.buildResources();
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Employee.class, Department.class };
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

		connectionProvider.clear();

		// test Query w/ a simple Oracle optimizer hint
		doInHibernate( this::sessionFactory, s -> {
			Query query = s.createQuery( "FROM QueryHintTest$Employee e WHERE e.department.name = :departmentName" )
					.addQueryHint( "ALL_ROWS" )
					.setParameter( "departmentName", "Sales" );
			List results = query.list();

			assertEquals(results.size(), 2);
		} );

		assertEquals(
			1,
			connectionProvider.getPreparedStatements().size()
		);
		assertNotNull( connectionProvider.getPreparedSQLStatements().get( 0 ).contains( "select /*+ ALL_ROWS */" ) );
		connectionProvider.clear();

		// test multiple hints
		doInHibernate( this::sessionFactory, s -> {
			Query query = s.createQuery( "FROM QueryHintTest$Employee e WHERE e.department.name = :departmentName" )
					.addQueryHint( "ALL_ROWS" )
					.addQueryHint( "USE_CONCAT" )
					.setParameter( "departmentName", "Sales" );
			List results = query.list();

			assertEquals(results.size(), 2);
		} );

		assertEquals(
				1,
				connectionProvider.getPreparedStatements().size()
		);
		assertNotNull( connectionProvider.getPreparedSQLStatements().get( 0 ).contains( "select /*+ ALL_ROWS, USE_CONCAT */" ) );
		connectionProvider.clear();
		
		// ensure the insertion logic can handle a comment appended to the front
		doInHibernate( this::sessionFactory, s -> {
			Query query = s.createQuery( "FROM QueryHintTest$Employee e WHERE e.department.name = :departmentName" )
					.setComment( "this is a test" )
					.addQueryHint( "ALL_ROWS" )
					.setParameter( "departmentName", "Sales" );
			List results = query.list();

			assertEquals(results.size(), 2);
		} );

		assertEquals(
				1,
				connectionProvider.getPreparedStatements().size()
		);
		assertNotNull( connectionProvider.getPreparedSQLStatements().get( 0 ).contains( "select /*+ ALL_ROWS */" ) );
		connectionProvider.clear();

		// test Criteria
		doInHibernate( this::sessionFactory, s -> {
			Criteria criteria = s.createCriteria( Employee.class )
					.addQueryHint( "ALL_ROWS" )
					.createCriteria( "department" ).add( Restrictions.eq( "name", "Sales" ) );
			List results = criteria.list();

			assertEquals(results.size(), 2);
		} );

		assertEquals(
				1,
				connectionProvider.getPreparedStatements().size()
		);
		assertNotNull( connectionProvider.getPreparedSQLStatements().get( 0 ).contains( "select /*+ ALL_ROWS */" ) );
		connectionProvider.clear();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12362" )
	public void testQueryHintAndComment() {
		connectionProvider.clear();

		doInHibernate( this::sessionFactory, s -> {
			Query query = s.createQuery( "FROM QueryHintTest$Employee e WHERE e.department.name = :departmentName" )
					.addQueryHint( "ALL_ROWS" )
					.setComment( "My_Query" )
					.setParameter( "departmentName", "Sales" );
			List results = query.list();

			assertEquals(results.size(), 2);
		} );

		assertEquals(
				1,
				connectionProvider.getPreparedStatements().size()
		);
		assertNotNull( connectionProvider.getPreparedSQLStatements().get( 0 ).contains( "/* My_Query */ select /*+ ALL_ROWS */" ) );
		connectionProvider.clear();
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
