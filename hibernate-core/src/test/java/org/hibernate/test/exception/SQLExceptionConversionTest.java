/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.exception;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.dialect.MySQLMyISAMDialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.jdbc.Work;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.fail;

/**
 * Implementation of SQLExceptionConversionTest.
 *
 * @author Steve Ebersole
 */
public class SQLExceptionConversionTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] {"exception/User.hbm.xml", "exception/Group.hbm.xml"};
	}

	@Test
	@SkipForDialect(
			value = MySQLMyISAMDialect.class,
			comment = "MySQL (MyISAM) does not support FK violation checking"
	)
	public void testIntegrityViolation() throws Exception {
		Session session = openSession();
		session.beginTransaction();

		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
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
						catch (ConstraintViolationException ignore) {
							// expected outcome
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
					}
				}
		);

		session.getTransaction().rollback();
		session.close();
	}

	@Test
	public void testBadGrammar() throws Exception {
		Session session = openSession();
		session.beginTransaction();

		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						// prepare/execute a query against a non-existent table
						PreparedStatement ps = null;
						try {
							ps = connection.prepareStatement("SELECT user_id, user_name FROM tbl_no_there");
							ps.executeQuery();

							fail("SQL compilation should have failed");
						}
						catch (SQLGrammarException ignored) {
							// expected outcome
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
					}
				}
		);

		session.getTransaction().rollback();
		session.close();
	}
}
