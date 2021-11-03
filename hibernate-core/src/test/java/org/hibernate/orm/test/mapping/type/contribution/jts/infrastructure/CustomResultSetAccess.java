/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.jts.infrastructure;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess;

import org.h2gis.utilities.SpatialResultSet;

/**
 * @author Steve Ebersole
 */
public class CustomResultSetAccess extends DeferredResultSetAccess {
	public CustomResultSetAccess(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			Function<String, PreparedStatement> statementCreator) {
		super( jdbcSelect, jdbcParameterBindings, executionContext, statementCreator );
	}

	@Override
	protected ResultSet wrapResultSet(ResultSet resultSet) throws SQLException {
		return resultSet.unwrap( SpatialResultSet.class );
	}
}
