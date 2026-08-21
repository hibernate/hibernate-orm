/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.Collection;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.QueryArgumentException;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.BindableType;

import static org.hibernate.query.internal.QueryArguments.areInstances;
import static org.hibernate.query.internal.QueryArguments.isInstance;

/**
 * @author Andrea Boriero
 */
class QueryParameterBindingValidator {

	private QueryParameterBindingValidator() {
	}

	public static void validate(
			QueryParameter<?> parameter,
			BindableType<?> parameterType,
			Object argument,
			SessionFactoryImplementor factory) {
		if ( argument != null && parameterType != null ) {
			final var parameterJavaType = getParameterJavaType( parameterType, factory );
			if ( parameterJavaType != null ) {
				final var criteriaBuilder = factory.getQueryEngine().getCriteriaBuilder();
				if ( argument instanceof Collection<?> collection
						&& !Collection.class.isAssignableFrom( parameterJavaType ) ) {
					// We have a collection passed in where we were expecting a non-collection.
					// NOTE: This can happen in Hibernate's notion of "parameter list" binding.
					// NOTE2: The case of a collection value and an expected collection
					// 	      (if that can even happen) will fall through to the main check.
					if ( !areInstances( parameterType, collection, criteriaBuilder ) ) {
						throw queryArgumentException( parameterJavaType, collection, parameter );
					}
				}
				else if ( !argument.getClass().isArray() ) {
					// assume single-valued argument
					if ( !isInstance( parameterType, argument, criteriaBuilder ) ) {
						throw queryArgumentException( parameterJavaType, argument, parameter );
					}
				}
				else {
					validateArrayValuedParameterBinding( parameterJavaType, argument, parameter );
				}
			}
			// else nothing we can check
		}
	}

	private static Class<?> getParameterJavaType(
			BindableType<?> parameterType, SessionFactoryImplementor factory) {
		final var javaType = parameterType.getJavaType();
		return javaType != null
				? javaType
				: factory.getQueryEngine().getCriteriaBuilder()
						.resolveExpressible( parameterType )
						.getJavaType();
	}

	private static @NonNull QueryArgumentException queryArgumentException(
			Class<?> parameterJavaType, Object value, QueryParameter<?> parameter) {
		if ( parameter.isNamed() ) {
			return new QueryArgumentException( "Argument to parameter named '"
						+ parameter.getName() + "' has an element with an incompatible type",
					parameterJavaType, value );
		}
		else {
			return new QueryArgumentException( "Argument to parameter at position "
						+ parameter.isOrdinal() + " has an element with an incompatible type",
					parameterJavaType, value );
		}
	}

	private static @NonNull QueryArgumentException queryArgumentException(
			Class<?> parameterJavaType, Collection<?> values, QueryParameter<?> parameter) {
		if ( parameter.isNamed() ) {
			return new QueryArgumentException( "Collection-values argument to parameter named '"
						+ parameter.getName() + "' has an incompatible type",
					parameterJavaType, values );
		}
		else {
			return new QueryArgumentException( "Collection-values argument to parameter at position "
						+ parameter.isOrdinal() + " has has an incompatible type",
					parameterJavaType, values );
		}
	}

	private static void validateArrayValuedParameterBinding(
			Class<?> parameterType, Object value, QueryParameter<?> parameter) {
		// TODO: improve the error messages using the given parameter info
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
