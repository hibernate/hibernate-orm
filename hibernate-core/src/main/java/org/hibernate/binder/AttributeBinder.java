/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder;

import java.lang.annotation.Annotation;

import org.hibernate.Incubating;
import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.boot.mapping.spi.AttributeApplication;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/**
 * Interprets a user-defined annotation applied to a persistent attribute and
 * customizes the corresponding boot mapping.
 * <p>
 * A custom annotation is associated with an implementation of this contract
 * using {@link AttributeBinderType @AttributeBinderType}. For each concrete
 * application of the annotated attribute, Hibernate supplies an
 * {@link AttributeBindingContext} correlating:
 * <ul>
 *     <li>the read-only categorized domain model and semantic
 *         {@link AttributeApplication}, and</li>
 *     <li>the mutable {@link PersistentClass} and {@link Property} materialized
 *         for that application.</li>
 * </ul>
 * This correlation is important for inherited, generic, and embeddable
 * attributes, where a source declaration may have multiple contextual usages
 * or concrete mapping applications.
 * <p>
 * Invocation occurs after the {@code Property} and the structural shape of its
 * value have been materialized, but before value resolution and mapping
 * finalization. Thus, a binder may customize the supplied boot mapping
 * objects, but should not assume that table keys, foreign keys, inverse
 * associations, or other later-phase products have been finalized.
 * <p>
 * For example, a binder may inspect the contextually resolved Java type while
 * changing a property option:
 * {@snippet :
 * public void bind(MyReadOnly annotation, AttributeBindingContext context) {
 *     var resolvedType = context.getAttribute().resolvedType();
 *     context.getProperty().setUpdateable(false);
 * }
 * }
 *
 * @param <A> the user-defined annotation interpreted by this binder
 *
 * @see AttributeBinderType
 * @see AttributeBindingContext
 * @see TypeBinder
 *
 * @author Gavin King
 */
@Incubating
public interface AttributeBinder<A extends Annotation> {
	/**
	 * Customizes the materialized mapping for one application of an annotated
	 * persistent attribute.
	 * <p>
	 * Mutations should be made during this callback. The semantic objects
	 * exposed by the context are read-only; the supplied mapping objects are the
	 * mutation targets.
	 *
	 * @param annotation the annotation instance discovered on the persistent
	 *                   attribute
	 * @param context the semantic and materialized views of the concrete
	 *                attribute application
	 */
	void bind(A annotation, AttributeBindingContext context);
}
