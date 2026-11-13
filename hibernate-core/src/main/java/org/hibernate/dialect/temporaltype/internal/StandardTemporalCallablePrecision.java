package org.hibernate.dialect.temporaltype.internal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.ParameterMode;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext.Access;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.internal.UtcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;

/// Exact callable routes proved using H2 2.5.252. No attribute precision profile
/// or ORM parameter name substitutes for actual callable transport evidence.
///
/// @author Steve Ebersole
public final class StandardTemporalCallablePrecision {
	private StandardTemporalCallablePrecision() {}

	public static TemporalPrecisionSupport resolve(TemporalCallablePrecisionContext context) {
		if ( context.dialect().getClass() != H2Dialect.class || context.dialect().getVersion().isBefore( 2, 5 ) ) {
			return TemporalPrecisionSupport.unsupported( "No established callable precision profile for the configured Dialect/version" );
		}
		if ( context.valueAccess() == Access.NAMED || context.registrationAccess() == Access.NAMED
				|| context.mode() == ParameterMode.INOUT || context.mode() == ParameterMode.OUT && !context.functionReturn() ) {
			return TemporalPrecisionSupport.unsupported( "Unassessed H2 callable mode/access route" );
		}
		final var java = context.type().getJdbcJavaType().getClass();
		final var jdbc = context.type().getJdbcType().getClass();
		if ( java == LocalDateTimeJavaType.class && jdbc == LocalDateTimeJdbcType.class
				|| java == InstantJavaType.class && jdbc == TimestampUtcAsPlainJdbcTimestampJdbcType.class
				|| java == OffsetDateTimeJavaType.class && jdbc == UtcTimestampJdbcType.class
						&& ((UtcTimestampJdbcType) context.type().getJdbcType()).getDelegate().getClass() == TimestampUtcAsPlainJdbcTimestampJdbcType.class
						&& context.adjustment() != null ) {
			return TemporalPrecisionSupport.supported( "h2-callable-v1:nanos9:" + java.getName() + ':' + jdbc.getName()
					+ ':' + context.mode() + ':' + context.boundary() + ':' + context.valueAccess()
					+ ':' + context.registrationAccess(), StandardTemporalCallablePrecision::finite,
					value -> value instanceof OffsetDateTime timestamp ? timestamp.withOffsetSameInstant( ZoneOffset.UTC ) : value );
		}
		return TemporalPrecisionSupport.unsupported( "Unassessed actual H2 callable Java/JDBC pair" );
	}
	private static Object finite(Object value) {
		final Instant instant = value instanceof Instant timestamp ? timestamp
				: value instanceof OffsetDateTime timestamp ? timestamp.toInstant() : null;
		if ( instant != null ) {
			// These exact timestamp carriers pass through java.sql.Timestamp.
			Math.addExact( Math.multiplyExact( instant.getEpochSecond(), 1000 ), instant.getNano() / 1_000_000 );
			instant.atOffset( ZoneOffset.UTC );
		}
		return value;
	}
}
