/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceException;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DB297Dialect;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * DB2 has 2 functions for getting a substring: "substr" and "substring"
 *
 * @author Gail Badner
 */
@RequiresDialect(DB297Dialect.class)
public class DB297SubStringFunctionsTest extends BaseCoreFunctionalTestCase {
	private static final MostRecentStatementInspector mostRecentStatementInspector = new MostRecentStatementInspector();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.STATEMENT_INSPECTOR, mostRecentStatementInspector );
	}

	@Before
	public void setup() {
		AnEntity anEntity = new AnEntity();
		anEntity.description = "A very long, boring description.";

		doInHibernate(
				this::sessionFactory, session -> {
					session.persist( anEntity );
				}
		);
	}

	@After
	public void cleanup() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.createQuery( "delete from AnEntity" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11957")
	public void testSubstringWithStringUnits() {

		mostRecentStatementInspector.clear();

		doInHibernate(
				this::sessionFactory, session -> {
					String value = session.createQuery(
							"select substring( e.description, 21, 11, octets ) from AnEntity e",
							String.class
					).uniqueResult();
					assertEquals( "description", value );
				}
		);

		assertTrue( mostRecentStatementInspector.mostRecentSql.contains( "substring(" ) );
		assertTrue( mostRecentStatementInspector.mostRecentSql.contains( "octets" ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11957")
	public void testSubstringWithoutStringUnits() {

		mostRecentStatementInspector.clear();

		doInHibernate(
				this::sessionFactory, session -> {
					String value = session.createQuery(
							"select substring( e.description, 21, 11 ) from AnEntity e",
							String.class
					).uniqueResult();
					assertEquals( "description", value );
				}
		);
		assertTrue( mostRecentStatementInspector.mostRecentSql.contains( "substr(" ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11957")
	public void testSubstrWithStringUnits() {

		mostRecentStatementInspector.clear();

		try {
			doInHibernate(
					this::sessionFactory, session -> {
						String value = session.createQuery(
								"select substr( e.description, 21, 11, octets ) from AnEntity e",
								String.class
						).uniqueResult();
						assertEquals( "description", value );
					}
			);
			fail( "Should have failed because substr cannot be used with string units." );
		}
		catch (PersistenceException expected) {
			assertTrue( SQLGrammarException.class.isInstance( expected.getCause() ) );
		}

		assertTrue( mostRecentStatementInspector.mostRecentSql.contains( "substr(" ) );
		assertTrue( mostRecentStatementInspector.mostRecentSql.contains( "octets" ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11957")
	public void testSubstrWithoutStringUnits() {

		mostRecentStatementInspector.clear();

		doInHibernate(
				this::sessionFactory, session -> {
					String value = session.createQuery(
							"select substr( e.description, 21, 11 ) from AnEntity e",
							String.class
					).uniqueResult();
					assertEquals( "description", value );
				}
		);
		assertTrue( mostRecentStatementInspector.mostRecentSql.contains( "substr(" ) );
	}

	@Entity(name="AnEntity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private long id;
		private String description;
	}

	private static class MostRecentStatementInspector implements StatementInspector {
		private String mostRecentSql;

		public String inspect(String sql) {
			mostRecentSql = sql;
			return sql;
		}
		private void clear() {
			mostRecentSql = null;
		}
	}
}