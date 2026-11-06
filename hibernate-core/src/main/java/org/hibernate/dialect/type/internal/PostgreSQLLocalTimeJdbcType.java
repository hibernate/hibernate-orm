package org.hibernate.dialect.type.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;

import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

/// pgJDBC represents PostgreSQL's 24:00 endpoint as LocalTime.MAX. Reconstruct
/// midnight when precision rounding reaches that endpoint, consistently with
/// time-only modulo-day semantics. PostgreSQL's maximum stored precision is
/// microseconds, so a legitimate stored 23:59:59.999999 is not this sentinel.
///
/// @since 8.0
/// @author Steve Ebersole
public final class PostgreSQLLocalTimeJdbcType extends LocalTimeJdbcType {
	public static final PostgreSQLLocalTimeJdbcType INSTANCE = new PostgreSQLLocalTimeJdbcType();

	@Override
	public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			private T wrap(LocalTime value, WrapperOptions options) {
				return javaType.wrap( LocalTime.MAX.equals( value ) ? LocalTime.MIDNIGHT : value, options );
			}
			@Override protected T doExtract(ResultSet result, int index, WrapperOptions options) throws SQLException {
				return wrap( result.getObject( index, LocalTime.class ), options );
			}
			@Override protected T doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return wrap( statement.getObject( index, LocalTime.class ), options );
			}
			@Override protected T doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return wrap( statement.getObject( name, LocalTime.class ), options );
			}
		};
	}
}
