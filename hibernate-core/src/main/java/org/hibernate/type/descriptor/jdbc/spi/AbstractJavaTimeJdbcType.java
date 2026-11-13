package org.hibernate.type.descriptor.jdbc.spi;

import java.time.temporal.Temporal;
import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.SPI;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.GetObjectExtractor;
import org.hibernate.type.descriptor.jdbc.internal.SetObjectBinder;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.SPI.Role.IMPLEMENT;

/// Base implementation for JDBC descriptors which exchange a Java time value
/// directly through `ResultSet.getObject()` and `PreparedStatement.setObject()`.
///
/// Extend this class when a JDBC driver natively supports the represented Java
/// time type. Implementations must supply the JDBC and DDL type codes required
/// by [org.hibernate.type.descriptor.jdbc.JdbcType].
///
/// @param <T> the represented Java time type
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(IMPLEMENT)
public abstract class AbstractJavaTimeJdbcType<T extends Temporal> implements JavaTimeJdbcType {
	private final Class<T> javaTimeType;

	@SPI(IMPLEMENT)
	public AbstractJavaTimeJdbcType(Class<T> javaTimeType) {
		this.javaTimeType = javaTimeType;
	}

	@Override
	public Class<T> getPreferredJavaTypeClass(WrapperOptions options) {
		return javaTimeType;
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( javaTimeType );
	}

	@Override
	public void registerOutParameter(CallableStatement statement, int index) throws SQLException {
		statement.registerOutParameter( index, getDdlTypeCode() );
	}

	@Override
	public void registerOutParameter(CallableStatement statement, String name) throws SQLException {
		statement.registerOutParameter( name, getDdlTypeCode() );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new SetObjectBinder<>( javaType, this, javaTimeType, getDdlTypeCode() );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new GetObjectExtractor<>( javaType, this, javaTimeType );
	}
}
