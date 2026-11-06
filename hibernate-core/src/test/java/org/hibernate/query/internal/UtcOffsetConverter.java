package org.hibernate.query.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Stores an {@link OffsetDateTime} as a plain wall clock, i.e. the column carries no offset.
 * <p>
 * Nothing about this converter is unusual, and nothing about it is required to trigger the bug: any
 * {@code AttributeConverter} whose relational type is not one of the types handled by
 * {@code BindingTypeHelper.resolveTimestampTemporalTypeVariant} will do.
 */
@Converter
class UtcOffsetConverter implements AttributeConverter<OffsetDateTime, LocalDateTime> {

	private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

	@Override
	public LocalDateTime convertToDatabaseColumn(OffsetDateTime offsetDateTime) {
		return offsetDateTime == null ? null : offsetDateTime.atZoneSameInstant(ZONE).toLocalDateTime();
	}

	@Override
	public OffsetDateTime convertToEntityAttribute(LocalDateTime wallClock) {
		return wallClock == null ? null : wallClock.atZone(ZONE).toOffsetDateTime();
	}

}
