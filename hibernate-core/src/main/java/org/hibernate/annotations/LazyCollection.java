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
 * Specify the laziness of a collection, either a
 * {@link jakarta.persistence.OneToMany} or
 * {@link jakarta.persistence.ManyToMany} association,
 * or an {@link jakarta.persistence.ElementCollection}.
 * This is an alternative to specifying the JPA
 * {@link jakarta.persistence.FetchType}. This annotation
 * is used to enable {@linkplain LazyCollectionOption#EXTRA
 * extra-lazy collection fetching}.
 *
 * @author Emmanuel Bernard
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyCollection {
	/**
	 * The laziness of the collection.
	 */
	LazyCollectionOption value() default LazyCollectionOption.TRUE;
}
