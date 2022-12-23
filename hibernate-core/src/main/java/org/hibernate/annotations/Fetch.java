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
 * Specifies the default fetching strategy for the annotated association.
 * <p>
 * When this annotation is <em>not</em> explicitly specified, then:
 * <ul>
 * <li>{@linkplain FetchMode#SELECT select fetching} is used for
 *     {@linkplain jakarta.persistence.FetchType#LAZY lazy} fetching,
 *     and
 * <li>{@linkplain FetchMode#JOIN join fetching} is used for
 *     {@linkplain jakarta.persistence.FetchType#EAGER eager} fetching.
 * </ul>
 * The default fetching strategy specified by this annotation may be
 * overridden in a given {@linkplain FetchProfile fetch profile}.
 *
 * @author Emmanuel Bernard
 *
 * @see FetchMode
 * @see FetchProfile
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Fetch {
	/**
	 * The method that should be used to fetch the association.
	 */
	FetchMode value();
}
