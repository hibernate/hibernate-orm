package org.hibernate.dialect.type.internal;

import java.time.OffsetDateTime;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;

/// Native Oracle OffsetDateTime handling with an enforced whole-minute offset domain.
///
/// @since 8.0
/// @author Steve Ebersole
public final class OracleOffsetDateTimeJdbcType extends OffsetDateTimeJdbcType {
	public static final OracleOffsetDateTimeJdbcType INSTANCE = new OracleOffsetDateTimeJdbcType();

	@Override
	public <T> ValueBinder<T> getBinder(JavaType<T> javaType) {
		return OracleTemporalJdbcTypeSupport.binder(
				javaType,
				this,
				OffsetDateTime.class,
				OracleTemporalJdbcTypeSupport::checkOffset
		);
	}

	@Override
	public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
		return OracleTemporalJdbcTypeSupport.extractor(
				javaType,
				this,
				OffsetDateTime.class,
				OracleTemporalJdbcTypeSupport::checkOffset
		);
	}
}
