/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc;

import org.hibernate.JDBCException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * GeneralWorkTest implementation
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/jdbc/Mappings.hbm.xml")
@SessionFactory
public class GeneralWorkTest {
	@AfterEach
	void cleanup(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testGeneralUsage(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			// in this current form, users must handle try/catches themselves for proper resource release
			Statement statement = null;
			try {
				statement = session.getJdbcCoordinator().getStatementPreparer().createStatement();
				ResultSet resultSet = null;
				try {
					resultSet = session.getJdbcCoordinator().getResultSetReturn().extract( statement, "select * from T_JDBC_PERSON" );
				}
				finally {
					releaseQuietly( session, resultSet, statement );
				}
				try {
					session.getJdbcCoordinator().getResultSetReturn().extract( statement, "select * from T_JDBC_BOAT" );
				}
				finally {
					releaseQuietly( session, resultSet, statement );
				}
			}
			finally {
				releaseQuietly( session, statement );
			}
		} ) );
	}

	@Test
	public void testSQLExceptionThrowing(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.doWork( (connection) -> {
					Statement statement = null;
					try {
						statement = session.getJdbcCoordinator().getStatementPreparer().createStatement();
						session.getJdbcCoordinator().getResultSetReturn().extract( statement, "select * from non_existent" );
					}
					finally {
						releaseQuietly( session, statement );
					}
					fail( "expecting exception" );
				} );
			}
			catch ( JDBCException expected ) {
				// expected outcome
			}
		} );
	}

	@Test
	public void testGeneralReturningUsage(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var p = new Person( "Abe", "Lincoln" );
			session.persist( p );
		} );

		factoryScope.inTransaction( (session) -> {
			long count = session.doReturningWork( (connection) -> {
				// in this current form, users must handle try/catches themselves for proper resource release
				Statement statement = null;
				long personCount = 0;
				try {
					statement = session.getJdbcCoordinator().getStatementPreparer().createStatement();
					ResultSet resultSet = null;
					try {
						resultSet = session.getJdbcCoordinator().getResultSetReturn().extract( statement, "select count(*) from T_JDBC_PERSON" );
						resultSet.next();
						personCount = resultSet.getLong( 1 );
						assertEquals( 1L, personCount );
					}
					finally {
						releaseQuietly( session, resultSet, statement );
					}
				}
				finally {
					releaseQuietly( session, statement );
				}
				return personCount;
			} );
		} );
	}

	private void releaseQuietly(SessionImplementor s, Statement statement) {
		if ( statement == null ) {
			return;
		}
		try {
			s.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( statement );
		}
		catch (Exception e) {
			// ignore
		}
	}

	private void releaseQuietly(SessionImplementor s, ResultSet resultSet, Statement statement) {
		if ( resultSet == null ) {
			return;
		}
		try {
			s.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( resultSet, statement );
		}
		catch (Exception e) {
			// ignore
		}
	}
}
