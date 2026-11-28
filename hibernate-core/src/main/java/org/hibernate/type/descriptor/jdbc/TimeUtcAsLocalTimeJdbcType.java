package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import jakarta.persistence.TemporalType;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/// Descriptor for [TIME_UTC][org.hibernate.type.SqlTypes#TIME_UTC] handling which exchanges the
/// time of day at UTC with the JDBC driver as a [LocalTime].
///
/// It stores what [TimeUtcAsJdbcTimeJdbcType] is meant to store, in the same
/// kind of column. It differs in involving no time zone on the way: that
/// descriptor passes through a [java.sql.Time], and stores a time which
/// depends on the default time zone of the JVM unless that is UTC.
///
/// Used when [temporal preservation][org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION]
/// is enabled, on databases whose driver accepts and returns a `LocalTime`.
///
/// @since 8.1
/// @author Steve Ebersole
public class TimeUtcAsLocalTimeJdbcType extends TimeUtcAsJdbcTimeJdbcType {
	public static final TimeUtcAsLocalTimeJdbcType INSTANCE = new TimeUtcAsLocalTimeJdbcType();

	@Override
	public String toString() {
		return "TimeUtcAsLocalTimeDescriptor";
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		// the literal is the time of day at UTC, as the column is
		return (appender, value, dialect, options) -> dialect.appendDateTimeLiteral(
				appender,
				toUtcTime( javaType.unwrap( value, OffsetTime.class, options ) ),
				TemporalType.TIME,
				TimeZone.getTimeZone( ZoneOffset.UTC )
		);
	}

	/// What is exchanged with the driver is a [LocalTime], and not the
	/// `java.sql.Time` of the descriptor this one extends.
	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return LocalTime.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setObject( index, toUtcTime( javaType.unwrap( value, OffsetTime.class, options ) ) );
			}

			@Override
			public Object getBindValue(X value, WrapperOptions options) throws SQLException {
				// checked as any value which is bound is, and then given as the time at UTC,
				// which is not what unwrapping an OffsetTime as a LocalTime gives
				return super.getBindValue( value, options ) == null
						? null
						: toUtcTime( javaType.unwrap( value, OffsetTime.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, toUtcTime( javaType.unwrap( value, OffsetTime.class, options ) ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaType.wrap( fromUtcTime( getLocalTime( rs, paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaType.wrap( fromUtcTime( statement.getObject( index, LocalTime.class ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaType.wrap( fromUtcTime( statement.getObject( name, LocalTime.class ) ), options );
			}
		};
	}

	/// Read the time of day at UTC from the result. A descriptor for a
	/// driver which needs care in doing so overrides this.
	protected LocalTime getLocalTime(ResultSet rs, int index) throws SQLException {
		return rs.getObject( index, LocalTime.class );
	}

	private static LocalTime toUtcTime(OffsetTime value) {
		return value.withOffsetSameInstant( ZoneOffset.UTC ).toLocalTime();
	}

	private static OffsetTime fromUtcTime(LocalTime utcTime) {
		return utcTime == null ? null : utcTime.atOffset( ZoneOffset.UTC );
	}
}
