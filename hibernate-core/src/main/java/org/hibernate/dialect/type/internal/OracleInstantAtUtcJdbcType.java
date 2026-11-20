package org.hibernate.dialect.type.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import jakarta.persistence.TemporalType;

import org.hibernate.Internal;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;

/// Descriptor which holds an [Instant] on Oracle as its date and time at
/// UTC, in a `TIMESTAMP` column which knows of no time zone, and exchanges
/// it with the Oracle JDBC driver as a [LocalDateTime].
///
/// Oracle ordinarily stores an `Instant` in a `TIMESTAMP WITH TIME ZONE`
/// column, which it does not allow in a primary key or a unique constraint.
/// This descriptor is for the columns which must be part of one: the row
/// start of a temporal table, and a changeset of an audit table which is an
/// `Instant`. It is used only with temporal preservation enabled.
///
/// A `LocalDateTime` passes through the driver unchanged, whatever the
/// default time zone of the JVM and the time zone of the database session,
/// whereas a `java.sql.Timestamp` passes through one or the other.
///
/// @since 8.1
/// @author Steve Ebersole
@Internal
public class OracleInstantAtUtcJdbcType extends TimestampUtcAsJdbcTimestampJdbcType {
	public static final OracleInstantAtUtcJdbcType INSTANCE = new OracleInstantAtUtcJdbcType();

	/// The column is a plain `TIMESTAMP`, and not the `TIMESTAMP WITH TIME ZONE`
	/// which Oracle has for [SqlTypes#TIMESTAMP_UTC].
	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.TIMESTAMP;
	}

	@Override
	public String toString() {
		return "OracleInstantAtUtcDescriptor";
	}

	private static LocalDateTime atUtc(Instant instant) {
		return LocalDateTime.ofInstant( instant, ZoneOffset.UTC );
	}

	@Override
	@SuppressWarnings("deprecation")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		// a literal with a time zone would be compared with the column in the
		// time zone of the session, so it is rendered as the date and time at
		// UTC which the column holds
		return (appender, value, dialect, options) -> dialect.getLiteralSupport().appendDateTimeLiteral(
				appender,
				atUtc( javaType.unwrap( value, Instant.class, options ) ),
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
				st.setObject( index, atUtc( javaType.unwrap( value, Instant.class, options ) ), Types.TIMESTAMP );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, atUtc( javaType.unwrap( value, Instant.class, options ) ), Types.TIMESTAMP );
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
				return value == null ? null : javaType.wrap( value.toInstant( ZoneOffset.UTC ), options );
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
