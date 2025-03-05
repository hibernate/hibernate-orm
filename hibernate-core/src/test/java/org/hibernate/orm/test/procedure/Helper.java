/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
