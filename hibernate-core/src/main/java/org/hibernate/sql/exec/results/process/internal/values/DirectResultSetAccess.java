/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal.values;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.results.process.internal.values.JdbcValuesSourceResultSetImpl.ResultSetAccess;

/**
 * @author Steve Ebersole
 */
public class DirectResultSetAccess implements ResultSetAccess {
	private final SharedSessionContractImplementor persistenceContext;
	private final PreparedStatement resultSetSource;
	private final ResultSet resultSet;

	public DirectResultSetAccess(
			SharedSessionContractImplementor persistenceContext,
			PreparedStatement resultSetSource, ResultSet resultSet) {
		this.persistenceContext = persistenceContext;
		this.resultSetSource = resultSetSource;
		this.resultSet = resultSet;

		persistenceContext.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().register( resultSet, resultSetSource );
	}

	@Override
	public ResultSet getResultSet() {
		return resultSet;
	}

	@Override
	public void release() {
		persistenceContext.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( resultSet, resultSetSource );
	}
}
