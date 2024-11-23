/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.internal;

import java.lang.reflect.Array;
import java.util.Collection;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType;

/**
 * Handles conversion to/from a collection of a converted element type.
 */
public class CollectionConverter<X extends Collection<Object>, Y> implements BasicValueConverter<X, Y> {

	private final BasicValueConverter<Object, Object> elementConverter;
	private final BasicCollectionJavaType<X, ?> domainJavaType;
	private final JavaType<Y> relationalJavaType;

	public CollectionConverter(
			BasicValueConverter<Object, Object> elementConverter,
			BasicCollectionJavaType<X, ?> domainJavaType,
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
		final Object[] relationalArray = (Object[]) relationalForm;
		final X domainForm = domainJavaType.getSemantics().instantiateRaw( relationalArray.length, null );
		for ( int i = 0; i < relationalArray.length; i++ ) {
			domainForm.add( elementConverter.toDomainValue( relationalArray[i] ) );
		}
		return domainForm;
	}

	@Override
	public Y toRelationalValue(X domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		final Object[] relationalArray = (Object[]) Array.newInstance(
				elementConverter.getRelationalJavaType().getJavaTypeClass(),
				domainForm.size()
		);
		int i = 0;
		for ( Object domainValue : domainForm ) {
			relationalArray[i++] = elementConverter.toRelationalValue( domainValue );
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
