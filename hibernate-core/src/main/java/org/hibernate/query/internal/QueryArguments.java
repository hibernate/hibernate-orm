/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.persistence.metamodel.Type;
import org.hibernate.HibernateException;
import org.hibernate.type.BindingContext;
import org.hibernate.type.descriptor.WrapperOptions;

import java.util.Collection;

/**
 * @since 7.3
 *
 * @author Gavin King
 */
public class QueryArguments {

	public static boolean isInstance(
			Type<?> parameterType, Object value,
			BindingContext bindingContext, WrapperOptions options) {
		if ( value == null ) {
			return true;
		}
		final var sqmExpressible = bindingContext.resolveExpressible( parameterType );
		assert sqmExpressible != null;
		final var javaType = sqmExpressible.getExpressibleJavaType();
		if ( !javaType.isInstance( value ) ) {
			try {
				// if this succeeds, we are good
				javaType.wrap( value, options );
			}
			catch (HibernateException | UnsupportedOperationException e) {
				return false;
			}
		}
		return true;
	}

	public static boolean areInstances(
			Type<?> parameterType, Collection<?> values,
			BindingContext bindingContext, WrapperOptions options) {
		if ( values.isEmpty() ) {
			return true;
		}
		final var sqmExpressible = bindingContext.resolveExpressible( parameterType );
		assert sqmExpressible != null;
		final var javaType = sqmExpressible.getExpressibleJavaType();
		for ( Object value : values ) {
			if ( !javaType.isInstance( value ) ) {
				try {
					// if this succeeds, we are good
					javaType.wrap( value, options );
				}
				catch (HibernateException | UnsupportedOperationException e) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean areInstances(
			Type<?> parameterType, Object[] values,
			BindingContext bindingContext, WrapperOptions options) {
		if ( values.length == 0 ) {
			return true;
		}
		if ( parameterType.getJavaType().isAssignableFrom( values.getClass().getComponentType() ) ) {
			return true;
		}
		final var sqmExpressible = bindingContext.resolveExpressible( parameterType );
		assert sqmExpressible != null;
		final var javaType = sqmExpressible.getExpressibleJavaType();
		for ( Object value : values ) {
			if ( !javaType.isInstance( value ) ) {
				try {
					// if this succeeds, we are good
					javaType.wrap( value, options );
				}
				catch (HibernateException | UnsupportedOperationException e) {
					return false;
				}
			}
		}
		return true;
	}
}
