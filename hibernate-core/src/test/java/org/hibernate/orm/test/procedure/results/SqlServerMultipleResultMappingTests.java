/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure.results;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(value = SQLServerDialect.class)
@SessionFactory
public class SqlServerMultipleResultMappingTests extends AbstractMultipleResultMappingTests {

	@BeforeAll
	void createFunctionsAndProcedures(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate(
						"""
							CREATE PROCEDURE get_results
							AS
							BEGIN
								SELECT id, code, name from regions;
								SELECT id, name, target_quarter FROM initiatives;
							END;
							"""
				);
			}
			catch (SQLException e) {
				throw session.getSessionFactory()
						.getJdbcServices()
						.getSqlExceptionHelper()
						.convert( e, "Error exporting procedure and function definitions" );
			}
		} ) );
	}

	@AfterAll
	void dropFunctionsAndProcedures(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate( "DROP PROCEDURE IF EXISTS get_results;" );
			}
			catch (SQLException e) {
				throw session.getSessionFactory()
						.getJdbcServices()
						.getSqlExceptionHelper()
						.convert( e, "Error dropping procedure and function definitions" );
			}
		} ) );
	}

}
