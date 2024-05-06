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
import java.util.Collection;
import java.util.Map;

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
 *
 * @deprecated
 * <ul>
 * <li>Use the JPA-defined {@link jakarta.persistence.FetchType#EAGER}
 *     instead of {@code LazyCollection(FALSE)}.
 * <li>Use static methods of {@link org.hibernate.Hibernate},
 *     for example {@link org.hibernate.Hibernate#size(Collection)},
 *     {@link org.hibernate.Hibernate#contains(Collection, Object)}, or
 *     {@link org.hibernate.Hibernate#get(Map, Object)} instead
 *     of {@code LazyCollection(EXTRA)}.
 * </ul>
 */
@Deprecated(since="6.2")
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyCollection {
	/**
	 * The laziness of the collection.
	 */
	LazyCollectionOption value() default LazyCollectionOption.TRUE;
}
