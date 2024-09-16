/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies whether mutating the annotated attribute should trigger an increment
 * to the {@link jakarta.persistence.Version version} of the entity instance. Or,
 * if {@link OptimisticLockType#ALL} or {@link OptimisticLockType#DIRTY} are used,
 * specifies whether the attribute should be included or excluded from the list of
 * checked attributes.
 * <p>
 * If this annotation is not present, mutating an attribute <em>does</em> cause the
 * version to be incremented.
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
