/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies whether updating the annotated attribute should trigger an increment
 * to the {@link jakarta.persistence.Version version} of the entity instance.
 * <p>
 * If this annotation is not present, updating an attribute does cause the version
 * to be incremented.
 *
 * @author Logi Ragnarsson
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OptimisticLock {
	/**
	 * {@code true} if changing the annotated attribute should <em>not</em> cause
	 * the version to be incremented.
	 */
	boolean excluded();
}
