/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.internal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import javax.persistence.TemporalType;

import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class BindingTypeHelper {
	/**
	 * Singleton access
	 */
	public static final BindingTypeHelper INSTANCE = new BindingTypeHelper();

	private BindingTypeHelper() {
	}

	public BasicType determineTypeForTemporalType(TemporalType temporalType, Type baseType, Object bindValue) {
		// todo : for 6.0 make TemporalType part of org.hibernate.type.descriptor.java.JdbcRecommendedSqlTypeMappingContext
		//		then we can just ask the org.hibernate.type.basic.BasicTypeFactory to handle this based on its registry
		//
		//   - or for 6.0 make TemporalType part of the state for those BasicType impls dealing with date/time types
		//
		// 	 - or for 6.0 make TemporalType part of Binder contract
		//
		//   - or add a org.hibernate.type.TemporalType#getVariant(TemporalType)
		//
		//	 - or ...

		// todo : (5.2) review Java type handling for sanity.  This part was done quickly ;)

		final Class javaType;

		// Determine the "value java type" :
		// 		prefer to leverage the bindValue java type (if bindValue not null),
		// 		followed by the java type reported by the baseType,
		// 		fallback to java.sql.Timestamp

		if ( bindValue != null ) {
			javaType = bindValue.getClass();
		}
		else if ( baseType != null ) {
			javaType = baseType.getReturnedClass();
		}
		else {
			javaType = java.sql.Timestamp.class;
		}

		switch ( temporalType ) {
			case TIMESTAMP: {
				return resolveTimestampTemporalTypeVariant( javaType, baseType );
			}
			case DATE: {
				return resolveDateTemporalTypeVariant( javaType, baseType );
			}
			case TIME: {
				return resolveTimeTemporalTypeVariant( javaType, baseType );
			}
			default: {
				throw new IllegalArgumentException( "Unexpected TemporalType [" + temporalType + "]; expecting TIMESTAMP, DATE or TIME" );
			}
		}
	}

	public BasicType resolveTimestampTemporalTypeVariant(Class javaType, Type baseType) {
		// prefer to use any Type already known - interprets TIMESTAMP as "no narrowing"
		if ( baseType != null && baseType instanceof BasicType ) {
			return (BasicType) baseType;
		}

		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.CALENDAR;
		}

		if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.TIMESTAMP;
		}

		if ( Instant.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.INSTANT;
		}

		if ( OffsetDateTime.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.OFFSET_DATE_TIME;
		}

		if ( ZonedDateTime.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.ZONED_DATE_TIME;
		}

		if ( OffsetTime.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.OFFSET_TIME;
		}

		throw new IllegalArgumentException( "Unsure how to handle given Java type [" + javaType.getName() + "] as TemporalType#TIMESTAMP" );
	}

	@SuppressWarnings("unchecked")
	public BasicType resolveDateTemporalTypeVariant(Class javaType, Type baseType) {
		// prefer to use any Type already known
		if ( baseType != null && baseType instanceof BasicType ) {
			if ( baseType.getReturnedClass().isAssignableFrom( javaType ) ) {
				return (BasicType) baseType;
			}
		}

		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.CALENDAR_DATE;
		}

		if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.TIMESTAMP;
		}

		if ( Instant.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.OFFSET_DATE_TIME;
		}

		if ( OffsetDateTime.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.OFFSET_DATE_TIME;
		}

		if ( ZonedDateTime.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.ZONED_DATE_TIME;
		}

		throw new IllegalArgumentException( "Unsure how to handle given Java type [" + javaType.getName() + "] as TemporalType#DATE" );
	}

	public BasicType resolveTimeTemporalTypeVariant(Class javaType, Type baseType) {
		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.CALENDAR_TIME;
		}

		if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.TIMESTAMP;
		}

		throw new IllegalArgumentException( "Unsure how to handle given Java type [" + javaType.getName() + "] as TemporalType#TIME" );
	}
}
