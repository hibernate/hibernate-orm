/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.BindableType;
import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JavaTypeHelper;
import org.hibernate.type.descriptor.java.TemporalJavaType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.type.descriptor.java.TemporalJavaType.resolveJavaTypeClass;
import static org.hibernate.type.descriptor.java.TemporalJavaType.resolveJdbcTypeCode;

/**
 * @author Steve Ebersole
 */
public class BindingTypeHelper {
	private BindingTypeHelper() {
	}

	public static <T> BindableType<T> resolveTemporalPrecision(
			TemporalType precision,
			BindableType<T> declaredParameterType,
			BindingContext bindingContext) {
		if ( precision != null ) {
			final TemporalJavaType<T> temporalJtd = getTemporalJavaType( declaredParameterType, bindingContext );
			if ( temporalJtd == null || temporalJtd.getPrecision() != precision ) {
				final TypeConfiguration typeConfiguration = bindingContext.getTypeConfiguration();
				final TemporalJavaType<T> temporalTypeForPrecision =
						getTemporalTypeForPrecision( precision, temporalJtd, typeConfiguration );
				return typeConfiguration.getBasicTypeRegistry()
						.resolve( temporalTypeForPrecision, resolveJdbcTypeCode( precision ) );
			}
		}
		return declaredParameterType;
	}

	private static <T> TemporalJavaType<T> getTemporalTypeForPrecision(
			TemporalType precision, TemporalJavaType<T> temporalJtd, TypeConfiguration typeConfiguration) {
		// Special case java.util.Date, because TemporalJavaType#resolveTypeForPrecision doesn't support widening,
		// since the main purpose of that method is to determine the final java type based on the reflective type
		// + the explicit @Temporal(TemporalType...) configuration
		if ( temporalJtd == null || java.util.Date.class.isAssignableFrom( temporalJtd.getJavaTypeClass() ) ) {
			final JavaType<?> descriptor =
					typeConfiguration.getJavaTypeRegistry()
							.getDescriptor( resolveJavaTypeClass( precision ) );
			//noinspection unchecked
			return (TemporalJavaType<T>) descriptor;
		}
		else {
			return temporalJtd.resolveTypeForPrecision( precision, typeConfiguration );
		}
	}

	private static <T> TemporalJavaType<T> getTemporalJavaType(
			BindableType<T> declaredParameterType, BindingContext bindingContext) {
		if ( declaredParameterType != null ) {
			final SqmExpressible<T> sqmExpressible = bindingContext.resolveExpressible( declaredParameterType );
			if ( !( JavaTypeHelper.isTemporal( sqmExpressible.getExpressibleJavaType() ) ) ) {
				throw new UnsupportedOperationException(
						"Cannot treat non-temporal parameter type with temporal precision"
				);
			}
			return (TemporalJavaType<T>) sqmExpressible.getExpressibleJavaType();
		}
		else {
			return null;
		}
	}

	public static JdbcMapping resolveBindType(
			Object value,
			JdbcMapping baseType,
			TypeConfiguration typeConfiguration) {
		if ( value == null || !JavaTypeHelper.isTemporal( baseType.getJdbcJavaType() ) ) {
			return baseType;
		}
		else {
			final Class<?> javaType = value.getClass();
			final TemporalJavaType<?> temporalJavaType = (TemporalJavaType<?>) baseType.getJdbcJavaType();
			final TemporalType temporalType = temporalJavaType.getPrecision();
			final BindableType<?> bindableType = (BindableType<?>) baseType;
			return switch ( temporalType ) {
				case TIMESTAMP -> (JdbcMapping) resolveTimestampTemporalTypeVariant( javaType, bindableType, typeConfiguration );
				case DATE -> (JdbcMapping) resolveDateTemporalTypeVariant( javaType, bindableType, typeConfiguration );
				case TIME -> (JdbcMapping) resolveTimeTemporalTypeVariant( javaType, bindableType, typeConfiguration );
			};
		}
	}

	private static BindableType<?> resolveTimestampTemporalTypeVariant(
			Class<?> javaType,
			BindableType<?> baseType,
			TypeConfiguration typeConfiguration) {
		if ( baseType.getJavaType().isAssignableFrom( javaType ) ) {
			return baseType;
		}
		else if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.CALENDAR );
		}
		else if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIMESTAMP );
		}
		else if ( Instant.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INSTANT );
		}
		else if ( OffsetDateTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.OFFSET_DATE_TIME );
		}
		else if ( ZonedDateTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.ZONED_DATE_TIME );
		}
		else if ( OffsetTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.OFFSET_TIME );
		}
		else {
			throw new IllegalArgumentException( "Unsure how to handle given Java type ["
												+ javaType.getName() + "] as TemporalType#TIMESTAMP" );
		}
	}

	private static BindableType<?> resolveDateTemporalTypeVariant(
			Class<?> javaType,
			BindableType<?> baseType,
			TypeConfiguration typeConfiguration) {
		if ( baseType.getJavaType().isAssignableFrom( javaType ) ) {
			return baseType;
		}
		else if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.CALENDAR_DATE );
		}
		else if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DATE );
		}
		else if ( Instant.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INSTANT );
		}
		else if ( OffsetDateTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.OFFSET_DATE_TIME );
		}
		else if ( ZonedDateTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.ZONED_DATE_TIME );
		}
		else {
			throw new IllegalArgumentException( "Unsure how to handle given Java type ["
												+ javaType.getName() + "] as TemporalType#DATE" );
		}
	}

	private static BindableType<?> resolveTimeTemporalTypeVariant(
			Class<?> javaType,
			BindableType<?> baseType,
			TypeConfiguration typeConfiguration) {
		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.CALENDAR_TIME );
		}
		else if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIME );
		}
		else if ( LocalTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.LOCAL_TIME );
		}
		else if ( OffsetTime.class.isAssignableFrom( javaType ) ) {
			return typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.OFFSET_TIME );
		}
		else {
			throw new IllegalArgumentException( "Unsure how to handle given Java type ["
												+ javaType.getName() + "] as TemporalType#TIME" );
		}
	}
}
