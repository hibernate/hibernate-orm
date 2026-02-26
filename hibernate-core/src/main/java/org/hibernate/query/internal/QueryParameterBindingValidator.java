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
						throw queryArgumentException(
								"Collection-valued argument to parameter %s has an incompatible type",
								parameterJavaType, collection, parameter );
					}
				}
				else if ( !argument.getClass().isArray() ) {
					// assume single-valued argument
					if ( !isInstance( parameterType, argument, criteriaBuilder ) ) {
						throw queryArgumentException(
								"Argument to parameter %s has an incompatible type",
								parameterJavaType, argument, parameter );
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
			String messagePattern, Class<?> parameterJavaType, Object value, QueryParameter<?> parameter) {
		final String message = String.format( messagePattern,
				parameter.isNamed() ? "named '" + parameter.getName() + "'"
						: "at position " + parameter.getPosition() );
		return new QueryArgumentException( message, parameterJavaType, value );
	}

	private static void validateArrayValuedParameterBinding(
			Class<?> parameterType, Object value, QueryParameter<?> parameter) {
		if ( !parameterType.isArray() ) {
			throw queryArgumentException( "Unexpected array-valued argument to parameter %s",
					parameterType, value, parameter );
		}

		final var componentType = value.getClass().getComponentType();
		final var parameterComponentType = parameterType.getComponentType();
		if ( componentType.isPrimitive() ) {
			// We have a primitive array.
			// We validate that the actual array has the component type (type of elements)
			// that we expect based on the component type of the parameter specification.
			if ( !parameterComponentType.isAssignableFrom( componentType ) ) {
				throw queryArgumentException(
						"Primitive array-valued argument to parameter %s has an incompatible component type",
						parameterType, value, parameter );
			}
		}
		else {
			// We have an object array.
			// Here we loop over the array and physically check each element against the
			// type we expect based on the component type of the parameter specification.
			for ( Object element : (Object[]) value ) {
				if ( element != null && !parameterComponentType.isInstance( element ) ) {
					throw queryArgumentException(
							"Array-valued argument to parameter %s has an element with an incompatible type",
							parameterComponentType, element, parameter );
				}
			}
		}
	}
}
