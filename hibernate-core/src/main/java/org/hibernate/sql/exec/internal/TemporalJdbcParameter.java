package org.hibernate.sql.exec.internal;

import jakarta.annotation.Nullable;

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

	private final boolean current;

	public TemporalJdbcParameter(SqlTypedMapping sqlTypedMapping) {
		this( sqlTypedMapping, false );
	}

	/**
	 * @param current whether the parameter is bound to the current changeset,
	 * whatever changeset the session reads at
	 */
	public TemporalJdbcParameter(SqlTypedMapping sqlTypedMapping, boolean current) {
		super( sqlTypedMapping );
		this.current = current;
	}

	@Override
	public void bindParameterValue(PreparedStatement statement, int startPosition, JdbcParameterBindings jdbcParamBindings, ExecutionContext executionContext)
			throws SQLException {
		final Object bindValue = getJdbcMapping().convertToRelationalValue( getParameterValue( executionContext.getSession() ) );
		bindParameterValue( getJdbcMapping(), statement, bindValue, startPosition, executionContext );
	}

	/// The historical value bound by this parameter, or null for a current read.
	public @Nullable Object getHistoricalChangeset(SharedSessionContractImplementor session) {
		return current ? null : session.getLoadQueryInfluencers().getTemporalIdentifier();
	}

	private Object getParameterValue(SharedSessionContractImplementor session) {
		final Object temporalIdentifier = getHistoricalChangeset( session );
		final Object currentChangesetIdentifier = temporalIdentifier != null
				? temporalIdentifier
				: session.getCurrentChangesetIdentifier();
		if ( currentChangesetIdentifier == null ) {
			throw new ExecutionException( "JDBC parameter value not bound - " + this );
		}
		return currentChangesetIdentifier;
	}
}
