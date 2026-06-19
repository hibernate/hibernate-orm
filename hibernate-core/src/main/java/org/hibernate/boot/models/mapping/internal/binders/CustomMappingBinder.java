/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.binders;

import java.lang.annotation.Annotation;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.TypeBinderType;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.models.mapping.internal.context.BindingContext;
import org.hibernate.boot.models.mapping.internal.context.BindingState;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import static org.hibernate.internal.util.GenericsHelper.typeArguments;

/**
 * Invokes user-defined mapping binders declared through Hibernate annotations.
 */
public class CustomMappingBinder {
	static void callTypeBinders(
			ClassDetails classDetails,
			PersistentClass persistentClass,
			BindingState bindingState,
			BindingContext bindingContext) {
		final MetadataBuildingContext metadataBuildingContext = bindingState.getMetadataBuildingContext();
		for ( var metaAnnotated : classDetails.getMetaAnnotated(
				TypeBinderType.class,
				bindingContext.getBootstrapContext().getModelsContext()
		) ) {
			callTypeBinder( metaAnnotated, metaAnnotated.annotationType(), persistentClass, metadataBuildingContext );
		}
	}

	static void callTypeBinders(
			ClassDetails classDetails,
			Component component,
			BindingState bindingState,
			BindingContext bindingContext) {
		final MetadataBuildingContext metadataBuildingContext = bindingState.getMetadataBuildingContext();
		for ( var metaAnnotated : classDetails.getMetaAnnotated(
				TypeBinderType.class,
				bindingContext.getBootstrapContext().getModelsContext()
		) ) {
			callTypeBinder( metaAnnotated, metaAnnotated.annotationType(), component, metadataBuildingContext );
		}
	}

	public static void callAttributeBinders(
			MemberDetails member,
			PersistentClass persistentClass,
			Property property,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( persistentClass == null ) {
			return;
		}

		final MetadataBuildingContext metadataBuildingContext = bindingState.getMetadataBuildingContext();
		for ( var metaAnnotated : member.getMetaAnnotated(
				AttributeBinderType.class,
				bindingContext.getBootstrapContext().getModelsContext()
		) ) {
			if ( metaAnnotated.annotationType() == TenantId.class
					|| metaAnnotated.annotationType() == Collate.class ) {
				continue;
			}
			callAttributeBinder( metaAnnotated, metaAnnotated.annotationType(), persistentClass, property, metadataBuildingContext );
		}
	}

	private static <A extends Annotation> void callTypeBinder(
			Annotation annotation,
			Class<A> annotationType,
			PersistentClass persistentClass,
			MetadataBuildingContext metadataBuildingContext) {
		try {
			typeBinder( annotationType ).bind(
					annotationType.cast( annotation ),
					metadataBuildingContext,
					persistentClass
			);
		}
		catch (Exception e) {
			throw new AnnotationException(
					"Error processing @TypeBinderType annotation '%s' for entity type '%s'"
							.formatted( annotation, persistentClass.getClassName() ),
					e
			);
		}
	}

	private static <A extends Annotation> void callTypeBinder(
			Annotation annotation,
			Class<A> annotationType,
			Component component,
			MetadataBuildingContext metadataBuildingContext) {
		try {
			typeBinder( annotationType ).bind(
					annotationType.cast( annotation ),
					metadataBuildingContext,
					component
			);
		}
		catch (Exception e) {
			throw new AnnotationException(
					"Error processing @TypeBinderType annotation '%s' for embeddable type '%s'"
							.formatted( annotation, component.getComponentClassName() ),
					e
			);
		}
	}

	private static <A extends Annotation> void callAttributeBinder(
			Annotation annotation,
			Class<A> annotationType,
			PersistentClass persistentClass,
			Property property,
			MetadataBuildingContext metadataBuildingContext) {
		try {
			attributeBinder( annotationType ).bind(
					annotationType.cast( annotation ),
					metadataBuildingContext,
					persistentClass,
					property
			);
		}
		catch (Exception e) {
			throw new AnnotationException(
					"Error processing @AttributeBinderType annotation '%s' for attribute '%s' of entity type '%s'"
							.formatted( annotation, property.getName(), persistentClass.getClassName() ),
					e
			);
		}
	}

	private static <A extends Annotation> TypeBinder<A> typeBinder(Class<A> annotationType)
			throws ReflectiveOperationException {
		final var binderType = annotationType.getAnnotation( TypeBinderType.class ).binder();
		checkImplementedTypeArgument( annotationType, binderType, TypeBinder.class );
		@SuppressWarnings("unchecked")
		final Class<? extends TypeBinder<A>> castBinderType = (Class<? extends TypeBinder<A>>) binderType;
		return castBinderType.getDeclaredConstructor().newInstance();
	}

	private static <A extends Annotation> org.hibernate.binder.AttributeBinder<A> attributeBinder(Class<A> annotationType)
			throws ReflectiveOperationException {
		final var binderType = annotationType.getAnnotation( AttributeBinderType.class ).binder();
		checkImplementedTypeArgument( annotationType, binderType, org.hibernate.binder.AttributeBinder.class );
		@SuppressWarnings("unchecked")
		final Class<? extends org.hibernate.binder.AttributeBinder<A>> castBinderType =
				(Class<? extends org.hibernate.binder.AttributeBinder<A>>) binderType;
		return castBinderType.getDeclaredConstructor().newInstance();
	}

	private static void checkImplementedTypeArgument(
			Class<? extends Annotation> annotationType,
			Class<?> binderType,
			Class<?> implementedType) {
		final var typeArguments = typeArguments( implementedType, binderType );
		if ( typeArguments.length == 1 ) {
			final var requiredAnnotationType = typeArguments[0];
			if ( annotationType != requiredAnnotationType ) {
				throw new AnnotationException(
						"Wrong kind of binder for annotation type: '%s' does not accept an annotation of type '%s'"
								.formatted( binderType.getTypeName(), annotationType.getTypeName() )
				);
			}
		}
	}

	private CustomMappingBinder() {
	}
}
