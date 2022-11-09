/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a root entity or collection for second-level caching, and
 * specifies:
 * <ul>
 * <li>a {@linkplain #region named cache region} in which to store
 *     the state of instances of the entity or collection, and
 * <li>an appropriate {@linkplain #usage cache concurrency strategy},
 *     given the expected data access patterns affecting the entity
 *     or collection.
 * </ul>
 * This annotation should always be used in preference to the less
 * useful JPA-defined annotation {@link jakarta.persistence.Cacheable},
 * since JPA provides no means to specify anything about the semantics
 * of the cache. Alternatively, it's legal, but redundant, for the two
 * annotations to be used together.
 * <p>
 * Note that entity subclasses of a root entity with a second-level
 * cache inherit the cache belonging to the root entity.
 *
 * @see jakarta.persistence.Cacheable
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Cache {
	/**
	 * The appropriate concurrency strategy for the annotated root
	 * entity or collection.
	 */
	CacheConcurrencyStrategy usage();

	/**
	 * The cache region name.
	 */
	String region() default "";

	/**
	 * Specifies which properties are included in the second-level
	 * cache, either:
	 * <ul>
	 * <li>{@code "all"} properties, the default, or
	 * <li>only {@code "non-lazy"} properties.
	 * </ul>
	 */
	String include() default "all";
}
