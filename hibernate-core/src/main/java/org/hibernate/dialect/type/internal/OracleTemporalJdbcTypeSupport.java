package org.hibernate.dialect.type.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.function.Consumer;

import org.hibernate.TemporalValueException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.internal.SetObjectBinder;

/// Shared Oracle offset-temporal support. Oracle's offset representation loses
/// offset seconds even when the instant survives, so reject them before binding.
///
/// @since 8.1
/// @author Steve Ebersole
final class OracleTemporalJdbcTypeSupport {
	private OracleTemporalJdbcTypeSupport() {
	}

	static void checkOffset(TemporalAccessor value) {
		if ( value != null && value.get( ChronoField.OFFSET_SECONDS ) % 60 != 0 ) {
			throw new TemporalValueException( "Oracle native temporal handling requires a whole-minute offset; got "
					+ value.get( ChronoField.OFFSET_SECONDS ) + " offset seconds. Use COLUMN or instant-oriented adjustment handling" );
		}
	}

	static <T, V> ValueBinder<T> binder(JavaType<T> javaType, JdbcType jdbcType, Class<V> carrier, Consumer<V> check) {
		return new SetObjectBinder<>( javaType, jdbcType, carrier, jdbcType.getDdlTypeCode() ) {
			@Override
			protected Object normalize(T value, WrapperOptions options) {
				final var jdbcValue = javaType.unwrap( value, carrier, options );
				check.accept( jdbcValue );
				return jdbcValue;
			}
		};
	}

	static <T, V> ValueExtractor<T> extractor(JavaType<T> javaType, JdbcType jdbcType, Class<V> carrier, Consumer<V> check) {
		return new BasicExtractor<>( javaType, jdbcType ) {
			private T wrap(V value, WrapperOptions options) {
				check.accept( value );
				org.hibernate.type.descriptor.jdbc.internal.TemporalJdbcValueRanges.check( value, options );
				return javaType.wrap( value, options );
			}

			@Override
			protected T doExtract(ResultSet result, int index, WrapperOptions options) throws SQLException {
				return wrap( result.getObject( index, carrier ), options );
			}

			@Override
			protected T doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return wrap( statement.getObject( index, carrier ), options );
			}

			@Override
			protected T doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return wrap( statement.getObject( name, carrier ), options );
			}
		};
	}
}
