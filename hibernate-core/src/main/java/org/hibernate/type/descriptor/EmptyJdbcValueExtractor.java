/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public class EmptyJdbcValueExtractor implements ValueExtractor {
	/**
	 * Singleton access
	 */
	public static final EmptyJdbcValueExtractor INSTANCE = new EmptyJdbcValueExtractor();

	@Override
	public Object extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
		return null;
	}

	@Override
	public Object extract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
		return null;
	}

	@Override
	public Object extract(CallableStatement statement, String paramName, WrapperOptions options) throws SQLException {
		return null;
	}
}
