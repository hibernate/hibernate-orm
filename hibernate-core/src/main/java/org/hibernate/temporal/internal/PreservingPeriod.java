package org.hibernate.temporal.internal;

import java.time.Instant;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;

/// Resolves a period SQL expression using the same encoding as its JDBC mapping.
///
/// @author Steve Ebersole
public final class PreservingPeriod {
	private PreservingPeriod() {}

	public static String currentExpression(SessionFactoryImplementor factory, JdbcMapping mapping) {
		final var dialect = factory.getJdbcServices().getDialect();
		if ( !factory.getSessionFactoryOptions().isTemporalPreservationEnabled()
				|| mapping.getJdbcJavaType().getJavaTypeClass() != Instant.class ) {
			return dialect.getCurrentTemporalSupport().currentTimestamp();
		}
		final var precision = factory.getTemporalPeriodPrecision();
		final var expression = precision == null ? null : precision.getCurrentExpression();
		if ( expression == null ) {
			throw new MappingException( "No preserving inline system-period expression for " + mapping.getJdbcType().getClass().getName() );
		}
		return expression;
	}
}
