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
		final SharedSessionContractImplementor session = executionContext.getSession();
		final Object temporalIdentifier = session.getLoadQueryInfluencers().getTemporalIdentifier();
		final Object currentChangesetIdentifier = temporalIdentifier != null && !current
				? temporalIdentifier
				: session.getCurrentChangesetIdentifier();
		if ( currentChangesetIdentifier == null ) {
			throw new ExecutionException( "JDBC parameter value not bound - " + this );
		}
		final Object bindValue = getJdbcMapping().convertToRelationalValue( currentChangesetIdentifier );
		bindParameterValue( getJdbcMapping(), statement, bindValue, startPosition, executionContext );
	}
}
