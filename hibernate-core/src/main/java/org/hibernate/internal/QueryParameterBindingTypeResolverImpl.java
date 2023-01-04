/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	@Override
	public <T> BindableType<? extends T> resolveParameterBindType(T bindValue) {
		if ( bindValue == null ) {
			// we can't guess
			return null;
		}

		final LazyInitializer lazyInitializer = extractLazyInitializer( bindValue );
		final Class<?> clazz = lazyInitializer != null ? lazyInitializer.getPersistentClass() : bindValue.getClass();

		// Resolve superclass bindable type if necessary, as we don't register types for e.g. Inet4Address
		Class<?> c = clazz;
		do {
			final BindableType<?> type = resolveParameterBindType( c );
			if ( type != null ) {
				//noinspection unchecked
				return (BindableType<? extends T>) type;
			}
			c = c.getSuperclass();
		}
		while ( c != Object.class );
		if ( !clazz.isEnum() && Serializable.class.isAssignableFrom( clazz ) ) {
			//noinspection unchecked
			return (BindableType<? extends T>) resolveParameterBindType( Serializable.class );
		}
		return null;
	}

}
