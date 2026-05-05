/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JdbcParameter for temporal restrictions; bound via JdbcParameterBindings.
 *
 * @author Gavin King
 */
public class TemporalJdbcParameter extends SqlTypedMappingJdbcParameter {

	public TemporalJdbcParameter(SqlTypedMapping sqlTypedMapping) {
		super( sqlTypedMapping );
	}

	@Override
	public void bindParameterValue(PreparedStatement statement, int startPosition, JdbcParameterBindings jdbcParamBindings, ExecutionContext executionContext)
			throws SQLException {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final Object temporalIdentifier = session.getLoadQueryInfluencers().getTemporalIdentifier();
		final Object currentChangesetIdentifier = temporalIdentifier != null
				? temporalIdentifier
				: session.getCurrentChangesetIdentifier();
		if ( currentChangesetIdentifier == null ) {
			throw new ExecutionException( "JDBC parameter value not bound - " + this );
		}
		final Object bindValue = getJdbcMapping().convertToRelationalValue( currentChangesetIdentifier );
		bindParameterValue( getJdbcMapping(), statement, bindValue, startPosition, executionContext );
	}
}
