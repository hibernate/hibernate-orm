/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.BindableType;
import org.hibernate.query.BindingContext;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaTypeHelper;
import org.hibernate.type.descriptor.java.TemporalJavaType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

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

	public <T> BindableType<T> resolveTemporalPrecision(
			TemporalType precision,
			BindableType<T> declaredParameterType,
			BindingContext bindingContext) {
		if ( precision != null ) {
			final SqmExpressible<T> sqmExpressible = declaredParameterType.resolveExpressible(bindingContext);
			if ( !( JavaTypeHelper.isTemporal( sqmExpressible.getExpressibleJavaType() ) ) ) {
				throw new UnsupportedOperationException(
						"Cannot treat non-temporal parameter type with temporal precision"
				);
			}

			final TemporalJavaType<T> temporalJtd = (TemporalJavaType<T>) sqmExpressible.getExpressibleJavaType();
			if ( temporalJtd.getPrecision() != precision ) {
				final TypeConfiguration typeConfiguration = bindingContext.getTypeConfiguration();
				final TemporalJavaType<T> temporalTypeForPrecision;
				// Special case java.util.Date, because TemporalJavaType#resolveTypeForPrecision doesn't support widening,
				// since the main purpose of that method is to determine the final java type based on the reflective type
				// + the explicit @Temporal(TemporalType...) configuration
				if ( java.util.Date.class.isAssignableFrom( temporalJtd.getJavaTypeClass() ) ) {
					//noinspection unchecked
					temporalTypeForPrecision = (TemporalJavaType<T>) typeConfiguration.getJavaTypeRegistry().getDescriptor(
							TemporalJavaType.resolveJavaTypeClass( precision )
					);
				}
				else {
					temporalTypeForPrecision = temporalJtd.resolveTypeForPrecision(
							precision,
							typeConfiguration
					);
				}
				return typeConfiguration.getBasicTypeRegistry().resolve(
						temporalTypeForPrecision,
						TemporalJavaType.resolveJdbcTypeCode( precision )
				);
			}
		}

		return declaredParameterType;
	}

	public JdbcMapping resolveBindType(
			Object value,
			JdbcMapping baseType,
			TypeConfiguration typeConfiguration) {
		if ( value == null || !JavaTypeHelper.isTemporal( baseType.getJdbcJavaType() ) ) {
			return baseType;
		}

		final Class<?> javaType = value.getClass();
		final TemporalType temporalType = ( (TemporalJavaType<?>) baseType.getJdbcJavaType() ).getPrecision();
		switch ( temporalType ) {
			case TIMESTAMP: {
				return (JdbcMapping) resolveTimestampTemporalTypeVariant( javaType, (BindableType<?>) baseType, typeConfiguration );
			}
			case DATE: {
				return (JdbcMapping) resolveDateTemporalTypeVariant( javaType, (BindableType<?>) baseType, typeConfiguration );
			}
			case TIME: {
				return (JdbcMapping) resolveTimeTemporalTypeVariant( javaType, (BindableType<?>) baseType, typeConfiguration );
			}
			default: {
				throw new IllegalArgumentException( "Unexpected TemporalType [" + temporalType + "]; expecting TIMESTAMP, DATE or TIME" );
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public BindableType resolveTimestampTemporalTypeVariant(
			Class javaType,
			BindableType baseType,
			TypeConfiguration typeConfiguration) {
		if ( baseType.getBindableJavaType().isAssignableFrom( javaType ) ) {
			return baseType;
		}

		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.CALENDAR );
		}

		if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIMESTAMP );
		}

		if ( Instant.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INSTANT );
		}

		if ( OffsetDateTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.OFFSET_DATE_TIME );
		}

		if ( ZonedDateTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.ZONED_DATE_TIME );
		}

		if ( OffsetTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.OFFSET_TIME );
		}

		throw new IllegalArgumentException( "Unsure how to handle given Java type [" + javaType.getName() + "] as TemporalType#TIMESTAMP" );
	}

	public BindableType<?> resolveDateTemporalTypeVariant(
			Class<?> javaType,
			BindableType<?> baseType,
			TypeConfiguration typeConfiguration) {
		if ( baseType.getBindableJavaType().isAssignableFrom( javaType ) ) {
			return baseType;
		}

		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.CALENDAR_DATE );
		}

		if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DATE );
		}

		if ( Instant.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INSTANT );
		}

		if ( OffsetDateTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.OFFSET_DATE_TIME );
		}

		if ( ZonedDateTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.ZONED_DATE_TIME );
		}

		throw new IllegalArgumentException( "Unsure how to handle given Java type [" + javaType.getName() + "] as TemporalType#DATE" );
	}

	public BindableType resolveTimeTemporalTypeVariant(
			Class javaType,
			BindableType baseType,
			TypeConfiguration typeConfiguration) {
		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.CALENDAR_TIME );
		}

		if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIME );
		}

		if ( LocalTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.LOCAL_TIME );
		}

		if ( OffsetTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.OFFSET_TIME );
		}

		throw new IllegalArgumentException( "Unsure how to handle given Java type [" + javaType.getName() + "] as TemporalType#TIME" );
	}
}
