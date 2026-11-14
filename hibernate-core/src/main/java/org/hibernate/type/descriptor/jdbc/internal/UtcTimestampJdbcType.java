package org.hibernate.type.descriptor.jdbc.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;

import java.time.Instant;
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

/// Instant-oriented inline handling for an adjusted mapping. JDBC execution uses
/// the resolved UTC descriptor; literals use the same UTC encoding independently
/// of the requested output zone and the global JDBC timezone.
///
/// @since 8.1
/// @author Steve Ebersole
public final class UtcTimestampJdbcType implements JdbcType {
	private final JdbcType delegate;

	public UtcTimestampJdbcType(JdbcType delegate) {
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
		return delegate.getBinder( javaType );
	}

	@Override
	public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
		return delegate.getExtractor( javaType );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, options) -> dialect.appendDateTimeLiteral(
				appender, delegate.getDdlTypeCode() == java.sql.Types.TIMESTAMP
						? javaType.unwrap( value, Instant.class, options ).atOffset( ZoneOffset.UTC ).toLocalDateTime()
						: javaType.unwrap( value, Instant.class, options ).atOffset( ZoneOffset.UTC ),
				TemporalType.TIMESTAMP, TimeZone.getTimeZone( "UTC" ) );
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof UtcTimestampJdbcType type && type.delegate == delegate;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode( delegate );
	}
}
