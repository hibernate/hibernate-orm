/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows specifying a pattern to be applied to the naming of columns for
 * a particular {@linkplain jakarta.persistence.Embedded embedded mapping}.
 * For example, given a typical embeddable named {@code Address} and
 * {@code @Embedded @EmbeddedColumnNaming("home_%s)}, we will get columns named
 * {@code home_street}, {@code home_city}, etc.
 *
 * @since 7.0
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Incubating
public @interface EmbeddedColumnNaming {
	/**
	 * The naming pattern.  It is expected to contain a single pattern marker ({@code %})
	 * into which the "raw" column name will be injected.
	 */
	String value();
}
