/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import java.util.Locale;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;

@Internal
public final class GeneratorTypeHelper {
	public static void checkGeneratorGeneratedType(Generator generator, GeneratorCreationContext context) {
		final var generatedType = generator.getGeneratedType();
		if ( generatedType != null ) {
			final var property = context.getProperty();
			if ( property != null ) {
				checkAssignable( generator, generatedType, context );
			}
		}
	}

	private static void checkAssignable(Generator generator, Class<?> generatedType, GeneratorCreationContext context) {
		final var attributeType = context.getType().getReturnedClass();
		if ( attributeType != null
				&& !wrap( attributeType ).isAssignableFrom( wrap( generatedType ) ) ) {
			throw new MappingException( String.format(
					Locale.ROOT,
					"Generator '%s' declares generated type '%s', which is not assignable to generated attribute '%s' of type '%s'",
					generator.getClass().getName(),
					generatedType.getTypeName(),
					attributePath( context ),
					attributeType.getTypeName()
			) );
		}
	}

	private static String attributePath(GeneratorCreationContext context) {
		final var property = context.getProperty();
		final var persistentClass = context.getPersistentClass();
		if ( persistentClass != null && property != null ) {
			return persistentClass.getEntityName() + "." + property.getName();
		}

		final var memberDetails = context.getMemberDetails();
		if ( memberDetails != null ) {
			return memberDetails.getDeclaringType().getName() + "." + memberDetails.getName();
		}

		return property == null ? "<unknown>" : property.getName();
	}

	private static Class<?> wrap(Class<?> type) {
		if ( !type.isPrimitive() ) {
			return type;
		}
		else if ( type == boolean.class ) {
			return Boolean.class;
		}
		else if ( type == byte.class ) {
			return Byte.class;
		}
		else if ( type == char.class ) {
			return Character.class;
		}
		else if ( type == double.class ) {
			return Double.class;
		}
		else if ( type == float.class ) {
			return Float.class;
		}
		else if ( type == int.class ) {
			return Integer.class;
		}
		else if ( type == long.class ) {
			return Long.class;
		}
		else if ( type == short.class ) {
			return Short.class;
		}
		else {
			return Void.class;
		}
	}

	private GeneratorTypeHelper() {
	}
}
