/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the persistence operations that should cascade
 * to associated entity instances.
 * <p>
 * This annotation competes with the {@code cascade} member
 * of JPA association mapping annotations, for example, with
 * {@link jakarta.persistence.OneToMany#cascade()}. The only
 * good reason to use it over the standard JPA approach is
 * to enable {@linkplain CascadeType#LOCK lock cascading},
 * by writing, for example, {@code @Cascade(LOCK)}.
 * <p>
 * If an association specified cascade types using both the
 * JPA association mapping annotations and this annotation,
 * then the cascade types from the two sources are aggregated.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 *
 * @deprecated Use the JPA-defined
 *             {@link jakarta.persistence.CascadeType}
 */
@Deprecated(since = "7", forRemoval = true)
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Cascade {
	/**
	 * The operations that should be cascaded.
	 */
	CascadeType[] value();
}
