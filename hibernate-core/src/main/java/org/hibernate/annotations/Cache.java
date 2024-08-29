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
 * <li>an appropriate {@linkplain #usage cache concurrency policy},
 *     given the expected data access patterns affecting the entity
 *     or collection.
 * </ul>
 * <p>
 * This annotation should always be used in preference to the less
 * useful JPA-defined annotation {@link jakarta.persistence.Cacheable},
 * since JPA provides no means to specify anything about the semantics
 * of the cache. Alternatively, it's legal, but redundant, for the two
 * annotations to be used together.
 * <p>
 * Note that entity subclasses of a root entity with a second-level
 * cache inherit the cache belonging to the root entity.
 * <p>
 * For example, the following entity is eligible for caching:
 * <pre>
 * &#64;Entity
 * &#64;Cache(usage = NONSTRICT_READ_WRITE)
 * public static class Person { ... }
 * </pre>
 * <p>
 * Similarly, this collection is cached:
 * <pre>
 * &#64;OneToMany(mappedBy = "person")
 * &#64;Cache(usage = NONSTRICT_READ_WRITE)
 * private List&lt;Phone&gt; phones = new ArrayList&lt;&gt;();
 * </pre>
 * <p>
 * Note that the second-level cache is disabled unless
 * {@value org.hibernate.cfg.AvailableSettings#CACHE_REGION_FACTORY}
 * is explicitly specified, and so, by default, this annotation has
 * no effect.
 *
 * @see jakarta.persistence.Cacheable
 * @see org.hibernate.Cache
 * @see org.hibernate.cfg.AvailableSettings#CACHE_REGION_FACTORY
 * @see org.hibernate.cfg.AvailableSettings#USE_SECOND_LEVEL_CACHE
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface Cache {
	/**
	 * The appropriate {@linkplain CacheConcurrencyStrategy concurrency
	 * policy} for the annotated root entity or collection.
	 */
	CacheConcurrencyStrategy usage();

	/**
	 * The cache region name.
	 */
	String region() default "";

	/**
	 * When bytecode enhancement is used, and {@linkplain LazyGroup
	 * field-level lazy fetching} is enabled, specifies whether lazy
	 * attributes of the entity are eligible for inclusion in the
	 * second-level cache, in the case where they happen to be loaded.
	 * <p>
	 * By default, a loaded lazy field <em>will</em> be cached when
	 * second-level caching is enabled. If this is not desirable&mdash;if,
	 * for example, the field value is extremely large and only rarely
	 * accessed&mdash;then setting {@code @Cache(includeLazy=false)} will
	 * prevent it and other lazy fields of the annotated entity from being
	 * cached, and the lazy fields will always be retrieved directly from
	 * the database.
	 *
	 * @see LazyGroup
	 */
	boolean includeLazy() default true;

	/**
	 * When bytecode enhancement is used, and {@linkplain LazyGroup
	 * field-level lazy fetching} is enabled, specifies which attributes
	 * of the entity are included in the second-level cache, either:
	 * <ul>
	 * <li>{@code "all"} properties, the default, or
	 * <li>only {@code "non-lazy"} properties.
	 * </ul>
	 *
	 * @deprecated Use {@link #includeLazy()} for the sake of typesafety.
	 */
	@Deprecated(since="6.4")
	String include() default "all";
}
