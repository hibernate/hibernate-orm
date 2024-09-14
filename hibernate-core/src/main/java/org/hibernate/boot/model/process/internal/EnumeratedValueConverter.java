/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.process.internal;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

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

		ReflectHelper.ensureAccessibility( valueField );

		final Class<E> enumJavaTypeClass = enumJavaType.getJavaTypeClass();
		final E[] enumConstants = enumJavaTypeClass.getEnumConstants();
		relationalToEnumMap = CollectionHelper.mapOfSize( enumConstants.length );
		enumToRelationalMap = CollectionHelper.mapOfSize( enumConstants.length );
		for ( int i = 0; i < enumConstants.length; i++ ) {
			final E enumConstant = enumConstants[i];
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
		if ( relationalForm == null ) {
			return null;
		}
		return relationalToEnumMap.get( relationalForm );
	}

	@Override
	public @Nullable R toRelationalValue(@Nullable E domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return enumToRelationalMap.get( domainForm );
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
