package org.hibernate.dialect.type.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.TimeZone;

import jakarta.persistence.TemporalType;

import org.hibernate.Internal;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

/// Descriptor which exchanges a [LocalTime] with the Oracle JDBC driver as a
/// [LocalDateTime] on the first day of 1970.
///
/// Oracle has no type for a time of day, so the value is stored in a
/// `TIMESTAMP` column. A `LocalTime` passed to the driver as such is stored
/// as a different time, which depends on the default time zone of the JVM
/// when the connection was opened. A `LocalDateTime` is stored and returned
/// as it is, whatever that time zone, the default time zone as the value is
/// read, and the time zone of the database session. The date is no more than
/// a carrier: a value read back is the time of whatever date is stored.
///
/// @author Steve Ebersole
@Internal
public class OracleLocalTimeJdbcType extends LocalTimeJdbcType {
	public static final OracleLocalTimeJdbcType INSTANCE = new OracleLocalTimeJdbcType();

	private static final LocalDate ANCHOR = LocalDate.of( 1970, 1, 1 );

	@Override
	@SuppressWarnings("deprecation")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		// a literal for a time of day has no date of its own on Oracle, so
		// it is rendered as the date and time which the column holds
		return (appender, value, dialect, options) -> dialect.getLiteralSupport().appendDateTimeLiteral(
				appender,
				javaType.unwrap( value, LocalTime.class, options ).atDate( ANCHOR ),
				TemporalType.TIMESTAMP,
				options == null || options.getJdbcTimeZone() == null
						? TimeZone.getDefault()
						: options.getJdbcTimeZone()
		);
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setObject( index, javaType.unwrap( value, LocalTime.class, options ).atDate( ANCHOR ), Types.TIMESTAMP );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, javaType.unwrap( value, LocalTime.class, options ).atDate( ANCHOR ), Types.TIMESTAMP );
			}

			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, Types.TIMESTAMP );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, Types.TIMESTAMP );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			private X wrap(LocalDateTime value, WrapperOptions options) {
				return value == null ? null : javaType.wrap( value.toLocalTime(), options );
			}

			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return wrap( rs.getObject( paramIndex, LocalDateTime.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return wrap( statement.getObject( index, LocalDateTime.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return wrap( statement.getObject( name, LocalDateTime.class ), options );
			}
		};
	}
}
