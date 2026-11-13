package org.hibernate.type.descriptor.jdbc.internal;

import java.time.OffsetTime;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.TemporalValueException;
import java.time.ZoneOffset;
import java.util.TimeZone;

import jakarta.persistence.TemporalType;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/// UTC input contract for an adjusted OffsetTime's relational carrier. The
/// mapping converter supplies UTC; extraction normalizes the primary clock to UTC
/// before the converter restores the requested offset. No date is introduced.
///
/// @since 8.0
/// @author Steve Ebersole
public final class UtcTimeJdbcType implements JdbcType {
	private final JdbcType delegate;

	public UtcTimeJdbcType(JdbcType delegate) {
		this.delegate = delegate;
	}

	public JdbcType getDelegate() {
		return delegate;
	}

	@Override
	public int getJdbcTypeCode() {
		return delegate.getJdbcTypeCode();
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return delegate.getDefaultSqlTypeCode();
	}

	@Override
	public int getDdlTypeCode() {
		return delegate.getDdlTypeCode();
	}

	@Override
	public JavaType<?> getRecommendedJavaType(Integer precision, Integer scale, TypeConfiguration configuration) {
		return delegate.getRecommendedJavaType( precision, scale, configuration );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return delegate.getPreferredJavaTypeClass( options );
	}

	@Override
	public void registerOutParameter(CallableStatement statement, int index) throws SQLException {
		delegate.registerOutParameter( statement, index );
	}

	@Override
	public void registerOutParameter(CallableStatement statement, String name) throws SQLException {
		delegate.registerOutParameter( statement, name );
	}

	@Override
	public <T> ValueBinder<T> getBinder(JavaType<T> javaType) {
		final var binder = delegate.getBinder( javaType );
		return new ValueBinder<>() {
			private void check(T value, WrapperOptions options) {
				if ( value != null && !ZoneOffset.UTC.equals( javaType.unwrap( value, OffsetTime.class, options ).getOffset() ) ) {
					throw new TemporalValueException( "UTC time encoding requires a UTC relational value" );
				}
			}

			@Override
			public void bind(PreparedStatement statement, T value, int index, WrapperOptions options) throws SQLException {
				check( value, options );
				binder.bind( statement, value, index, options );
			}

			@Override
			public void bind(CallableStatement statement, T value, String name, WrapperOptions options) throws SQLException {
				check( value, options );
				binder.bind( statement, value, name, options );
			}

			@Override
			public Object getBindValue(T value, WrapperOptions options) throws SQLException {
				check( value, options );
				return binder.getBindValue( value, options );
			}
		};
	}

	@Override
	public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
		final var extractor = delegate.getExtractor( javaType );
		return new ValueExtractor<>() {
			private T utc(T value, WrapperOptions options) {
				return value == null ? null : javaType.wrap(
						javaType.unwrap( value, OffsetTime.class, options ).withOffsetSameInstant( ZoneOffset.UTC ), options );
			}

			@Override
			public T extract(ResultSet result, int index, WrapperOptions options) throws SQLException {
				return utc( extractor.extract( result, index, options ), options );
			}

			@Override
			public T extract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return utc( extractor.extract( statement, index, options ), options );
			}

			@Override
			public T extract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return utc( extractor.extract( statement, name, options ), options );
			}
		};
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, options) -> {
			final var time = javaType.unwrap( value, OffsetTime.class, options );
			if ( !ZoneOffset.UTC.equals( time.getOffset() ) ) {
				throw new TemporalValueException( "UTC time literal encoding requires a UTC relational value" );
			}
			dialect.appendDateTimeLiteral( appender, time, TemporalType.TIME, TimeZone.getTimeZone( "UTC" ) );
		};
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UtcTimeJdbcType type && type.delegate == delegate;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode( delegate );
	}
}
