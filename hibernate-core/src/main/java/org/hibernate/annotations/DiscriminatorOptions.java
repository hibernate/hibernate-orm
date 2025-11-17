/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;
import org.hibernate.binder.internal.DiscriminatorOptionsBinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Optional annotation used in conjunction with the JPA-defined
 * {@link jakarta.persistence.DiscriminatorColumn} annotation to
 * express Hibernate-specific discriminator properties.
 *
 * @author Hardy Ferentschik
 */
@TypeBinderType(binder = DiscriminatorOptionsBinder.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface DiscriminatorOptions {
	/**
	 * If enabled, allowed discriminator values are always explicitly
	 * enumerated in {@code select} queries, even when retrieving all
	 * instances of a root entity and its subtypes. This is useful if
	 * there are discriminator column values which do <em>not</em>
	 * map to any subtype of the root entity type.
	 * <p>
	 * This setting has the side effect of suppressing the generation
	 * of a {@code check} constraint in the DDL for the discriminator
	 * column.
	 *
	 * @return {@code true} if allowed discriminator values must always
	 *         be explicitly enumerated
	 */
	boolean force() default false;

	/**
	 * Should be {@code false} if a discriminator column is also part
	 * of a mapped composite identifier, and should not be duplicated
	 * in SQL {@code INSERT} statements.
	 */
	boolean insert() default true;
}
