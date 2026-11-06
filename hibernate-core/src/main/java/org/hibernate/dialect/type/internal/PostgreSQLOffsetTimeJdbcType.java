package org.hibernate.dialect.type.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.time.LocalTime;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;

import org.hibernate.TemporalValueException;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import java.time.OffsetTime;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType;

/// Specialized OffsetTimeJdbcType implementation for PostgreSQL which uses inferred `setObject` binding,
/// rather than passing the JDBC TIME_WITH_TIMEZONE target type. Reading the server representation
/// avoids typed end-of-day corruption and binary getString offset adjustment.
///
/// @since 8.0
/// @author Steve Ebersole
public final class PostgreSQLOffsetTimeJdbcType extends OffsetTimeJdbcType {
	public static final PostgreSQLOffsetTimeJdbcType INSTANCE = new PostgreSQLOffsetTimeJdbcType();

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
			private OffsetTime checked(X value, WrapperOptions options) {
				final var time = javaType.unwrap( value, OffsetTime.class, options );
				if ( Math.abs( time.getOffset().getTotalSeconds() ) >= 16 * 3600 ) {
					throw new TemporalValueException( "PostgreSQL OffsetTime requires an offset between -15:59:59 and +15:59:59; got " + time.getOffset() );
				}
				return time;
			}

			@Override
			protected void doBind(PreparedStatement statement, X value, int index, WrapperOptions options) throws SQLException {
				statement.setObject( index, checked( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement statement, X value, String name, WrapperOptions options) throws SQLException {
				statement.setObject( name, checked( value, options ) );
			}

			@Override
			protected void doBindNull(PreparedStatement statement, int index, WrapperOptions options) throws SQLException {
				statement.setNull( index, java.sql.Types.OTHER );
			}

			@Override
			protected void doBindNull(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				statement.setNull( name, java.sql.Types.OTHER );
			}
		};
	}
	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			private X wrap(byte[] bytes, WrapperOptions options) {
				if ( bytes == null ) return null;
				// pgJDBC exposes the original server representation through getBytes.
				// Modern PostgreSQL's binary timetz is int64 microseconds plus int32
				// seconds west of UTC. getString in binary mode changes the offset.
				if ( bytes.length == 12 && bytes[0] == 0 ) {
					final var buffer = ByteBuffer.wrap( bytes );
					final long micros = buffer.getLong();
					final var offset = ZoneOffset.ofTotalSeconds( -buffer.getInt() );
					final var time = micros == 86_400_000_000L ? LocalTime.MIDNIGHT
							: LocalTime.ofNanoOfDay( Math.multiplyExact( micros, 1000L ) );
					return javaType.wrap( time.atOffset( offset ), options );
				}
				final String text = new String( bytes, StandardCharsets.US_ASCII );
				final OffsetTime value;
				if ( text.startsWith( "24:00:00" ) ) {
					value = LocalTime.MIDNIGHT.atOffset( ZoneOffset.of( text.substring( 8 ) ) );
				}
				else {
					final int plus = text.indexOf( '+', 1 );
					final int minus = text.indexOf( '-', 1 );
					final int offsetStart = plus < 0 ? minus : plus;
					value = LocalTime.parse( text.substring( 0, offsetStart ) )
							.atOffset( ZoneOffset.of( text.substring( offsetStart ) ) );
				}
				return javaType.wrap( value, options );
			}

			@Override
			protected X doExtract(ResultSet result, int index, WrapperOptions options) throws SQLException {
				return wrap( result.getBytes( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return wrap( statement.getBytes( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return wrap( statement.getBytes( name ), options );
			}
		};
	}

}
