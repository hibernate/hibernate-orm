package org.hibernate.dialect.type.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalTime;

import org.hibernate.Internal;
import org.hibernate.dialect.DB2GetObjectExtractor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

/// Descriptor which exchanges a [LocalTime] with the DB2 JDBC driver directly.
///
/// The driver accepts a `LocalTime` passed to `setObject()` on its own, and
/// rejects one passed together with a SQL type, as [LocalTimeJdbcType] passes
/// it. It also fails with a [NullPointerException] when a null is requested
/// as a `LocalTime`, which [DB2GetObjectExtractor] deals with.
///
/// @author Steve Ebersole
@Internal
public class DB2LocalTimeJdbcType extends LocalTimeJdbcType {
	public static final DB2LocalTimeJdbcType INSTANCE = new DB2LocalTimeJdbcType();

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setObject( index, javaType.unwrap( value, LocalTime.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, javaType.unwrap( value, LocalTime.class, options ) );
			}

			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, Types.TIME );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, Types.TIME );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new DB2GetObjectExtractor<>( javaType, this, LocalTime.class );
	}
}
