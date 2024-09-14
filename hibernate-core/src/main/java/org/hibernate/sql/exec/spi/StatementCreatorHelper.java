/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.exec.spi;

import java.sql.PreparedStatement;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Helper for creating JDBC statements.
 *
 * @author Steve Ebersole
 */
public class StatementCreatorHelper {
	public static PreparedStatement prepareQueryStatement(
			String sql,
			SharedSessionContractImplementor session) {
		return session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql );
	}
}
