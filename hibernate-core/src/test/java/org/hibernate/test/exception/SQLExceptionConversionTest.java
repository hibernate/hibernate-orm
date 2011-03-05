package org.hibernate.test.exception;

import java.sql.Connection;
import java.sql.PreparedStatement;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.dialect.MySQLMyISAMDialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Implementation of SQLExceptionConversionTest.
 *
 * @author Steve Ebersole
 */
public class SQLExceptionConversionTest extends FunctionalTestCase {

	public SQLExceptionConversionTest(String name) {
		super(name);
	}

	public String[] getMappings() {
		return new String[] {"exception/User.hbm.xml", "exception/Group.hbm.xml"};
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite(SQLExceptionConversionTest.class);
	}

	public void testIntegrityViolation() throws Exception {
		if ( getDialect() instanceof MySQLMyISAMDialect ) {
			reportSkip( "MySQL (ISAM) does not support FK violation checking", "exception conversion" );
			return;
		}

		SQLExceptionConverter converter = getDialect().buildSQLExceptionConverter();

		Session session = openSession();
		session.beginTransaction();
		Connection connection = session.connection();

		// Attempt to insert some bad values into the T_MEMBERSHIP table that should
		// result in a constraint violation
		PreparedStatement ps = null;
		try {
			ps = connection.prepareStatement("INSERT INTO T_MEMBERSHIP (user_id, group_id) VALUES (?, ?)");
			ps.setLong(1, 52134241);    // Non-existent user_id
			ps.setLong(2, 5342);        // Non-existent group_id
			ps.executeUpdate();

			fail("INSERT should have failed");
		}
		catch (ConstraintViolationException expected) {
		}
		finally {
			if ( ps != null ) {
				try {
					ps.close();
				}
				catch( Throwable ignore ) {
					// ignore...
				}
			}
		}

		session.getTransaction().rollback();
		session.close();
	}

	public void testBadGrammar() throws Exception {
		SQLExceptionConverter converter = getDialect().buildSQLExceptionConverter();

		Session session = openSession();
		Connection connection = session.connection();

        // prepare/execute a query against a non-existent table
		PreparedStatement ps = null;
		try {
			ps = connection.prepareStatement("SELECT user_id, user_name FROM tbl_no_there");
			ps.executeQuery();

			fail("SQL compilation should have failed");
		}
		catch (SQLGrammarException expected) {
		}
		finally {
			if ( ps != null ) {
				try {
					ps.close();
				}
				catch( Throwable ignore ) {
					// ignore...
				}
			}
		}

		session.close();
	}
}
