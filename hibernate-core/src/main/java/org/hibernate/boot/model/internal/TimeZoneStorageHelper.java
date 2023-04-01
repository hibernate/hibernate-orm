/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.internal.OffsetDateTimeCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeCompositeUserType;
import org.hibernate.usertype.internal.ZonedDateTimeCompositeUserType;

import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import static org.hibernate.TimeZoneStorageStrategy.COLUMN;
import static org.hibernate.dialect.TimeZoneSupport.NATIVE;

public class TimeZoneStorageHelper {

	private static final String OFFSET_TIME_CLASS = OffsetTime.class.getName();
	private static final String OFFSET_DATETIME_CLASS = OffsetDateTime.class.getName();
	private static final String ZONED_DATETIME_CLASS = ZonedDateTime.class.getName();

	static Class<? extends CompositeUserType<?>> resolveTimeZoneStorageCompositeUserType(
			XProperty property,
			XClass returnedClass,
			MetadataBuildingContext context) {
		if ( useColumnForTimeZoneStorage( property, context ) ) {
			String returnedClassName = returnedClass.getName();
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

	public static boolean isOffsetTimeClass(XAnnotatedElement element) {
		if ( element instanceof XProperty ) {
			XProperty property = (XProperty) element;
			return isOffsetTimeClass( property.getType().getName() );
		}
		return false;
	}

	private static boolean isOffsetTimeClass(String returnedClassName) {
		return OFFSET_TIME_CLASS.equals( returnedClassName );
	}

	static boolean useColumnForTimeZoneStorage(XAnnotatedElement element, MetadataBuildingContext context) {
		final TimeZoneStorage timeZoneStorage = element.getAnnotation( TimeZoneStorage.class );
		if ( timeZoneStorage == null ) {
			if ( element instanceof XProperty ) {
				XProperty property = (XProperty) element;
				return isTemporalWithTimeZoneClass( property.getType().getName() )
						//no @TimeZoneStorage annotation, so we need to use the default storage strategy
						&& context.getBuildingOptions().getDefaultTimeZoneStorage() == COLUMN;
			}
			else {
				return false;
			}
		}
		else {
			switch ( timeZoneStorage.value() ) {
				case COLUMN:
					return true;
				case AUTO:
					// if the db has native support for timezones, we use that, not a column
					return context.getBuildingOptions().getTimeZoneSupport() != NATIVE;
				default:
					return false;
			}
		}
	}
}
