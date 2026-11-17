package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for {@link SqlTypes#TIME_UTC TIME_UTC} handling.
 *
 * @author Christian Beikov
 */
public class TimeUtcAsJdbcTimeJdbcType implements JdbcType {

	public static final TimeUtcAsJdbcTimeJdbcType INSTANCE = new TimeUtcAsJdbcTimeJdbcType();

	public TimeUtcAsJdbcTimeJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIME;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.TIME_UTC;
	}

	@Override
	public String getFriendlyName() {
		return "TIME_UTC";
	}

	@Override
	public String toString() {
		return "TimeUtcDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( OffsetTime.class );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Time.class;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIME );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setTime( index, getBindValue( value, options ), Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setTime( name, getBindValue( value, options ), Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
			}

			@Override
			public Time getBindValue(X value, WrapperOptions options) throws SQLException {
				final OffsetTime offsetTime = getJavaType().unwrap( value, OffsetTime.class, options );
				return new Time( offsetTime.atDate( LocalDate.EPOCH ).toEpochSecond() * 1000 + offsetTime.getNano() / 1_000_000 );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {

			private static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;
			private static final int MAX_OFFSET_SECONDS = 18 * 60 * 60;

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				final Time time = rs.getTime( paramIndex, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
				return javaType.wrap( toOffsetTime( time ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				final Time time = statement.getTime( index, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
				return javaType.wrap( toOffsetTime( time ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				final Time time = statement.getTime( name, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
				return javaType.wrap( toOffsetTime( time ), options );
			}

			private OffsetTime toOffsetTime(Time time) {
				if ( time == null ) {
					return null;
				}
				final long epochMillis = time.getTime();
				final ZoneOffset offset;
				if ( epochMillis < 0 ) {
					offset = ZoneOffset.ofTotalSeconds( MAX_OFFSET_SECONDS );
				}
				else if ( epochMillis >= DAY_IN_MILLIS ) {
					offset = ZoneOffset.ofTotalSeconds( -MAX_OFFSET_SECONDS );
				}
				else {
					offset = ZoneOffset.UTC;
				}
				return Instant.ofEpochMilli( epochMillis )
						.atOffset( offset )
						.toOffsetTime();
			}
		};
	}
}
