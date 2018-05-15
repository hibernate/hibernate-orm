/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public class EmptyJdbcValueExtractor implements JdbcValueExtractor {
	/**
	 * Singleton access
	 */
	public static final EmptyJdbcValueExtractor INSTANCE = new EmptyJdbcValueExtractor();

	@Override
	public Object extract(ResultSet rs, int position, ExecutionContext executionContext)
			throws SQLException {
		return null;
	}

	@Override
	public Object extract(CallableStatement statement, int jdbcParameterPosition, ExecutionContext executionContext)
			throws SQLException {
		return null;
	}

	@Override
	public Object extract(CallableStatement statement, String jdbcParameterName, ExecutionContext executionContext)
			throws SQLException {
		return null;
	}
}
