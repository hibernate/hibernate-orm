
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.queryhint;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.SQLServer2012Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Brett Meyer
 */

@RequiresDialect(SQLServer2012Dialect.class)
public class QueryHintSQLServer2012Test extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Employee.class, Department.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.DIALECT, QueryHintTestSQLServer2012Dialect.class.getName() );
		configuration.setProperty( AvailableSettings.USE_SQL_COMMENTS, "true" );
	}

	@Test
	public void testQueryHint() {
		Department department = new Department();
		department.name = "Sales";
		Employee employee1 = new Employee();
		employee1.department = department;
		Employee employee2 = new Employee();
		employee2.department = department;

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( department );
		s.persist( employee1 );
		s.persist( employee2 );
		s.getTransaction().commit();
		s.clear();

		// test Query w/ a simple SQLServer2012 optimizer hint
		s.getTransaction().begin();
		Query query = s.createQuery( "FROM QueryHintSQLServer2012Test$Employee e WHERE e.department.name = :departmentName" ).addQueryHint( "MAXDOP 2" )
				.setParameter( "departmentName", "Sales" );
		List results = query.list();
		s.getTransaction().commit();
		s.clear();

		assertEquals( results.size(), 2 );
		assertTrue( QueryHintTestSQLServer2012Dialect.getProcessedSql().contains( "OPTION (MAXDOP 2)" ) );

		QueryHintTestSQLServer2012Dialect.resetProcessedSql();

		// test multiple hints
		s.getTransaction().begin();
		query = s.createQuery( "FROM QueryHintSQLServer2012Test$Employee e WHERE e.department.name = :departmentName" )
                .addQueryHint("MAXDOP 2")
				.addQueryHint("CONCAT UNION")
                .setParameter("departmentName", "Sales");
		results = query.list();
		s.getTransaction().commit();
		s.clear();

		assertEquals( results.size(), 2 );
		assertTrue( QueryHintTestSQLServer2012Dialect.getProcessedSql().contains( "MAXDOP 2" ) );
		assertTrue( QueryHintTestSQLServer2012Dialect.getProcessedSql().contains( "CONCAT UNION" ) );

		QueryHintTestSQLServer2012Dialect.resetProcessedSql();

		// ensure the insertion logic can handle a comment appended to the front
		s.getTransaction().begin();
		query = s.createQuery( "FROM QueryHintSQLServer2012Test$Employee e WHERE e.department.name = :departmentName" ).setComment( "this is a test" )
				.addQueryHint( "MAXDOP 2" )
                .setParameter( "departmentName", "Sales" );
		results = query.list();
		s.getTransaction().commit();
		s.clear();

		assertEquals( results.size(), 2 );
		assertTrue( QueryHintTestSQLServer2012Dialect.getProcessedSql().contains( "OPTION (MAXDOP 2)" ) );

		QueryHintTestSQLServer2012Dialect.resetProcessedSql();

		// test Criteria
		s.getTransaction().begin();
		Criteria criteria = s.createCriteria( Employee.class ).addQueryHint( "MAXDOP 2" ).createCriteria( "department" )
				.add( Restrictions.eq( "name", "Sales" ) );
		results = criteria.list();
		s.getTransaction().commit();
		s.close();

		assertEquals( results.size(), 2 );
		assertTrue( QueryHintTestSQLServer2012Dialect.getProcessedSql().contains( "OPTION (MAXDOP 2)" ) );
	}

	/**
	 * Since the query hint is added to the SQL during Loader's executeQueryStatement -> preprocessSQL, rather than
	 * early on during the QueryTranslator or QueryLoader initialization, there's not an easy way to check the full SQL
	 * after completely processing it. Instead, use this ridiculous hack to ensure Loader actually calls Dialect. TODO:
	 * This is terrible. Better ideas?
	 */
	public static class QueryHintTestSQLServer2012Dialect extends SQLServer2012Dialect {

		private static String processedSql;

		@Override
		public String getQueryHintString(String sql, List<String> hints) {
			processedSql = super.getQueryHintString( sql, hints );
			return processedSql;
		}

		public static String getProcessedSql() {
			return processedSql;
		}

		public static void resetProcessedSql() {
			processedSql = "";
		}
	}

	@Entity
	public static class Employee {

		@Id
		@GeneratedValue
		public long id;

		@ManyToOne
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
