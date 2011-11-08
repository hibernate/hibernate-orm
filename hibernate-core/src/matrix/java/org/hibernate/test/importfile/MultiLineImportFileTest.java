package org.hibernate.test.importfile;

import java.math.BigInteger;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue( jiraKey = "HHH-2403" )
public class MultiLineImportFileTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.HBM2DDL_IMPORT_FILES, "/multiline-stmt.sql" );
		cfg.setProperty( Environment.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR, MultipleLinesSqlCommandExtractor.class.getName() );
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
		assertEquals( "incorrect row number", 3L, count.longValue() );

		String multilineText = (String) s.createSQLQuery( "SELECT text FROM test_data WHERE id = 2" ).uniqueResult();
		assertEquals( "multiline string inserted incorrectly", "Multiline comment line 1\r\n-- line 2'\r\n/* line 3 */", multilineText );

		String empty = (String) s.createSQLQuery( "SELECT text FROM test_data WHERE id = 3" ).uniqueResult();
		assertNull( "NULL value inserted incorrectly", empty );

		tx.commit();
		s.close();
	}
}
