/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.converter.internal;

import java.lang.reflect.Array;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Handles conversion to/from an array of a converted element type.
 */
public class ArrayConverter<X, Y> implements BasicValueConverter<X, Y> {

	private final BasicValueConverter<Object, Object> elementConverter;
	private final JavaType<X> domainJavaType;
	private final JavaType<Y> relationalJavaType;

	public ArrayConverter(
			BasicValueConverter<Object, Object> elementConverter,
			JavaType<X> domainJavaType,
			JavaType<Y> relationalJavaType) {
		this.elementConverter = elementConverter;
		this.domainJavaType = domainJavaType;
		this.relationalJavaType = relationalJavaType;
	}

	@Override
	public X toDomainValue(Y relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		if ( relationalForm.getClass().getComponentType() == elementConverter.getDomainJavaType().getJavaTypeClass() ) {
			//noinspection unchecked
			return (X) relationalForm;
		}
		final Object[] relationalArray = (Object[]) relationalForm;
		final Object[] domainArray = (Object[]) Array.newInstance(
				elementConverter.getDomainJavaType().getJavaTypeClass(),
				relationalArray.length
		);
		for ( int i = 0; i < relationalArray.length; i++ ) {
			domainArray[i] = elementConverter.toDomainValue( relationalArray[i] );
		}
		//noinspection unchecked
		return (X) domainArray;
	}

	@Override
	public Y toRelationalValue(X domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		if ( domainForm.getClass().getComponentType() == elementConverter.getRelationalJavaType().getJavaTypeClass() ) {
			//noinspection unchecked
			return (Y) domainForm;
		}
		final Object[] domainArray = (Object[]) domainForm;
		final Object[] relationalArray = (Object[]) Array.newInstance(
				elementConverter.getRelationalJavaType().getJavaTypeClass(),
				domainArray.length
		);
		for ( int i = 0; i < domainArray.length; i++ ) {
			relationalArray[i] = elementConverter.toRelationalValue( domainArray[i] );
		}
		//noinspection unchecked
		return (Y) relationalArray;
	}

	@Override
	public JavaType<X> getDomainJavaType() {
		return domainJavaType;
	}

	@Override
	public JavaType<Y> getRelationalJavaType() {
		return relationalJavaType;
	}

}
