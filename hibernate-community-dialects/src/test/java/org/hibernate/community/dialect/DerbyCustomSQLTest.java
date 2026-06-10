/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.orm.test.sql.hand.Employment;
import org.hibernate.orm.test.sql.hand.ImageHolder;
import org.hibernate.orm.test.sql.hand.Organization;
import org.hibernate.orm.test.sql.hand.Person;
import org.hibernate.orm.test.sql.hand.TextHolder;
import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(DerbyDialect.class)
@DomainModel(
		annotatedClasses = {
				Organization.class,
				Person.class,
				Employment.class,
				TextHolder.class,
				ImageHolder.class
		}
)
public class DerbyCustomSQLTest extends CustomStoredProcTestSupport {
	@BeforeEach
	public void createProcedures(SessionFactoryScope scope) {
		scope.inSession(
				session -> session.doWork(
						connection -> {
							dropProcedures( connection.createStatement() );
							createProcedures( connection.createStatement() );
						}
				)
		);
	}

	@AfterEach
	public void dropProcedures(SessionFactoryScope scope) {
		scope.inSession(
				session -> session.doWork(
						connection -> dropProcedures( connection.createStatement() )
				)
		);
	}

	private void createProcedures(Statement statement) throws SQLException {
		try (statement) {
			statement.execute( """
					CREATE PROCEDURE selectAllEmployments ()
					PARAMETER STYLE JAVA
					LANGUAGE JAVA
					READS SQL DATA
					DYNAMIC RESULT SETS 2
					EXTERNAL NAME 'org.hibernate.community.dialect.DerbyStoreProcedures.selectAllEmployments'
					""" );
			statement.execute( """
					CREATE PROCEDURE paramHandling (j SMALLINT, i SMALLINT)
					PARAMETER STYLE JAVA
					LANGUAGE JAVA
					READS SQL DATA
					DYNAMIC RESULT SETS 2
					EXTERNAL NAME 'org.hibernate.community.dialect.DerbyStoreProcedures.paramHandling'
					""" );
			statement.execute( """
					CREATE PROCEDURE simpleScalar (p_number SMALLINT)
					PARAMETER STYLE JAVA
					LANGUAGE JAVA
					READS SQL DATA
					DYNAMIC RESULT SETS 2
					EXTERNAL NAME 'org.hibernate.community.dialect.DerbyStoreProcedures.simpleScalar'
					""" );
		}
	}

	private void dropProcedures(Statement statement) throws SQLException {
		try (statement) {
			dropProcedure( statement, "selectAllEmployments" );
			dropProcedure( statement, "paramHandling" );
			dropProcedure( statement, "simpleScalar" );
		}
	}

	private void dropProcedure(Statement statement, String name) throws SQLException {
		try {
			statement.execute( "DROP PROCEDURE " + name );
		}
		catch (SQLException e) {
			if ( !"42Y55".equals( e.getSQLState() ) ) {
				throw e;
			}
		}
	}
}
