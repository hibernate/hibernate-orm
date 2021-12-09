/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.tuple.AttributeBinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Associates a user-defined annotation with an {@link AttributeBinder},
 * allowing the annotation to drive some custom model binding.
 * <p>
 * The user-defined annotation may be used to annotate fields and
 * properties of entity and embeddable classes. The {@code AttributeBinder}
 * will be called when the annotation is discovered by Hibernate.
 *
 * @author Gavin King
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Incubating
public @interface AttributeBinderType {
	/**
	 * @return a type which implements {@link AttributeBinder}
	 */
	Class<? extends AttributeBinder<?>> binder();
}
