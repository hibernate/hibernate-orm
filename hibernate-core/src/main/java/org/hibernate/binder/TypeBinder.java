/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder;

import java.lang.annotation.Annotation;

import org.hibernate.Incubating;
import org.hibernate.annotations.TypeBinderType;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;

/**
 * Interprets a user-defined annotation applied to an entity or embeddable type
 * and customizes the corresponding boot mapping.
 * <p>
 * A custom annotation is associated with an implementation of this contract
 * using {@link TypeBinderType @TypeBinderType}. Hibernate invokes the
 * entity overload with an {@link EntityBindingContext} and the embeddable
 * overload with an {@link EmbeddableBindingContext}. These contexts correlate
 * read-only categorized type information with the mutable
 * {@link PersistentClass} or {@link Component} produced from it.
 * <p>
 * Entity binders run after the managed-type structure has been materialized.
 * Embeddable binders run once for each concrete embeddable usage, after its
 * component structure has been materialized. Both callbacks occur before
 * later value resolution and mapping finalization; a binder should not assume
 * that table keys, foreign keys, inverse associations, or other later-phase
 * products have been finalized.
 * <p>
 * An implementation only needs to override the target kinds supported by its
 * annotation. Each default method reports an unsupported placement using the
 * standard diagnostic supplied by its context.
 *
 * @param <A> the user-defined annotation interpreted by this binder
 *
 * @see TypeBinderType
 * @see EntityBindingContext
 * @see EmbeddableBindingContext
 * @see AttributeBinder
 *
 * @author Gavin King
 */
@Incubating
public interface TypeBinder<A extends Annotation> {
	/**
	 * Customizes the materialized mapping of an annotated entity.
	 *
	 * @param annotation the annotation instance discovered on the entity type
	 * @param context the categorized entity and its mutable boot mapping
	 *
	 * @throws org.hibernate.AnnotationException by default, indicating that the
	 *         annotation is not supported on an entity
	 */
	default void bind(A annotation, EntityBindingContext context) {
		context.unsupportedAnnotationPlacement( annotation );
	}

	/**
	 * Customizes one materialized usage of an annotated embeddable type.
	 *
	 * @param annotation the annotation instance discovered on the embeddable
	 *                   type
	 * @param context the categorized embeddable usage and its mutable component
	 *                mapping
	 *
	 * @throws org.hibernate.AnnotationException by default, indicating that the
	 *         annotation is not supported on an embeddable
	 */
	default void bind(A annotation, EmbeddableBindingContext context) {
		context.unsupportedAnnotationPlacement( annotation );
	}
}
