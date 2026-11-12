package org.hibernate.dialect.temporal.internal;

import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionContext;
import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionSupport;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

/// H2 emulated periods encode Instant boundaries as UTC clock fields.
///
/// @author Steve Ebersole
public class H2TemporalTableSupport extends DefaultTemporalTableSupport {
	public H2TemporalTableSupport(H2Dialect dialect) { super( dialect ); }

	@Override
	public JdbcType getPreservingTemporalJdbcType(TemporalTableStrategy strategy, JdbcTypeRegistry registry) {
		if ( strategy == TemporalTableStrategy.NATIVE ) { return null; }
		final var descriptor = registry.getDescriptor( getTemporalColumnType() );
		return descriptor.getClass() == TimestampJdbcType.class
				? TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE : descriptor;
	}

	@Override
	public String getPreservingTemporalCurrentTimestampExpression(TemporalTableStrategy strategy, JdbcType jdbcType) {
		return strategy == TemporalTableStrategy.NATIVE ? null
				: org.hibernate.dialect.temporaltype.internal.PreservingCurrentTimestamp.expression(
						dialect, org.hibernate.type.descriptor.java.InstantJavaType.INSTANCE, jdbcType );
	}
	@Override
	public TemporalPeriodPrecisionSupport getTemporalPeriodPrecisionSupport(TemporalPeriodPrecisionContext context) {
		return StandardPeriodPrecision.resolve( dialect, context );
	}

}
