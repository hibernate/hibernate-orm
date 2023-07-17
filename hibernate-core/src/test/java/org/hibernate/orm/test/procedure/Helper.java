/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.procedure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Steve Ebersole
 */
public class Helper {
	@FunctionalInterface
	public interface StatementAction {
		void accept(Statement statement) throws SQLException;
	}

	public static void withStatement(SessionFactoryImplementor sessionFactory, StatementAction action) throws SQLException {
		final JdbcConnectionAccess connectionAccess = sessionFactory.getServiceRegistry()
				.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess();

		try (Connection connection = connectionAccess.obtainConnection()) {
			withStatement( connection, action );
		}
	}

	public static void withStatement(Connection connection, StatementAction action) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			action.accept( statement );
		}
	}
}
