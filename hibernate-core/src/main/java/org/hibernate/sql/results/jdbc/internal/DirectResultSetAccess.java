/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class DirectResultSetAccess extends AbstractResultSetAccess {
	private final PreparedStatement resultSetSource;
	private final ResultSet resultSet;

	public DirectResultSetAccess(
			SharedSessionContractImplementor persistenceContext,
			PreparedStatement resultSetSource,
			ResultSet resultSet) {
		super( persistenceContext );
		this.resultSetSource = resultSetSource;
		this.resultSet = resultSet;

		persistenceContext.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().register( resultSet, resultSetSource );
	}

	@Override
	public ResultSet getResultSet() {
		return resultSet;
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return getPersistenceContext().getFactory();
	}

	@Override
	public void release() {
		getPersistenceContext().getJdbcCoordinator()
				.getLogicalConnection()
				.getResourceRegistry()
				.release( resultSet, resultSetSource );
	}
}
