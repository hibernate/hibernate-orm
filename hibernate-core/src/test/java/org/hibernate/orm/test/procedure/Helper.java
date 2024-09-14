/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.procedure;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.transaction.TransactionUtil;

/**
 * @author Steve Ebersole
 */
public class Helper {
	@FunctionalInterface
	public interface StatementAction {
		void accept(Statement statement) throws SQLException;
	}

	public static void withStatement(SessionFactoryImplementor sessionFactory, StatementAction action) throws SQLException {
		TransactionUtil.doWithJDBC(
				sessionFactory.getServiceRegistry(),
				connection -> {
					withStatement( connection, action );
				}
		);
	}

	public static void withStatement(Connection connection, StatementAction action) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			action.accept( statement );
		}
	}
}
