package org.hibernate.dialect.temporal.spi;

import java.util.Objects;

import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/// Boot-known inputs for a period precision contribution. No connection or mutable
/// factory state is exposed. The descriptor is the actual period executor.
///
/// @param strategy resolved temporal table strategy
/// @param jdbcType actual period JDBC descriptor
/// @param ddlType effective SQL column definition
/// @param precision Dialect-selected fractional precision
/// @param source timestamp generation route
/// @author Steve Ebersole
/// @since 8.1
public record TemporalPeriodPrecisionContext(
		TemporalTableStrategy strategy, JdbcType jdbcType, String ddlType, int precision, Source source) {
	public TemporalPeriodPrecisionContext {
		Objects.requireNonNull( strategy );
		Objects.requireNonNull( jdbcType );
		Objects.requireNonNull( ddlType );
		Objects.requireNonNull( source );
	}

	public enum Source {
		JAVA, FETCHED, INLINE, NATIVE, CHANGELOG
	}
}
