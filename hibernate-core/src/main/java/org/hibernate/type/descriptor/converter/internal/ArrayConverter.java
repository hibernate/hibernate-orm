/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.internal;

import java.lang.reflect.Array;

import org.hibernate.internal.build.AllowReflection;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Given a {@link BasicValueConverter} for array elements, handles conversion
 * to and from an array of the converted element type.
 *
 * @param <E> the unconverted element type
 * @param <F> the converted element type
 * @param <T> the unconverted array type
 * @param <S> the converted array type
 */
@AllowReflection
public class ArrayConverter<T, S, E, F> implements BasicValueConverter<T, S> {

	private final BasicValueConverter<E, F> elementConverter;
	private final JavaType<T> domainJavaType;
	private final JavaType<S> relationalJavaType;

	public ArrayConverter(
			BasicValueConverter<E, F> elementConverter,
			JavaType<T> domainJavaType,
			JavaType<S> relationalJavaType) {
		this.elementConverter = elementConverter;
		this.domainJavaType = domainJavaType;
		this.relationalJavaType = relationalJavaType;
	}

	@Override
	public T toDomainValue(S relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		else {
			final Class<E> elementClass = elementConverter.getDomainJavaType().getJavaTypeClass();
			if ( relationalForm.getClass().getComponentType() == elementClass) {
				//noinspection unchecked
				return (T) relationalForm;
			}
			else {
				//noinspection unchecked
				return convertTo( (F[]) relationalForm, elementClass );
			}
		}
	}

	private T convertTo(F[] relationalArray, Class<E> elementClass) {
		//TODO: the following implementation only handles conversion between non-primitive arrays!
		//noinspection unchecked
		final E[] domainArray = (E[]) Array.newInstance( elementClass, relationalArray.length );
		for ( int i = 0; i < relationalArray.length; i++ ) {
			domainArray[i] = elementConverter.toDomainValue( relationalArray[i] );
		}
		//noinspection unchecked
		return (T) domainArray;
	}

	@Override
	public S toRelationalValue(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		else {
			final Class<F> elementClass = elementConverter.getRelationalJavaType().getJavaTypeClass();
			if ( domainForm.getClass().getComponentType() == elementClass) {
				//noinspection unchecked
				return (S) domainForm;
			}
			else {
				//noinspection unchecked
				return convertFrom((E[]) domainForm, elementClass);
			}
		}
	}

	private S convertFrom(E[] domainArray, Class<F> elementClass) {
		//TODO: the following implementation only handles conversion between non-primitive arrays!
		//noinspection unchecked
		final F[] relationalArray = (F[]) Array.newInstance( elementClass, domainArray.length );
		for ( int i = 0; i < domainArray.length; i++ ) {
			relationalArray[i] = elementConverter.toRelationalValue( domainArray[i] );
		}
		//noinspection unchecked
		return (S) relationalArray;
	}

	@Override
	public JavaType<T> getDomainJavaType() {
		return domainJavaType;
	}

	@Override
	public JavaType<S> getRelationalJavaType() {
		return relationalJavaType;
	}

}
