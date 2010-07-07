/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.Test;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * GeneralWorkTest implementation
 *
 * @author Steve Ebersole
 */
public class GeneralWorkTest extends FunctionalTestCase {
	public GeneralWorkTest(String string) {
		super( string );
	}

	public String getBaseForMappings() {
		return "org/hibernate/";
	}

	public String[] getMappings() {
		return new String[] { "jdbc/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( GeneralWorkTest.class );
	}

	public void testGeneralUsage() throws Throwable {
		Session session = openSession();
		session.beginTransaction();
		session.doWork(
				new Work() {
					public void execute(Connection connection) throws SQLException {
						// in this current form, users must handle try/catches themselves for proper resource release
						Statement statement = null;
						try {
							statement = connection.createStatement();
							ResultSet resultSet = null;
							try {
								resultSet = statement.executeQuery( "select * from T_JDBC_PERSON" );
							}
							finally {
								releaseQuietly( resultSet );
							}
							try {
								resultSet = statement.executeQuery( "select * from T_JDBC_BOAT" );
							}
							finally {
								releaseQuietly( resultSet );
							}
						}
						finally {
							releaseQuietly( statement );
						}
					}
				}
		);
		session.getTransaction().commit();
		session.close();
	}

	public void testSQLExceptionThrowing() {
		Session session = openSession();
		session.beginTransaction();
		try {
			session.doWork(
					new Work() {
						public void execute(Connection connection) throws SQLException {
							Statement statement = null;
							try {
								statement = connection.createStatement();
								statement.executeQuery( "select * from non_existent" );
							}
							finally {
								releaseQuietly( statement );
							}
						}
					}
			);
			fail( "expecting exception" );
		}
		catch ( JDBCException expected ) {
			// expected outcome
		}
		session.getTransaction().commit();
		session.close();
	}

	private void releaseQuietly(Statement statement) {
		if ( statement == null ) {
			return;
		}
		try {
			statement.close();
		}
		catch ( SQLException e ) {
			// ignore
		}
	}

	private void releaseQuietly(ResultSet resultSet) {
		if ( resultSet == null ) {
			return;
		}
		try {
			resultSet.close();
		}
		catch ( SQLException e ) {
			// ignore
		}
	}
}
