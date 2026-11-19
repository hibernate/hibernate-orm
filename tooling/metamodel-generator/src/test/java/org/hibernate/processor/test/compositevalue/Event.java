package org.hibernate.processor.test.compositevalue;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

@Entity
public class Event {
	@Id
	Long id;

	ZonedDateTime basic;

	@CompositeType(DateTimeAndZoneType.class)
	ZonedDateTime composite;

	@ZonedDateTimeType
	ZonedDateTime metaComposite;

	@TimeZoneStorage(TimeZoneStorageType.COLUMN)
	OffsetDateTime column;

	@TimeZoneStorage
	OffsetDateTime auto;

	@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
	OffsetDateTime normalized;

	@Embedded
	Money price;

	@Embedded
	Details details;
}
