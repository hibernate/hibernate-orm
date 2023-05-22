/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the default fetching strategy for the annotated association,
 * or, if {@link #profile} is specified, the fetching strategy for the
 * annotated association in the named {@linkplain FetchProfile fetch profile}.
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
 * The default fetching strategy specified by this annotation may be
 * overridden in a given {@linkplain FetchProfile fetch profile}.
 * <p>
 * If {@link #profile} is specified, then the given profile name must
 * match the name of an existing fetch profile declared using the
 * {@link FetchProfile#name @FetchProfile} annotation.
 *
 * @author Emmanuel Bernard
 *
 * @see FetchMode
 * @see FetchProfile
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(Fetches.class)
public @interface Fetch {
	/**
	 * The method that should be used to fetch the association.
	 */
	FetchMode value();

	/**
	 * The name of the {@link FetchProfile fetch profile} in
	 * which this fetch mode should be applied. By default,
	 * it is applied the default fetch profile.
	 */
	String profile() default "";
}
