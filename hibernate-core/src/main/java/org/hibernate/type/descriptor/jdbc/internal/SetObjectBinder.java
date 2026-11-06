package org.hibernate.type.descriptor.jdbc.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/// Support for binding values directly through \`setObject\` JDBC driver calls.
///
/// @since 8.0
/// @author Steve Ebersole
public class SetObjectBinder<T> extends BasicBinder<T> {
	private final Class<?> baseClass;
	private final int jdbcTypeCode;

	public SetObjectBinder(
			JavaType<T> javaType,
			JdbcType jdbcType,
			Class<?> baseClass,
			int jdbcTypeCode) {
		super( javaType, jdbcType );
		this.baseClass = baseClass;
		this.jdbcTypeCode = jdbcTypeCode;
	}

	@Override
	protected void doBind(PreparedStatement st, T value, int index, WrapperOptions options) throws SQLException {
		st.setObject( index, checked( normalize( value, options ), options ), jdbcTypeCode );
	}

	protected Object normalize(T value, WrapperOptions options) {
		if ( value instanceof java.time.Instant && TemporalJdbcValueRanges.isTemporalSqlType( jdbcTypeCode ) ) {
			TemporalJdbcValueRanges.check( value, options );
		}
		return getJavaType().unwrap( value, baseClass, options );
	}

	private Object checked(Object value, WrapperOptions options) {
		if ( TemporalJdbcValueRanges.isTemporalSqlType( jdbcTypeCode ) ) TemporalJdbcValueRanges.check( value, options );
		return value;
	}

	@Override
	public Object getBindValue(T value, WrapperOptions options) throws SQLException {
		if ( !TemporalJdbcValueRanges.isTemporalSqlType( jdbcTypeCode ) || !TemporalJdbcValueRanges.isNewHandling( options ) ) return super.getBindValue( value, options );
		return value == null ? null : checked( normalize( value, options ), options );
	}

	@Override
	protected void doBind(CallableStatement st, T value, String name, WrapperOptions options) throws SQLException {
		st.setObject( name, checked( normalize( value, options ), options ), jdbcTypeCode );
	}

	@Override
	protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
		st.setNull( index, jdbcTypeCode );
	}

	@Override
	protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
		st.setNull( name, jdbcTypeCode );
	}
}
