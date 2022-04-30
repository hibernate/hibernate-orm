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
 * Specifies a batch size for batch fetching of the annotated entity or
 * collection.
 * <p>
 * For example:
 * <pre>
 *    &#64;Entity
 *    &#64;BatchSize(size = 100)
 *    class Product {
 *        ...
 *    }
 * </pre>
 * will initialize up to 100 lazy Product entity proxies at a time, but:
 * <pre>
 *    &#64;OneToMany
 *    &#64;BatchSize(size = 5) /
 *    Set&lt;Product&gt; getProducts() { ... };
 * </pre>
 * will initialize up to 5 lazy collections of products at a time.
 *
 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_BATCH_FETCH_SIZE
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface BatchSize {
	/**
	 * Strictly positive integer.
	 */
	int size();
}
