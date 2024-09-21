/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.proxy.LazyInitializer;
import org.hibernate.query.BindableType;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;

import java.io.Serializable;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @implNote This code was originally in {@link SessionFactoryImpl}, but has been factored out so that it
 *           can be shared by {@link org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl},
 *           which is where it really belongs, IMO. Eventually, we can kill off the subtyping relationship
 *           between {@link SessionFactoryImpl} and {@link QueryParameterBindingTypeResolver}, and then
 *           get rid of this class.
 *
 * @author Gavin King
 *
 * @since 6.2
 */
public abstract class QueryParameterBindingTypeResolverImpl implements QueryParameterBindingTypeResolver {

	@Override
	public <T> BindableType<T> resolveParameterBindType(Class<T> javaType) {
		return getMappingMetamodel().resolveQueryParameterType( javaType );
	}

	@Override @SuppressWarnings("unchecked")
	public <T> BindableType<? super T> resolveParameterBindType(T bindValue) {
		if ( bindValue == null ) {
			// we can't guess
			return null;
		}

		final Class<T> clazz = unproxiedClass( bindValue );

		// Resolve superclass bindable type if necessary, as we don't register types for e.g. Inet4Address
		Class<? super T> c = clazz;
		do {
			final BindableType<? super T> type = resolveParameterBindType( c );
			if ( type != null ) {
				return type;
			}
			c = c.getSuperclass();
		}
		while ( c != Object.class );

		if ( clazz.isEnum() ) {
			return null; //createEnumType( (Class) clazz );
		}
		else if ( Serializable.class.isAssignableFrom( clazz ) ) {
			return (BindableType<? super T>) resolveParameterBindType( Serializable.class );
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> unproxiedClass(T bindValue) {
		final LazyInitializer lazyInitializer = extractLazyInitializer( bindValue );
		final Class<?> result = lazyInitializer != null ? lazyInitializer.getPersistentClass() : bindValue.getClass();
		return (Class<T>) result;
	}

}
