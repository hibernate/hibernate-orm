/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies whether mutating the annotated attribute should trigger an increment
 * to the {@linkplain jakarta.persistence.Version version} of the entity instance.
 * Or, when {@link OptimisticLockType#ALL @OptimisticLocking(type = ALL)} or
 * {@link OptimisticLockType#DIRTY @OptimisticLocking(type = DIRTY)} is used,
 * specifies whether the annotated attribute should be included or excluded from
 * the list of checked attributes.
 * <p>
 * If this annotation is not present, mutating an attribute <em>does</em> cause
 * the version to be incremented.
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
