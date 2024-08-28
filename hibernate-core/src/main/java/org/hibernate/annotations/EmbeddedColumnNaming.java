/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows specifying a pattern to be applied to the naming of columns for
 * a particular {@linkplain jakarta.persistence.Embedded embedded mapping}.
 *
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface EmbeddedColumnNaming {
	/**
	 * The naming pattern.  It is expected to contain a single pattern marker ({@code %})
	 * into which the "raw" column name will be injected.  E.g., given a typical {@code Address}
	 * embeddable and {@code @Embedded @EmbeddedColumnNaming("home_%s)}, we will get columns named
	 * {@code home_street}, {@code home_city}, etc.
	 */
	String value();
}
