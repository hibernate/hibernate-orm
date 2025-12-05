/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exception;

import java.sql.PreparedStatement;
import java.sql.Types;

import org.hibernate.Session;
import org.hibernate.dialect.HANADialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.SQLGrammarException;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * Implementation of SQLExceptionConversionTest.
 *
 * @author Steve Ebersole
 */
@DomainModel(xmlMappings = {
		"org/hibernate/orm/test/exception/User.hbm.xml",
		"org/hibernate/orm/test/exception/Group.hbm.xml"
})
@SessionFactory
public class SQLExceptionConversionTest {

	@Test
	@SkipForDialect(value = HANADialect.class, comment = "Hana do not support FK violation checking")
	@SkipForDialect(value = TiDBDialect.class, comment = "TiDB do not support FK violation checking")
	public void testIntegrityViolation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			session.doWork(
					connection -> {
						// Attempt to insert some bad values into the T_MEMBERSHIP table that should
						// result in a constraint violation
						PreparedStatement ps = null;
						try {
							final String sql = "INSERT INTO T_MEMBERSHIP (user_id, group_id) VALUES (?, ?)";
							ps = ((SessionImplementor) session).getJdbcCoordinator()
									.getStatementPreparer()
									.prepareStatement( sql );
							ps.setLong( 1, 52134241 );    // Non-existent user_id
							ps.setLong( 2, 5342 );        // Non-existent group_id
							((SessionImplementor) session).getJdbcCoordinator().getResultSetReturn()
									.executeUpdate( ps, sql );

							fail( "INSERT should have failed" );
						}
						catch (ConstraintViolationException ignore) {
							// expected outcome
						}
						finally {
							releaseStatement( session, ps );
						}
					}
			);
		} );
	}

	@Test
	public void testBadGrammar(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			session.doWork(
					connection -> {
						// prepare/execute a query against a non-existent table
						PreparedStatement ps = null;
						try {
							final String sql = "SELECT user_id, user_name FROM tbl_no_there";
							ps = ((SessionImplementor) session).getJdbcCoordinator().getStatementPreparer()
									.prepareStatement( sql );
							((SessionImplementor) session).getJdbcCoordinator().getResultSetReturn().extract( ps, sql );

							fail( "SQL compilation should have failed" );
						}
						catch (SQLGrammarException ignored) {
							// expected outcome
						}
						finally {
							releaseStatement( session, ps );
						}
					}
			);

		} );
	}

	@Test
	@JiraKey(value = "HHH-7357")
	public void testNotNullConstraint(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			final User user = new User();
			user.setUsername( "Lukasz" );
			session.persist( user );
			session.flush();

			session.doWork(
					connection -> {
						final JdbcCoordinator jdbcCoordinator = ((SessionImplementor) session).getJdbcCoordinator();
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
						catch (ConstraintViolationException ignore) {
							// expected outcome
						}
						finally {
							releaseStatement( session, ps );
						}
					}
			);

		} );
	}

	private void releaseStatement(Session session, PreparedStatement ps) {
		if ( ps != null ) {
			try {
				((SessionImplementor) session).getJdbcCoordinator().getLogicalConnection().getResourceRegistry()
						.release( ps );
			}
			catch (Throwable ignore) {
				// ignore...
			}
		}
	}
}
