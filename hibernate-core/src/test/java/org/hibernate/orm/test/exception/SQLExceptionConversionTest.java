/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exception;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.SQLGrammarException;

import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.spi.ConversionContext;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Implementation of SQLExceptionConversionTest.
 *
 * @author Steve Ebersole
 */
public class SQLExceptionConversionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] {"exception/User.hbm.xml", "exception/Group.hbm.xml"};
	}

	@Test
	@SkipForDialect(value = HANADialect.class, comment = "Hana do not support FK violation checking")
	@SkipForDialect(value = TiDBDialect.class, comment = "TiDB do not support FK violation checking")
	public void testIntegrityViolation() {
		final Session session = openSession();
		session.beginTransaction();

		session.doWork(
				connection -> {
					// Attempt to insert some bad values into the T_MEMBERSHIP table that should
					// result in a constraint violation
					PreparedStatement ps = null;
					try {
						final String sql = "INSERT INTO T_MEMBERSHIP (user_id, group_id) VALUES (?, ?)";
						ps = ((SessionImplementor)session).getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( sql );
						ps.setLong(1, 52134241);    // Non-existent user_id
						ps.setLong(2, 5342);        // Non-existent group_id
						((SessionImplementor)session).getJdbcCoordinator().getResultSetReturn().executeUpdate( ps, sql );

						fail("INSERT should have failed");
					}
					catch (ConstraintViolationException ignore) {
						// expected outcome
					}
					finally {
						releaseStatement( session, ps );
					}
				}
		);

		session.getTransaction().rollback();
		session.close();
	}

	@Test
	public void testBadGrammar() {
		final Session session = openSession();
		session.beginTransaction();

		session.doWork(
				connection -> {
					// prepare/execute a query against a non-existent table
					PreparedStatement ps = null;
					try {
						final String sql = "SELECT user_id, user_name FROM tbl_no_there";
						ps = ((SessionImplementor)session).getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
						((SessionImplementor)session).getJdbcCoordinator().getResultSetReturn().extract( ps, sql );

						fail("SQL compilation should have failed");
					}
					catch (SQLGrammarException ignored) {
						// expected outcome
					}
					finally {
						releaseStatement( session, ps );
					}
				}
		);

		session.getTransaction().rollback();
		session.close();
	}

	@Test
	@JiraKey(value = "HHH-7357")
	public void testNotNullConstraint() {
		final Session session = openSession();
		session.beginTransaction();

		final User user = new User();
		user.setUsername( "Lukasz" );
		session.persist( user );
		session.flush();

		session.doWork(
				connection -> {
					final JdbcCoordinator jdbcCoordinator = ( (SessionImplementor) session ).getJdbcCoordinator();
					final StatementPreparer statementPreparer = jdbcCoordinator.getStatementPreparer();
					final ResultSetReturn resultSetReturn = jdbcCoordinator.getResultSetReturn();
					PreparedStatement ps = null;
					try {
						final String sql = "UPDATE T_USER SET user_name = ? WHERE user_id = ?";
						ps = statementPreparer.prepareStatement( sql );
						// Attempt to update username to NULL (NOT NULL constraint defined).
						ps.setNull( 1, Types.VARCHAR );
						ps.setLong( 2, user.getId() );
						resultSetReturn.executeUpdate( ps, sql );

						fail( "UPDATE should have failed because of not NULL constraint." );
					}
					catch ( ConstraintViolationException ignore ) {
						// expected outcome
					}
					finally {
						releaseStatement( session, ps );
					}
				}
		);

		session.getTransaction().rollback();
		session.close();
	}

	private void releaseStatement(Session session, PreparedStatement ps) {
		if ( ps != null ) {
			try {
				((SessionImplementor) session).getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
			}
			catch ( Throwable ignore ) {
				// ignore...
			}
		}
	}

	@Test
	@JiraKey(value = "HHH-19001")
	public void testConstraintViolation() {
		final Session session = openSession();
		session.beginTransaction();

		session.doWork(
				connection -> {
					// Attempt to insert some existent values into the T_MEMBERSHIP table that should
					// result in a constraint violation
					PreparedStatement ps = null;
					try {
						final String sql = "INSERT INTO T_MEMBERSHIP (user_id, group_id) VALUES (?, ?)";
						ps = ((SessionImplementor)session).getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( sql );
						ps.setLong(1, 1);    // existent user_id
						ps.setLong(2, 1);    // existent group_id
						((SessionImplementor)session).getJdbcCoordinator().getResultSetReturn().executeUpdate( ps, sql );

						fail("INSERT should have failed");
					}
					catch (ConstraintViolationException cvException) {
						SQLException sqlException = new SQLException(cvException.getSQLException().getMessage(), "23505");
						throw convertSqlException(sqlException.getMessage(), sqlException);
					}
					finally {
						releaseStatement( session, ps );
					}
				}
		);

		session.getTransaction().rollback();
		session.close();
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
				if ( sqlState != null ) {
					final String message = sqle.getMessage();
					final int idx = message.indexOf( "violation: " );
					if ( idx > 0 ) {
						String constraintName = message.substring( idx + "violation: ".length() );
						if ( sqle.getSQLState().equals( "23505" ) ) {
							constraintName = constraintName.substring( 1, constraintName.indexOf( ':' ) );
						}
						return constraintName;
					}
				}
				return null;
			} );

	protected JDBCException convertSqlException(String message, SQLException e) {
		final String fullMessage = message + " [" + e.getMessage() + "]";
		return simpleConverterAccess.getValue().convert( e, fullMessage, null );
	}

	private final ValueHolder<SQLExceptionConversionDelegate> simpleConverterAccess =
			new ValueHolder<>( () -> new SQLExceptionConversionDelegate() {
				private final SQLStateConversionDelegate sqlStateDelegate = new SQLStateConversionDelegate(
						new ConversionContext() {
							@Override
							public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
								return EXTRACTOR;
							}
						} );


				@Override
				public JDBCException convert(SQLException sqlException, String message, String sql) {
					JDBCException exception = sqlStateDelegate.convert( sqlException, message, sql );
					if ( exception == null ) {
						exception = new ConstraintViolationException( message, sqlException, sql );
					}
					return exception;
				}
			}
			);
}
