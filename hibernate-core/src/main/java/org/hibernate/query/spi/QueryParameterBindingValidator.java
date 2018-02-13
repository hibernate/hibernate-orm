/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import javax.persistence.TemporalType;

import org.hibernate.type.Type;

/**
 * @author Andrea Boriero
 */
public class QueryParameterBindingValidator {

	public static final QueryParameterBindingValidator INSTANCE = new QueryParameterBindingValidator();

	private QueryParameterBindingValidator() {
	}

	public <P> void validate(Type paramType, Object bind) {
		validate( paramType, bind, null );
	}

	public <P> void validate(Type paramType, Object bind, TemporalType temporalType) {
		if ( bind == null || paramType == null ) {
			// nothing we can check
			return;
		}
		final Class parameterType = paramType.getReturnedClass();
		if ( parameterType == null ) {
			// nothing we can check
			return;
		}

		if ( Collection.class.isInstance( bind ) && !Collection.class.isAssignableFrom( parameterType ) ) {
			// we have a collection passed in where we are expecting a non-collection.
			// 		NOTE : this can happen in Hibernate's notion of "parameter list" binding
			// 		NOTE2 : the case of a collection value and an expected collection (if that can even happen)
			//			will fall through to the main check.
			validateCollectionValuedParameterBinding( parameterType, (Collection) bind, temporalType );
		}
		else if ( bind.getClass().isArray() ) {
			validateArrayValuedParameterBinding( parameterType, bind, temporalType );
		}
		else {
			if ( !isValidBindValue( parameterType, bind, temporalType ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter value [%s] did not match expected type [%s (%s)]",
								bind,
								parameterType.getName(),
								extractName( temporalType )
						)
				);
			}
		}
	}

	private String extractName(TemporalType temporalType) {
		return temporalType == null ? "n/a" : temporalType.name();
	}

	private void validateCollectionValuedParameterBinding(
			Class parameterType,
			Collection value,
			TemporalType temporalType) {
		// validate the elements...
		for ( Object element : value ) {
			if ( !isValidBindValue( parameterType, element, temporalType ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Parameter value element [%s] did not match expected type [%s (%s)]",
								element,
								parameterType.getName(),
								extractName( temporalType )
						)
				);
			}
		}
	}

	private static boolean isValidBindValue(Class expectedType, Object value, TemporalType temporalType) {
		if ( expectedType.isPrimitive() ) {
			if ( expectedType == boolean.class ) {
				return Boolean.class.isInstance( value );
			}
			else if ( expectedType == char.class ) {
				return Character.class.isInstance( value );
			}
			else if ( expectedType == byte.class ) {
				return Byte.class.isInstance( value );
			}
			else if ( expectedType == short.class ) {
				return Short.class.isInstance( value );
			}
			else if ( expectedType == int.class ) {
				return Integer.class.isInstance( value );
			}
			else if ( expectedType == long.class ) {
				return Long.class.isInstance( value );
			}
			else if ( expectedType == float.class ) {
				return Float.class.isInstance( value );
			}
			else if ( expectedType == double.class ) {
				return Double.class.isInstance( value );
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
			final boolean bindIsTemporal = Date.class.isInstance( value )
					|| Calendar.class.isInstance( value );

			if ( parameterDeclarationIsTemporal && bindIsTemporal ) {
				return true;
			}
		}

		return false;
	}

	private void validateArrayValuedParameterBinding(
			Class parameterType,
			Object value,
			TemporalType temporalType) {
		if ( !parameterType.isArray() ) {
			throw new IllegalArgumentException(
					String.format(
							"Encountered array-valued parameter binding, but was expecting [%s (%s)]",
							parameterType.getName(),
							extractName( temporalType )
					)
			);
		}

		if ( value.getClass().getComponentType().isPrimitive() ) {
			// we have a primitive array.  we validate that the actual array has the component type (type of elements)
			// we expect based on the component type of the parameter specification
			if ( !parameterType.getComponentType().isAssignableFrom( value.getClass().getComponentType() ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Primitive array-valued parameter bind value type [%s] did not match expected type [%s (%s)]",
								value.getClass().getComponentType().getName(),
								parameterType.getName(),
								extractName( temporalType )
						)
				);
			}
		}
		else {
			// we have an object array.  Here we loop over the array and physically check each element against
			// the type we expect based on the component type of the parameter specification
			final Object[] array = (Object[]) value;
			for ( Object element : array ) {
				if ( !isValidBindValue( parameterType.getComponentType(), element, temporalType ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Array-valued parameter value element [%s] did not match expected type [%s (%s)]",
									element,
									parameterType.getName(),
									extractName( temporalType )
							)
					);
				}
			}
		}
	}
}
