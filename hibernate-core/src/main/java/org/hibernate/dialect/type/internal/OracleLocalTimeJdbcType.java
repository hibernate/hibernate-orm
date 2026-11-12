package org.hibernate.dialect.type.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.internal.SetObjectBinder;

/// Oracle LocalTime handling through zone-less LocalDateTime fields. The anchor
/// date is only a JDBC carrier field: no instant or timezone is resolved. Oracle's
/// LocalTime path can shift fields with differing client/session timezones and
/// discards fractions; TIMESTAMP handling avoids both losses.
///
/// @since 8.1
/// @author Steve Ebersole
public final class OracleLocalTimeJdbcType extends LocalTimeJdbcType {
	public static final OracleLocalTimeJdbcType INSTANCE = new OracleLocalTimeJdbcType();
	private static final LocalDate ANCHOR = LocalDate.of( 1970, 1, 1 );

	@Override
	public int getDdlTypeCode() {
		return Types.TIMESTAMP;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, options) -> dialect.getLiteralSupport().appendDateTimeLiteral(
				appender,
				javaType.unwrap( value, LocalTime.class, options ).atDate( ANCHOR ),
				jakarta.persistence.TemporalType.TIMESTAMP,
				java.util.TimeZone.getTimeZone( "UTC" )
		);
	}

	@Override
	public <T> ValueBinder<T> getBinder(JavaType<T> javaType) {
		return new SetObjectBinder<>( javaType, this, LocalDateTime.class, Types.TIMESTAMP ) {
			@Override
			protected Object normalize(T value, WrapperOptions options) {
				return javaType.unwrap( value, LocalTime.class, options ).atDate( ANCHOR );
			}
		};
	}

	@Override
	public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			private T wrap(LocalDateTime value, WrapperOptions options) {
				return javaType.wrap( value == null ? null : value.toLocalTime(), options );
			}

			@Override
			protected T doExtract(ResultSet result, int index, WrapperOptions options) throws SQLException {
				return wrap( result.getObject( index, LocalDateTime.class ), options );
			}

			@Override
			protected T doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return wrap( statement.getObject( index, LocalDateTime.class ), options );
			}

			@Override
			protected T doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return wrap( statement.getObject( name, LocalDateTime.class ), options );
			}
		};
	}
}
