package org.hibernate.dialect.type.internal;

import org.hibernate.TemporalValueException;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsJdbcTimeJdbcType;

/// Specialized TimeUtcAsJdbcTimeJdbcType implementation for PostgreSQL.
/// UTC time-only representation using driver LocalTime handling, avoiding
/// java.sql.Time's fractional loss and JVM-default-zone dependence. This
/// descriptor accepts UTC-adjusted inputs only and is contributed only to the
/// direct-enabled path. Mapping adjustment or COLUMN derivation supplies UTC.
///
/// @since 8.0
/// @author Steve Ebersole
public final class PostgreSQLUtcTimeJdbcType extends TimeUtcAsJdbcTimeJdbcType {
	public static final PostgreSQLUtcTimeJdbcType INSTANCE = new PostgreSQLUtcTimeJdbcType();

	@Override
	public void registerOutParameter(CallableStatement statement, int index) {
		throw new org.hibernate.MappingException( "PostgreSQL temporal OUT/INOUT parameter " + index
				+ " cannot be extracted with preservation by this driver path; use a supported result-set mapping" );
	}

	@Override
	public void registerOutParameter(CallableStatement statement, String name) {
		throw new org.hibernate.MappingException( "PostgreSQL temporal OUT/INOUT parameter '" + name
				+ "' cannot be extracted with preservation by this driver path; use a supported result-set mapping" );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			private LocalTime utc(X value, WrapperOptions options) {
				final var time = javaType.unwrap( value, OffsetTime.class, options );
				if ( !ZoneOffset.UTC.equals( time.getOffset() ) ) {
					throw new TemporalValueException( "PostgreSQL UTC time handling requires a UTC-adjusted input" );
				}
				return time.toLocalTime();
			}

			@Override
			protected void doBind(PreparedStatement statement, X value, int index, WrapperOptions options) throws SQLException {
				statement.setObject( index, utc( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement statement, X value, String name, WrapperOptions options) throws SQLException {
				statement.setObject( name, utc( value, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			private X wrap(LocalTime time, WrapperOptions options) {
				return time == null ? null : javaType.wrap( (time.equals( LocalTime.MAX ) ? LocalTime.MIDNIGHT : time).atOffset( ZoneOffset.UTC ), options );
			}

			@Override
			protected X doExtract(ResultSet result, int index, WrapperOptions options) throws SQLException {
				return wrap( result.getObject( index, LocalTime.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return wrap( statement.getObject( index, LocalTime.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return wrap( statement.getObject( name, LocalTime.class ), options );
			}
		};
	}
}
