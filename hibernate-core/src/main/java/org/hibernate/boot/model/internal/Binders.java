/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.TypeBinderType;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import java.lang.annotation.Annotation;

import static org.hibernate.internal.util.GenericsHelper.typeArguments;

/**
 * @author Gavin King
 * @since 7.3
 */
public class Binders {
	static <A extends Annotation> void callTypeBinder(
			Annotation annotation, Class<A> annotationType,
			Component embeddable,
			MetadataBuildingContext context) {
		try {
			typeBinder( annotationType )
					.bind( annotationType.cast( annotation ),
							context, embeddable );
		}
		catch (Exception e) {
			throw new AnnotationException(
					"Error processing @TypeBinderType annotation '%s' for embeddable type '%s'"
							.formatted( annotation, embeddable.getComponentClassName() ), e );
		}
	}

	static <A extends Annotation> void callTypeBinder(
			Annotation annotation, Class<A> annotationType,
			PersistentClass entity,
			MetadataBuildingContext context) {
		try {
			typeBinder( annotationType )
					.bind( annotationType.cast( annotation ),
							context, entity );
		}
		catch (Exception e) {
			throw new AnnotationException(
					"Error processing @TypeBinderType annotation '%s' for entity type '%s'"
							.formatted( annotation, entity.getClassName() ), e );
		}
	}

	static <A extends Annotation> void callPropertyBinder(
			Annotation annotation, Class<A> annotationType,
			PersistentClass entity, Property property,
			MetadataBuildingContext context) {
		try {
			propertyBinder( annotationType )
					.bind( annotationType.cast( annotation ),
							context, entity, property );
		}
		catch (Exception e) {
			throw new AnnotationException(
					"error processing @AttributeBinderType annotation '%s' for attribute '%s' of entity type '%s'"
							.formatted( annotation, property.getName(), entity.getClassName() ), e );
		}
	}

	private static <A extends Annotation> TypeBinder<A> typeBinder(Class<A> annotationType)
			throws Exception {
		final var binderType =
				annotationType.getAnnotation( TypeBinderType.class )
						.binder();
		checkImplementedTypeArgument( annotationType, binderType, TypeBinder.class );
		@SuppressWarnings("unchecked") // Safe, we just checked
		final var castBinderType = (Class<? extends TypeBinder<A>>) binderType;
		return castBinderType.getDeclaredConstructor().newInstance();
	}

	private static <A extends Annotation> AttributeBinder<A> propertyBinder(Class<A> annotationType)
					throws Exception {
		final var binderType =
				annotationType.getAnnotation( AttributeBinderType.class )
						.binder();
		checkImplementedTypeArgument( annotationType, binderType, PropertyBinder.class );
		@SuppressWarnings("unchecked") // Safe, we just checked
		final var castBinderType = (Class<? extends AttributeBinder<A>>) binderType;
		return castBinderType.getDeclaredConstructor().newInstance();
	}

	private static void checkImplementedTypeArgument(
			Class<? extends Annotation> annotationType,
			Class<?> binderType, Class<?> implementedType) {
		final var args = typeArguments( implementedType, binderType );
		if ( args.length == 1 ) {
			final var requiredAnnotationType = args[0];
			if ( annotationType != requiredAnnotationType ) {
				throw new AnnotationException(
						"Wrong kind of binder for annotation type:"
						+ " '%s' does not accept an annotation of type '%s'"
								.formatted( binderType.getTypeName(),
										annotationType.getTypeName() )
				);
			}
		}
	}
}
