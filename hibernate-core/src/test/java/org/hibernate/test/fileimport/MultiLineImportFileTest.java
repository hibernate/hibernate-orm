/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fileimport;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jdbc.Work;
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-2403")
@RequiresDialect(value = H2Dialect.class,
		jiraKey = "HHH-6286",
		comment = "Only running the tests against H2, because the sql statements in the import file are not generic. " +
				"This test should actually not test directly against the db")
public class MultiLineImportFileTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.HBM2DDL_IMPORT_FILES, "/org/hibernate/test/fileimport/multi-line-statements.sql" );
		cfg.setProperty(
				Environment.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR,
				MultipleLinesSqlCommandExtractor.class.getName()
		);
	}

	@Override
	public String[] getMappings() {
		return NO_MAPPINGS;
	}

	@Test
	public void testImportFile() throws Exception {
		Session s = openSession();
		final Transaction tx = s.beginTransaction();

		BigInteger count = (BigInteger) s.createSQLQuery( "SELECT COUNT(*) FROM test_data" ).uniqueResult();
		assertEquals( "Incorrect row number", 3L, count.longValue() );

		final String multiLineText = (String) s.createSQLQuery( "SELECT text FROM test_data WHERE id = 2" )
				.uniqueResult();
		//  "Multi-line comment line 1\n-- line 2'\n/* line 3 */"
		final String expected = String.format( "Multi-line comment line 1%n-- line 2'%n/* line 3 */" );
		assertEquals( "Multi-line string inserted incorrectly", expected, multiLineText );

		String empty = (String) s.createSQLQuery( "SELECT text FROM test_data WHERE id = 3" ).uniqueResult();
		assertNull( "NULL value inserted incorrectly", empty );

		tx.commit();
		s.close();
	}

	@AfterClassOnce
	public void tearDown() {
		final Session session = openSession();
		session.getTransaction().begin();
		session.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				PreparedStatement statement = null;
				try {
					statement = connection.prepareStatement( "DROP TABLE test_data" );
					statement.execute();
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			}
		} );
		session.getTransaction().commit();
		session.close();
	}
}
