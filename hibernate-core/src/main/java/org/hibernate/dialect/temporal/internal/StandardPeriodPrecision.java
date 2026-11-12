package org.hibernate.dialect.temporal.internal;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionContext;
import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionSupport;
import org.hibernate.dialect.type.internal.PostgreSQLTimestampUtcAsJdbcTimestampJdbcType;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;

/// Exact stock period routes. This is independent of ordinary attribute precision
/// and makes no assertion about an unassessed descriptor or native producer.
///
/// @author Steve Ebersole
public final class StandardPeriodPrecision {
	private StandardPeriodPrecision() {
	}

	public static TemporalPeriodPrecisionSupport resolve(Dialect dialect, TemporalPeriodPrecisionContext context) {
		if ( context.strategy() == TemporalTableStrategy.NATIVE ) {
			return TemporalPeriodPrecisionSupport.unsupported( "Native period precision requires an independently established producer contract" );
		}
		final int precision = context.precision();
		final Class<?> jdbc = context.jdbcType().getClass();
		final boolean plain = jdbc == TimestampUtcAsPlainJdbcTimestampJdbcType.class;
		final boolean postgres = jdbc == PostgreSQLTimestampUtcAsJdbcTimestampJdbcType.class;
		final boolean h2Route = dialect instanceof H2Dialect && plain;
		final boolean postgresRoute = dialect instanceof PostgreSQLDialect && postgres;
		final boolean oracleRoute = dialect instanceof OracleDialect && plain;
		if ( !h2Route && !postgresRoute && !oracleRoute ) {
			return TemporalPeriodPrecisionSupport.unsupported( "Unassessed period descriptor/storage route" );
		}
		final int maximum = postgresRoute ? 6 : 9;
		if ( precision < 0 || precision > maximum ) {
			return TemporalPeriodPrecisionSupport.unsupported( "Period precision must be in 0.." + maximum );
		}
		final int stockPrecision = oracleRoute ? 9 : 6;
		if ( precision != stockPrecision ) {
			return TemporalPeriodPrecisionSupport.unsupported(
					"Changing the stock period precision requires an explicit storage/producer precision contribution" );
		}
		final String expected = "timestamp(" + precision + ")" + (postgresRoute ? " with time zone" : "");
		if ( !context.ddlType().toLowerCase( Locale.ROOT ).trim().equals( expected ) ) {
			return TemporalPeriodPrecisionSupport.unsupported( "Period column definition does not match the certified grid: " + expected );
		}
		String expression = null;
		if ( context.source() == TemporalPeriodPrecisionContext.Source.INLINE ) {
			expression = dialect.getTemporalTableSupport().getPreservingTemporalCurrentTimestampExpression(
					context.strategy(), context.jdbcType() );
			if ( expression == null ) {
				return TemporalPeriodPrecisionSupport.unsupported( "No transaction-stable period producer for this encoding" );
			}
			expression = "cast(" + expression + " as " + expected + ")";
		}
		final var adjustment = dialect.getTemporalValueSemantics().precisionAdjustment();
		return TemporalPeriodPrecisionSupport.supported(
				"period-v1:" + jdbc.getName() + ':' + expected + ':' + adjustment + ':' + context.source(),
				value -> switch ( adjustment ) {
					case ROUND -> DateTimeUtils.roundToSecondPrecision( value, precision );
					case TRUNCATE -> DateTimeUtils.truncateToPrecision( value, precision );
					case ROUND_MAX -> DateTimeUtils.roundToSecondPrecision(
							DateTimeUtils.roundToSecondPrecision( value, maximum ), precision );
				}, expression );
	}
}
