package org.hibernate.dialect.type.internal;

import java.time.OffsetTime;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.OffsetTimeJdbcType;

/// Native Oracle OffsetTime handling with an enforced whole-minute offset domain.
///
/// @since 8.1
/// @author Steve Ebersole
public final class OracleOffsetTimeJdbcType extends OffsetTimeJdbcType {
	public static final OracleOffsetTimeJdbcType INSTANCE = new OracleOffsetTimeJdbcType();

	@Override
	public <T> ValueBinder<T> getBinder(JavaType<T> javaType) {
		return OracleTemporalJdbcTypeSupport.binder(
				javaType,
				this,
				OffsetTime.class,
				OracleTemporalJdbcTypeSupport::checkOffset
		);
	}

	@Override
	public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
		return OracleTemporalJdbcTypeSupport.extractor(
				javaType,
				this,
				OffsetTime.class,
				OracleTemporalJdbcTypeSupport::checkOffset
		);
	}
}
