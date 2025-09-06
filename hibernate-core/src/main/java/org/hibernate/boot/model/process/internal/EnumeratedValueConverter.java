/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.ReflectHelper.ensureAccessibility;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * @author Steve Ebersole
 */
public class EnumeratedValueConverter<E extends Enum<E>,R> implements BasicValueConverter<E,R> {
	private final EnumJavaType<E> enumJavaType;
	private final JavaType<R> relationalJavaType;

	private final Map<R,E> relationalToEnumMap;
	private final Map<E,R> enumToRelationalMap;

	public EnumeratedValueConverter(
			EnumJavaType<E> enumJavaType,
			JavaType<R> relationalJavaType,
			Field valueField) {
		this.enumJavaType = enumJavaType;
		this.relationalJavaType = relationalJavaType;

		ensureAccessibility( valueField );

		final var enumJavaTypeClass = enumJavaType.getJavaTypeClass();
		final E[] enumConstants = enumJavaTypeClass.getEnumConstants();
		relationalToEnumMap = mapOfSize( enumConstants.length );
		enumToRelationalMap = mapOfSize( enumConstants.length );
		for ( final E enumConstant : enumConstants ) {
			try {
				//noinspection unchecked
				final R relationalValue = (R) valueField.get( enumConstant );

				relationalToEnumMap.put( relationalValue, enumConstant );
				enumToRelationalMap.put( enumConstant, relationalValue );
			}
			catch (IllegalAccessException e) {
				throw new RuntimeException( e );
			}
		}
	}

	public Set<R> getRelationalValueSet() {
		return relationalToEnumMap.keySet();
	}

	@Override
	public @Nullable E toDomainValue(@Nullable R relationalForm) {
		return relationalForm == null ? null : relationalToEnumMap.get( relationalForm );
	}

	@Override
	public @Nullable R toRelationalValue(@Nullable E domainForm) {
		return domainForm == null ? null : enumToRelationalMap.get( domainForm );
	}

	@Override
	public JavaType<E> getDomainJavaType() {
		return enumJavaType;
	}

	@Override
	public JavaType<R> getRelationalJavaType() {
		return relationalJavaType;
	}
}
