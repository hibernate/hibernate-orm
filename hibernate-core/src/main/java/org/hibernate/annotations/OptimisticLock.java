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
 * Whether or not a change of the annotated property will trigger an entity version increment.
 *
 * If the annotation is not present, the property is involved in the optimistic lock strategy (default).
 *
 * @author Logi Ragnarsson
 */
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OptimisticLock {
	/**
	 * Whether the annotated property should be included in optimistic locking determinations for the owner.
	 */
	boolean excluded();
}
