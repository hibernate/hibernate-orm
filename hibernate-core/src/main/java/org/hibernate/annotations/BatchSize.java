/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.binder.internal.BatchSizeBinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a maximum batch size for batch fetching of the annotated
 * entity or collection.
 * <p>
 * When batch fetching is enabled, Hibernate is able to fetch multiple
 * instances of an entity or collection in a single round trip to the
 * database. Instead of a SQL {@code select} with just one primary key
 * value in the {@code where} clause, the {@code where} clause contains
 * a list of primary keys inside a SQL {@code in} condition. The primary
 * key values to batch fetch are chosen from among the identifiers of
 * unfetched entity proxies or collection roles associated with the
 * session.
 * <p>
 * For example:
 * <pre>
 *    &#64;Entity
 *    &#64;BatchSize(size = 100)
 *    class Product {
 *        ...
 *    }
 * </pre>
 * <p>
 * will initialize up to 100 unfetched {@code Product} proxies in each
 * trip to the database.
 * <p>
 * Similarly:
 * <pre>
 *    &#64;OneToMany
 *    &#64;BatchSize(size = 5) /
 *    Set&lt;Product&gt; getProducts() { ... };
 * </pre>
 * <p>
 * will initialize up to 5 unfetched collections of {@code Product}s in
 * each SQL {@code select}.
 *
 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_BATCH_FETCH_SIZE
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@AttributeBinderType(binder = BatchSizeBinder.class)
@TypeBinderType(binder = BatchSizeBinder.class)
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface BatchSize {
	/**
	 * The maximum batch size, a strictly positive integer.
	 * <p/>
	 * Default is defined by {@link org.hibernate.cfg.FetchSettings#DEFAULT_BATCH_FETCH_SIZE}
	 */
	int size();
}
