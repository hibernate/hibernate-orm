package org.hibernate.processor.test.compositevalue;

import java.time.ZonedDateTime;

import jakarta.persistence.Embeddable;

import org.hibernate.annotations.CompositeType;

@Embeddable
public class Details {
	String note;

	@CompositeType(DateTimeAndZoneType.class)
	ZonedDateTime shippedAt;
}
