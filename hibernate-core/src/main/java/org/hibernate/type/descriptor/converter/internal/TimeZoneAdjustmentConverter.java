package org.hibernate.type.descriptor.converter.internal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.persistence.AttributeConverter;

import org.hibernate.Internal;
import org.hibernate.TemporalPreservationException;

import static org.hibernate.cfg.MappingSettings.TEMPORAL_PRESERVATION;

/// Applies the zone declared with [org.hibernate.annotations.TimeZoneAdjustment]
/// to the values of an attribute: every value is read in the declared zone,
/// and a value written in another zone or offset is either rejected or
/// converted to the declared zone, keeping its instant.
///
/// @param <T> `OffsetDateTime` or `ZonedDateTime`
///
/// @since 8.1
/// @author Steve Ebersole
@Internal
public abstract sealed class TimeZoneAdjustmentConverter<T> implements AttributeConverter<T, T> {
	private final ZoneId zone;
	private final boolean strict;
	private final String role;

	/// @param zone the declared zone
	/// @param strict whether a value in another zone or offset is rejected, as opposed to converted
	/// @param role the mapping, for messages
	private TimeZoneAdjustmentConverter(ZoneId zone, boolean strict, String role) {
		this.zone = zone;
		this.strict = strict;
		this.role = role;
	}

	/// The converter for values of the given class, which must be
	/// `OffsetDateTime` or `ZonedDateTime`.
	public static TimeZoneAdjustmentConverter<?> forClass(Class<?> javaClass, ZoneId zone, boolean strict, String role) {
		if ( javaClass == OffsetDateTime.class ) {
			return new ForOffsetDateTime( zone, strict, role );
		}
		else if ( javaClass == ZonedDateTime.class ) {
			return new ForZonedDateTime( zone, strict, role );
		}
		else {
			throw new IllegalArgumentException( "Not an OffsetDateTime or ZonedDateTime: " + javaClass.getName() );
		}
	}

	public ZoneId getZone() {
		return zone;
	}

	public boolean isStrict() {
		return strict;
	}

	@Override
	public T convertToDatabaseColumn(T value) {
		if ( value == null ) {
			return null;
		}
		if ( isInZone( value ) ) {
			return value;
		}
		if ( strict ) {
			throw new TemporalPreservationException(
					"Cannot write the value '" + value + "' for '" + role + "': with '" + TEMPORAL_PRESERVATION
					+ "' enabled, the zone of the mapping is declared to be '" + zone + "' by @TimeZoneAdjustment,"
					+ " and the VALUE policy does not allow the zone or offset of the value to be lost; write"
					+ " the value in the declared zone, or select the NORMALIZED policy"
			);
		}
		return inZone( value );
	}

	@Override
	public T convertToEntityAttribute(T value) {
		return value == null || isInZone( value ) ? value : inZone( value );
	}

	abstract boolean isInZone(T value);

	abstract T inZone(T value);

	public static final class ForOffsetDateTime extends TimeZoneAdjustmentConverter<OffsetDateTime> {
		private ForOffsetDateTime(ZoneId zone, boolean strict, String role) {
			super( zone, strict, role );
		}

		@Override
		boolean isInZone(OffsetDateTime value) {
			final Instant instant = value.toInstant();
			return value.getOffset().equals( getZone().getRules().getOffset( instant ) );
		}

		@Override
		OffsetDateTime inZone(OffsetDateTime value) {
			return value.atZoneSameInstant( getZone() ).toOffsetDateTime();
		}
	}

	public static final class ForZonedDateTime extends TimeZoneAdjustmentConverter<ZonedDateTime> {
		private ForZonedDateTime(ZoneId zone, boolean strict, String role) {
			super( zone, strict, role );
		}

		@Override
		boolean isInZone(ZonedDateTime value) {
			// 'UTC' and 'Z' are different identifiers of the same zone
			return value.getZone().equals( getZone() )
				|| value.getZone().normalized().equals( getZone().normalized() );
		}

		@Override
		ZonedDateTime inZone(ZonedDateTime value) {
			return value.withZoneSameInstant( getZone() );
		}
	}
}
