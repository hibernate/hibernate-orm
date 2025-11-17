/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.hibernate.query.QueryArgumentException;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.type.BindableType;
import org.hibernate.type.BindingContext;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.TemporalType;

/**
 * @author Andrea Boriero
 */
public class QueryParameterBindingValidator {

	public static final QueryParameterBindingValidator INSTANCE = new QueryParameterBindingValidator();

	private QueryParameterBindingValidator() {
	}

	public void validate(BindableType<?> paramType, Object bind, BindingContext bindingContext) {
		validate( paramType, bind, null, bindingContext );
	}

	public void validate(
			BindableType<?> paramType,
			Object bind,
			TemporalType temporalPrecision,
			BindingContext bindingContext) {
		if ( bind == null || paramType == null ) {
			// nothing we can check
			return;
		}

		final SqmBindableType<?> sqmExpressible = bindingContext.resolveExpressible( paramType );
		final Class<?> parameterJavaType =
				paramType.getJavaType() != null
						? paramType.getJavaType()
						: sqmExpressible.getJavaType();

		if ( parameterJavaType != null ) {
			if ( bind instanceof Collection<?> collection
					&& !Collection.class.isAssignableFrom( parameterJavaType ) ) {
				// we have a collection passed in where we are expecting a non-collection.
				// 		NOTE : this can happen in Hibernate's notion of "parameter list" binding
				// 		NOTE2 : the case of a collection value and an expected collection (if that can even happen)
				//			will fall through to the main check.
				validateCollectionValuedParameterBinding(
						parameterJavaType,
						collection,
						temporalPrecision
				);
			}
			else if ( bind.getClass().isArray() ) {
				validateArrayValuedParameterBinding(
						parameterJavaType,
						bind,
						temporalPrecision
				);
			}
			else if ( !isValidBindValue(
					sqmExpressible.getExpressibleJavaType(),
					parameterJavaType,
					bind,
					temporalPrecision
			) ) {
				throw new QueryArgumentException(
						String.format(
								"Argument [%s] of type [%s] did not match parameter type [%s (%s)]",
								bind,
								bind.getClass().getName(),
								parameterJavaType.getName(),
								extractName( temporalPrecision )
						),
						parameterJavaType,
						bind
				);
			}
		}
		// else nothing we can check
	}

	private String extractName(TemporalType temporalType) {
		return temporalType == null ? "n/a" : temporalType.name();
	}

	private void validateCollectionValuedParameterBinding(
			Class<?> parameterType,
			Collection<?> value,
			TemporalType temporalType) {
		// validate the elements...
		for ( Object element : value ) {
			if ( !isValidBindValue( parameterType, element, temporalType ) ) {
				throw new QueryArgumentException(
						String.format(
								"Parameter value element [%s] did not match expected type [%s (%s)]",
								element,
								parameterType.getName(),
								extractName( temporalType )
						)
						,
						parameterType,
						element
				);
			}
		}
	}

	private static boolean isValidBindValue(
			JavaType<?> expectedJavaType,
			Class<?> expectedType,
			Object value,
			TemporalType temporalType) {
		if ( value == null ) {
			return true;
		}
		else if ( expectedJavaType.isInstance( value ) ) {
			return true;
		}
		else if ( temporalType != null ) {
			final boolean parameterDeclarationIsTemporal = Date.class.isAssignableFrom( expectedType )
					|| Calendar.class.isAssignableFrom( expectedType );
			final boolean bindIsTemporal = value instanceof Date
					|| value instanceof Calendar;

			return parameterDeclarationIsTemporal && bindIsTemporal;
		}

		return false;
	}

	private static boolean isValidBindValue(
			Class<?> expectedType,
			Object value,
			TemporalType temporalType) {
		if ( expectedType.isPrimitive() ) {
			if ( expectedType == boolean.class ) {
				return value instanceof Boolean;
			}
			else if ( expectedType == char.class ) {
				return value instanceof Character;
			}
			else if ( expectedType == byte.class ) {
				return value instanceof Byte;
			}
			else if ( expectedType == short.class ) {
				return value instanceof Short;
			}
			else if ( expectedType == int.class ) {
				return value instanceof Integer;
			}
			else if ( expectedType == long.class ) {
				return value instanceof Long;
			}
			else if ( expectedType == float.class ) {
				return value instanceof Float;
			}
			else if ( expectedType == double.class ) {
				return value instanceof Double;
			}
			return false;
		}
		else if ( value == null) {
			return true;
		}
		else if ( expectedType.isInstance( value ) ) {
			return true;
		}
		else if ( temporalType != null ) {
			final boolean parameterDeclarationIsTemporal = Date.class.isAssignableFrom( expectedType )
					|| Calendar.class.isAssignableFrom( expectedType );
			final boolean bindIsTemporal = value instanceof Date
					|| value instanceof Calendar;

			return parameterDeclarationIsTemporal && bindIsTemporal;
		}

		return false;
	}

	private void validateArrayValuedParameterBinding(
			Class<?> parameterType,
			Object value,
			TemporalType temporalType) {
		if ( !parameterType.isArray() ) {
			throw new QueryArgumentException(
					String.format(
							"Encountered array-valued parameter binding, but was expecting [%s (%s)]",
							parameterType.getName(),
							extractName( temporalType )
					),
					parameterType,
					value
			);
		}

		if ( value.getClass().getComponentType().isPrimitive() ) {
			// we have a primitive array.  we validate that the actual array has the component type (type of elements)
			// we expect based on the component type of the parameter specification
			if ( !parameterType.getComponentType().isAssignableFrom( value.getClass().getComponentType() ) ) {
				throw new QueryArgumentException(
						String.format(
								"Primitive array-valued parameter bind value type [%s] did not match expected type [%s (%s)]",
								value.getClass().getComponentType().getName(),
								parameterType.getName(),
								extractName( temporalType )
						),
						parameterType,
						value
				);
			}
		}
		else {
			// we have an object array.  Here we loop over the array and physically check each element against
			// the type we expect based on the component type of the parameter specification
			final Object[] array = (Object[]) value;
			for ( Object element : array ) {
				if ( !isValidBindValue( parameterType.getComponentType(), element, temporalType ) ) {
					throw new QueryArgumentException(
							String.format(
									"Array-valued parameter value element [%s] did not match expected type [%s (%s)]",
									element,
									parameterType.getName(),
									extractName( temporalType )
							),
							parameterType,
							array
					);
				}
			}
		}
	}
}
