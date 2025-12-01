/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.Collection;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.QueryArgumentException;
import org.hibernate.type.BindableType;

import static org.hibernate.query.internal.QueryArguments.areInstances;
import static org.hibernate.query.internal.QueryArguments.isInstance;

/**
 * @author Andrea Boriero
 */
@Internal
public class QueryParameterBindingValidator {

	private QueryParameterBindingValidator() {
	}

	public static void validate(BindableType<?> parameterType, Object argument, SessionFactoryImplementor factory) {
		if ( argument != null && parameterType != null ) {
			final var parameterJavaType = getParameterJavaType( parameterType, factory );
			if ( parameterJavaType != null ) {
				if ( argument instanceof Collection<?> collection
						&& !Collection.class.isAssignableFrom( parameterJavaType ) ) {
					// we have a collection passed in where we are expecting a non-collection.
					// 		NOTE: this can happen in Hibernate's notion of "parameter list" binding
					// 		NOTE2: the case of a collection value and an expected collection (if that can even happen)
					//			   will fall through to the main check.
					validateCollectionValuedParameterBinding( parameterType, parameterJavaType, collection, factory );
				}
				else if ( argument.getClass().isArray() ) {
					validateArrayValuedParameterBinding( parameterJavaType, argument );
				}
				else {
					validateSingleValuedParameterBinding( parameterType, parameterJavaType, argument, factory );
				}
			}
			// else nothing we can check
		}
	}

	private static Class<?> getParameterJavaType(BindableType<?> parameterType, SessionFactoryImplementor factory) {
		final var javaType = parameterType.getJavaType();
		return javaType != null
				? javaType
				: factory.getQueryEngine().getCriteriaBuilder()
						.resolveExpressible( parameterType )
						.getJavaType();
	}

	private static void validateSingleValuedParameterBinding(
			BindableType<?> parameterType, Class<?> parameterJavaType,
			Object value,
			SessionFactoryImplementor factory) {
		if ( !isInstance( parameterType, value,
				factory.getQueryEngine().getCriteriaBuilder() ) ) {
			throw new QueryArgumentException( "Argument did not match parameter type",
					parameterJavaType, value );
		}
	}

	private static void validateCollectionValuedParameterBinding(
			BindableType<?> parameterType, Class<?> parameterJavaType,
			Collection<?> values,
			SessionFactoryImplementor factory) {
		if ( !areInstances( parameterType, values,
				factory.getQueryEngine().getCriteriaBuilder() ) ) {
			throw new QueryArgumentException( "Collection-valued argument did not match parameter type",
					parameterJavaType, values );

		}
	}

	private static void validateArrayValuedParameterBinding(Class<?> parameterType, Object value) {
		if ( !parameterType.isArray() ) {
			throw new QueryArgumentException( "Unexpected array-valued parameter binding",
					parameterType, value );
		}

		final var componentType = value.getClass().getComponentType();
		final var parameterComponentType = parameterType.getComponentType();
		if ( componentType.isPrimitive() ) {
			// We have a primitive array.
			// We validate that the actual array has the component type (type of elements)
			// that we expect based on the component type of the parameter specification.
			if ( !parameterComponentType.isAssignableFrom( componentType ) ) {
				throw new QueryArgumentException( "Primitive array-valued argument type did not match parameter type",
						parameterType, value );
			}
		}
		else {
			// We have an object array.
			// Here we loop over the array and physically check each element against the
			// type we expect based on the component type of the parameter specification.
			for ( Object element : (Object[]) value ) {
				if ( element != null && !parameterComponentType.isInstance( element ) ) {
					throw new QueryArgumentException( "Array element did not match parameter element type",
							parameterComponentType, element );
				}
			}
		}
	}
}
