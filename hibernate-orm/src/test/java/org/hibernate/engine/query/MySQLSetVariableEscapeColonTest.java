/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.Session;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.jdbc.Work;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(MySQL5Dialect.class)
@TestForIssue( jiraKey = "HHH-1237")
public class MySQLSetVariableEscapeColonTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { };
	}

	@Test
	public void testBoundedLongStringAccess() {

		Session s = openSession();
		s.beginTransaction();
		try {
			s.doWork( new Work() {
				@Override
				public void execute(Connection connection) throws SQLException {
					Statement statement = connection.createStatement();
					try {
						statement.executeUpdate( "SET @a='test'" );
					}
					finally {
						statement.close();
					}
				}
			}  );
			Object[] result = (Object[]) session.createSQLQuery( "SELECT @a, (@a::=20) FROM dual" ).uniqueResult();
			assertEquals("test", result[0]);
			assertEquals(20, ((Number) result[1]).intValue());

			s.getTransaction().commit();
		}
		finally {
			s.close();
		}
	}

}

