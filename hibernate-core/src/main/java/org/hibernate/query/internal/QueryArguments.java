/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.persistence.metamodel.Type;
import org.hibernate.HibernateException;
import org.hibernate.type.BindingContext;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.EntityJavaType;

import java.util.Collection;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Gavin King
 */
public class QueryArguments {

	private static boolean isInstance(Object value, JavaType<?> javaType) {
		try {
			// special handling for entity arguments due to
			// the possibility of an uninitialized proxy
			// (which we don't want or need to fetch)
			if ( javaType instanceof EntityJavaType<?> ) {
				final var javaTypeClass = javaType.getJavaTypeClass();
				final var initializer = extractLazyInitializer( value );
				final var valueEntityClass =
						initializer != null
								? initializer.getPersistentClass()
								: value.getClass();
				// accept assignability in either direction
				return javaTypeClass.isAssignableFrom( valueEntityClass )
					|| valueEntityClass.isAssignableFrom( javaTypeClass );
			}
			else {
				// require that the argument be assignable to the parameter
				return javaType.isInstance( javaType.coerce( value ) );
			}
		}
		catch (HibernateException ce) {
			return false;
		}
	}

	public static boolean isInstance(
			Type<?> parameterType, Object value,
			BindingContext bindingContext) {
		if ( value == null ) {
			return true;
		}
		final var sqmExpressible = bindingContext.resolveExpressible( parameterType );
		assert sqmExpressible != null;
		final var javaType = sqmExpressible.getExpressibleJavaType();
		return isInstance( value, javaType );
	}

	public static boolean areInstances(
			Type<?> parameterType, Collection<?> values,
			BindingContext bindingContext) {
		if ( values.isEmpty() ) {
			return true;
		}
		final var sqmExpressible = bindingContext.resolveExpressible( parameterType );
		assert sqmExpressible != null;
		final var javaType = sqmExpressible.getExpressibleJavaType();
		for ( Object value : values ) {
			if ( !isInstance( value, javaType ) ) {
				return false;
			}
		}
		return true;
	}

	public static boolean areInstances(
			Type<?> parameterType, Object[] values,
			BindingContext bindingContext) {
		if ( values.length == 0 ) {
			return true;
		}
		if ( parameterType.getJavaType()
				.isAssignableFrom( values.getClass().getComponentType() ) ) {
			return true;
		}
		final var sqmExpressible = bindingContext.resolveExpressible( parameterType );
		assert sqmExpressible != null;
		final var javaType = sqmExpressible.getExpressibleJavaType();
		for ( Object value : values ) {
			if ( !isInstance( value, javaType ) ) {
				return false;
			}
		}
		return true;
	}
}
