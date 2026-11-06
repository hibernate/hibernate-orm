package org.hibernate.type.descriptor.converter.internal;

import java.util.Objects;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.MappingException;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

/// Mapping-owned timezone/offset adjustment applied on both sides of JDBC handling.
///
/// @see org.hibernate.annotations.TimeZoneAdjustment
///
/// @since 8.0
/// @author Steve Ebersole
public final class TemporalAdjustmentConverter<T> implements BasicValueConverter<T, T> {
	private final JavaType<T> javaType;
	private final ZoneId target;
	private final boolean utcTimeCarrier;
	private final String role;

	public TemporalAdjustmentConverter(JavaType<T> javaType, ZoneId target) {
		this( javaType, target, false );
	}

	public TemporalAdjustmentConverter(JavaType<T> javaType, ZoneId target, boolean utcTimeCarrier) {
		this( javaType, target, utcTimeCarrier, null );
	}

	public TemporalAdjustmentConverter(JavaType<T> javaType, ZoneId target, boolean utcTimeCarrier, String role) {
		final Class<T> type = javaType.getJavaTypeClass();
		if ( type != OffsetDateTime.class && type != ZonedDateTime.class
				&& (type != OffsetTime.class || !(target instanceof ZoneOffset)) ) {
			throw new MappingException( "Timezone adjustment to '" + target + "' is not applicable to " + type.getName() );
		}
		this.javaType = javaType;
		this.target = Objects.requireNonNull( target );
		if ( utcTimeCarrier && type != OffsetTime.class ) {
			throw new IllegalArgumentException( "UTC time encoding requires OffsetTime" );
		}
		this.utcTimeCarrier = utcTimeCarrier;
		this.role = role;
	}

	public boolean usesUtcTimeCarrier() {
		return utcTimeCarrier;
	}

	public ZoneId getTarget() {
		return target;
	}

	@Override
	public T toDomainValue(T relationalForm) {
		return adjust( relationalForm );
	}

	@Override
	public T toRelationalValue(T domainForm) {
		final T adjusted = adjust( domainForm );
		return utcTimeCarrier && adjusted != null
				? javaType.getJavaTypeClass().cast( ((OffsetTime) adjusted).withOffsetSameInstant( ZoneOffset.UTC ) )
				: adjusted;
	}

	private T adjust(T value) {
		if ( value == null ) {
			return null;
		}
		try {
			final Object adjusted;
			if ( value instanceof OffsetTime time ) {
				adjusted = time.withOffsetSameInstant( (ZoneOffset) target );
			}
			else if ( value instanceof OffsetDateTime timestamp ) {
				adjusted = timestamp.atZoneSameInstant( target ).toOffsetDateTime();
			}
			else {
				adjusted = ((ZonedDateTime) value).withZoneSameInstant( target );
			}
			return javaType.getJavaTypeClass().cast( adjusted );
		}
		catch (java.time.DateTimeException ex) {
			throw new org.hibernate.TemporalValueException( "Temporal adjustment for "
					+ (role == null ? javaType.getJavaTypeClass().getName() : role)
					+ " cannot represent the value in target '" + target + "': " + ex.getMessage(), ex );
		}
	}

	@Override
	public JavaType<T> getDomainJavaType() {
		return javaType;
	}

	@Override
	public JavaType<T> getRelationalJavaType() {
		return javaType;
	}
}
