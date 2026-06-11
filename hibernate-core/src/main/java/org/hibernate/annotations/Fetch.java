/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Remove;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the default fetching method for the annotated association.
 * <p>
 * When this annotation is <em>not</em> explicitly specified, then:
 * <ul>
 * <li>{@linkplain FetchMode#SELECT select fetching} is used for
 *     {@linkplain jakarta.persistence.FetchType#LAZY lazy} fetching,
 *     and
 * <li>{@linkplain FetchMode#JOIN join fetching} is used for
 *     {@linkplain jakarta.persistence.FetchType#EAGER eager} fetching.
 * </ul>
 * <p>
 * The default fetching method specified by this annotation may be
 * overridden in a given {@linkplain FetchProfile fetch profile}.
 * <p>
 * Note that join fetching is incompatible with lazy fetching, and so
 * {@code @Fetch(JOIN)} implies {@code fetch=EAGER}, overriding any
 * explicitly-specified {@code fetch=LAZY} setting.
 *
 * @author Emmanuel Bernard
 *
 * @see FetchMode
 * @see FetchProfile
 *
 * @apiNote Since {@link FetchMode} is deprecated, and since its name
 *          collides with {@link jakarta.persistence.Fetch}, this
 *          annotation will eventually be deprecated and removed.
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Remove // see the @apiNote
public @interface Fetch {
	/**
	 * The method that should be used to fetch the association.
	 */
	FetchMode value();
}
