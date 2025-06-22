/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.binder.TypeBinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Associates a user-defined annotation with a {@link TypeBinder},
 * allowing the annotation to drive some custom model binding.
 * <p>
 * The user-defined annotation may be used to annotate entity and
 * embeddable classes. The {@code TypeBinder} will be called when
 * the annotation is discovered by Hibernate.
 *
 * @author Gavin King
 *
 * @see AttributeBinderType
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Incubating
public @interface TypeBinderType {
	/**
	 * @return a type which implements {@link TypeBinder}
	 */
	Class<? extends TypeBinder<?>> binder();
}
