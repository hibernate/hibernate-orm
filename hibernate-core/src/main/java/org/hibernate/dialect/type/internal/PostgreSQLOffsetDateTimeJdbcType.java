package org.hibernate.dialect.type.internal;

import java.time.OffsetDateTime;

import org.hibernate.TemporalValueException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.SetObjectBinder;

/// Native PostgreSQL timestamp handling within its accepted offset range.
/// The database normalizes to UTC; this descriptor does not retain input offsets.
///
/// @since 8.1
/// @author Steve Ebersole
public final class PostgreSQLOffsetDateTimeJdbcType extends OffsetDateTimeJdbcType {
	public static final PostgreSQLOffsetDateTimeJdbcType INSTANCE = new PostgreSQLOffsetDateTimeJdbcType();

	@Override
	public <T> ValueBinder<T> getBinder(JavaType<T> javaType) {
		return new SetObjectBinder<>( javaType, this, OffsetDateTime.class, getDdlTypeCode() ) {
			@Override
			protected Object normalize(T value, WrapperOptions options) {
				final var timestamp = javaType.unwrap( value, OffsetDateTime.class, options );
				if ( Math.abs( timestamp.getOffset().getTotalSeconds() ) >= 16 * 3600 ) {
					throw new TemporalValueException( "PostgreSQL native timestamp handling requires an offset between -15:59:59 and +15:59:59; got "
							+ timestamp.getOffset() );
				}
				return timestamp;
			}
		};
	}
}
