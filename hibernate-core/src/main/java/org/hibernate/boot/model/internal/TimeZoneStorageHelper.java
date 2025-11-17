/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.internal.OffsetDateTimeCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeCompositeUserType;
import org.hibernate.usertype.internal.ZonedDateTimeCompositeUserType;

import static org.hibernate.type.TimeZoneStorageStrategy.COLUMN;
import static org.hibernate.dialect.TimeZoneSupport.NATIVE;

public class TimeZoneStorageHelper {

	private static final String OFFSET_TIME_CLASS = OffsetTime.class.getName();
	private static final String OFFSET_DATETIME_CLASS = OffsetDateTime.class.getName();
	private static final String ZONED_DATETIME_CLASS = ZonedDateTime.class.getName();

	static Class<? extends CompositeUserType<?>> resolveTimeZoneStorageCompositeUserType(
			MemberDetails attributeMember,
			ClassDetails returnedClass,
			MetadataBuildingContext context) {
		if ( useColumnForTimeZoneStorage( attributeMember, context ) ) {
			final String returnedClassName = returnedClass.getName();
			if ( OFFSET_DATETIME_CLASS.equals( returnedClassName ) ) {
				return OffsetDateTimeCompositeUserType.class;
			}
			else if ( ZONED_DATETIME_CLASS.equals( returnedClassName ) ) {
				return ZonedDateTimeCompositeUserType.class;
			}
			else if ( OFFSET_TIME_CLASS.equals( returnedClassName ) ) {
				return OffsetTimeCompositeUserType.class;
			}
		}
		return null;
	}

	private static boolean isTemporalWithTimeZoneClass(String returnedClassName) {
		return OFFSET_DATETIME_CLASS.equals( returnedClassName )
			|| ZONED_DATETIME_CLASS.equals( returnedClassName )
			|| isOffsetTimeClass( returnedClassName );
	}

	public static boolean isOffsetTimeClass(AnnotationTarget element) {
		return element instanceof MemberDetails memberDetails
			&& isOffsetTimeClass( memberDetails );
	}

	public static boolean isOffsetTimeClass(MemberDetails element) {
		final var type = element.getType();
		return type != null
			&& isOffsetTimeClass( type.determineRawClass().getClassName() );

	}

	private static boolean isOffsetTimeClass(String returnedClassName) {
		return OFFSET_TIME_CLASS.equals( returnedClassName );
	}

	static boolean useColumnForTimeZoneStorage(AnnotationTarget element, MetadataBuildingContext context) {
		final var timeZoneStorage = element.getDirectAnnotationUsage( TimeZoneStorage.class );
		if ( timeZoneStorage == null ) {
			return element instanceof MemberDetails attributeMember
				&& isTemporalWithTimeZoneClass( attributeMember.getType().getName() )
				//no @TimeZoneStorage annotation, so we need to use the default storage strategy
				&& context.getBuildingOptions().getDefaultTimeZoneStorage() == COLUMN;
		}
		else {
			return switch ( timeZoneStorage.value() ) {
				case COLUMN -> true;
				// if the db has native support for timezones, we use that, not a column
				case AUTO -> context.getBuildingOptions().getTimeZoneSupport() != NATIVE;
				default -> false;
			};
		}
	}
}
