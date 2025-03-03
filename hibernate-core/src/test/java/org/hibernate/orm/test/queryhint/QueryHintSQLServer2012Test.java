/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.queryhint;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Brett Meyer
 */

@RequiresDialect(value = SQLServerDialect.class, majorVersion = 11)
@DomainModel(
		annotatedClasses = { QueryHintSQLServer2012Test.Employee.class, QueryHintSQLServer2012Test.Department.class }
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.USE_SQL_COMMENTS, value = "true"),
		settingProviders = @SettingProvider(provider = QueryHintSQLServer2012Test.DialectProvider.class, settingName = AvailableSettings.DIALECT)
)
public class QueryHintSQLServer2012Test {

	public static class DialectProvider implements SettingProvider.Provider<String> {

		@Override
		public String getSetting() {
			return QueryHintTestSQLServer2012Dialect.class.getName();
		}
	}

	@Test
	public void testQueryHint(SessionFactoryScope scope) {
		Department department = new Department();
		department.name = "Sales";
		Employee employee1 = new Employee();
		employee1.department = department;
		Employee employee2 = new Employee();
		employee2.department = department;

		List result = scope.fromSession(
				session -> {
					session.getTransaction().begin();
					session.persist( department );
					session.persist( employee1 );
					session.persist( employee2 );

					try {
						session.getTransaction().commit();
						session.clear();

						// test Query w/ a simple SQLServer2012 optimizer hint
						session.getTransaction().begin();
						Query query = session.createQuery(
										"FROM QueryHintSQLServer2012Test$Employee e WHERE e.department.name = :departmentName" )
								.addQueryHint(
										"MAXDOP 2" )
								.setParameter( "departmentName", "Sales" );
						List results = query.list();
						session.getTransaction().commit();
						session.clear();

						assertEquals( 2, results.size() );
						assertTrue( QueryHintTestSQLServer2012Dialect.getProcessedSql()
											.contains( "OPTION (MAXDOP 2)" ) );

						QueryHintTestSQLServer2012Dialect.resetProcessedSql();

						// test multiple hints
						session.getTransaction().begin();
						query = session.createQuery(
										"FROM QueryHintSQLServer2012Test$Employee e WHERE e.department.name = :departmentName" )
								.addQueryHint( "MAXDOP 2" )
								.addQueryHint( "CONCAT UNION" )
								.setParameter( "departmentName", "Sales" );
						results = query.list();
						session.getTransaction().commit();
						session.clear();

						assertEquals( results.size(), 2 );
						assertTrue( QueryHintTestSQLServer2012Dialect.getProcessedSql().contains( "MAXDOP 2" ) );
						assertTrue( QueryHintTestSQLServer2012Dialect.getProcessedSql().contains( "CONCAT UNION" ) );

						QueryHintTestSQLServer2012Dialect.resetProcessedSql();

						// ensure the insertion logic can handle a comment appended to the front
						session.getTransaction().begin();
						query = session.createQuery(
										"FROM QueryHintSQLServer2012Test$Employee e WHERE e.department.name = :departmentName" )
								.setComment( "this is a test" )
								.addQueryHint( "MAXDOP 2" )
								.setParameter( "departmentName", "Sales" );
						results = query.list();
						session.getTransaction().commit();
						session.clear();

						assertEquals( results.size(), 2 );
						assertTrue( QueryHintTestSQLServer2012Dialect.getProcessedSql()
											.contains( "OPTION (MAXDOP 2)" ) );

						QueryHintTestSQLServer2012Dialect.resetProcessedSql();

						// test Criteria
						session.getTransaction().begin();

						CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
						CriteriaQuery<Employee> criteria = criteriaBuilder.createQuery( Employee.class );
						Root<Employee> root = criteria.from( Employee.class );
						Join<Object, Object> departement = root.join( "department", JoinType.INNER );
						criteria.select( root ).where( criteriaBuilder.equal( departement.get( "name" ), "Sales" ) );
//		Criteria criteria = s.createCriteria( Employee.class ).addQueryHint( "MAXDOP 2" ).createCriteria( "department" )
//				.add( Restrictions.eq( "name", "Sales" ) );
//		results = criteria.list();
						Query<Employee> criteriaQuery = session.createQuery( criteria );
						criteriaQuery.addQueryHint( "MAXDOP 2" );
						results = criteriaQuery.list();
						session.getTransaction().commit();
						return results;
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);

		assertEquals( result.size(), 2 );
		assertTrue( QueryHintTestSQLServer2012Dialect.getProcessedSql().contains( "OPTION (MAXDOP 2)" ) );
	}

	/**
	 * Since the query hint is added to the SQL during Loader's executeQueryStatement -> preprocessSQL, rather than
	 * early on during the QueryTranslator or QueryLoader initialization, there's not an easy way to check the full SQL
	 * after completely processing it. Instead, use this ridiculous hack to ensure Loader actually calls Dialect. TODO:
	 * This is terrible. Better ideas?
	 */
	public static class QueryHintTestSQLServer2012Dialect extends SQLServerDialect {

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
