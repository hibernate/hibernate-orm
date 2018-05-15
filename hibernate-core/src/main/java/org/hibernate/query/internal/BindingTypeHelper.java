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

import org.hibernate.Internal;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.AllowableTemporalParameterType;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@Internal
public class BindingTypeHelper {
	private static final Logger log = Logger.getLogger( BindingTypeHelper.class );

	/**
	 * Singleton access
	 */
	public static final BindingTypeHelper INSTANCE = new BindingTypeHelper();

	private BindingTypeHelper() {
	}

	@SuppressWarnings({"WeakerAccess", "unchecked"})
	public <T> AllowableParameterType<T> resolveTemporalPrecision(
			TemporalType precision,
			AllowableParameterType baseType,
			TypeConfiguration typeConfiguration) {
		if ( ! ( baseType instanceof AllowableTemporalParameterType ) ) {
			throw new UnsupportedOperationException( "Cannot treat non-temporal parameter type with temporal precision" );
		}

		return ( (AllowableTemporalParameterType) baseType ).resolveTemporalPrecision( precision, typeConfiguration );
	}

	public BasicType resolveTimestampTemporalTypeVariant(Class javaType, AllowableParameterType baseType) {
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
	public AllowableParameterType resolveDateTemporalTypeVariant(Class javaType, AllowableParameterType baseType) {
		// prefer to use any Type already known
		if ( baseType != null && baseType instanceof BasicType ) {
			if ( baseType.getJavaTypeDescriptor().getJavaType().isAssignableFrom( javaType ) ) {
				return baseType;
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

	public BasicType resolveTimeTemporalTypeVariant(Class javaType, AllowableParameterType baseType) {
		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.CALENDAR_TIME;
		}

		if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return StandardSpiBasicTypes.TIMESTAMP;
		}

		throw new IllegalArgumentException( "Unsure how to handle given Java type [" + javaType.getName() + "] as TemporalType#TIME" );
	}
}
