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
import java.sql.Types;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.MySQLMyISAMDialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.jdbc.Work;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
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
			value = { MySQLMyISAMDialect.class, AbstractHANADialect.class },
			comment = "MySQL (MyISAM) / Hana do not support FK violation checking"
	)
	public void testIntegrityViolation() throws Exception {
		final Session session = openSession();
		session.beginTransaction();

		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						// Attempt to insert some bad values into the T_MEMBERSHIP table that should
						// result in a constraint violation
						PreparedStatement ps = null;
						try {
							ps = ((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( "INSERT INTO T_MEMBERSHIP (user_id, group_id) VALUES (?, ?)" );
							ps.setLong(1, 52134241);    // Non-existent user_id
							ps.setLong(2, 5342);        // Non-existent group_id
							((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( ps );

							fail("INSERT should have failed");
						}
						catch (ConstraintViolationException ignore) {
							// expected outcome
						}
						finally {
							releaseStatement( session, ps );
						}
					}
				}
		);

		session.getTransaction().rollback();
		session.close();
	}

	@Test
	public void testBadGrammar() throws Exception {
		final Session session = openSession();
		session.beginTransaction();

		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						// prepare/execute a query against a non-existent table
						PreparedStatement ps = null;
						try {
							ps = ((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( "SELECT user_id, user_name FROM tbl_no_there" );
							((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( ps );

							fail("SQL compilation should have failed");
						}
						catch (SQLGrammarException ignored) {
							// expected outcome
						}
						finally {
							releaseStatement( session, ps );
						}
					}
				}
		);

		session.getTransaction().rollback();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7357")
	public void testNotNullConstraint() {
		final Session session = openSession();
		session.beginTransaction();

		final User user = new User();
		user.setUsername( "Lukasz" );
		session.save( user );
		session.flush();

		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						final JdbcCoordinator jdbcCoordinator = ( (SessionImplementor) session ).getTransactionCoordinator().getJdbcCoordinator();
						final StatementPreparer statementPreparer = jdbcCoordinator.getStatementPreparer();
						final ResultSetReturn resultSetReturn = jdbcCoordinator.getResultSetReturn();
						PreparedStatement ps = null;
						try {
							ps = statementPreparer.prepareStatement( "UPDATE T_USER SET user_name = ? WHERE user_id = ?" );
							ps.setNull( 1, Types.VARCHAR ); // Attempt to update user name to NULL (NOT NULL constraint defined).
							ps.setLong( 2, user.getId() );
							resultSetReturn.executeUpdate( ps );

							fail( "UPDATE should have failed because of not NULL constraint." );
						}
						catch ( ConstraintViolationException ignore ) {
							// expected outcome
						}
						finally {
							releaseStatement( session, ps );
						}
					}
				}
		);

		session.getTransaction().rollback();
		session.close();
	}

	private void releaseStatement(Session session, PreparedStatement ps) {
		if ( ps != null ) {
			try {
				( (SessionImplementor) session ).getTransactionCoordinator().getJdbcCoordinator().release( ps );
			}
			catch ( Throwable ignore ) {
				// ignore...
			}
		}
	}
}
