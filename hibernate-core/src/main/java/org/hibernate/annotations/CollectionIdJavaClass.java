/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the Java class to use for the {@linkplain CollectionId id} of an id-bag mapping.
 * An alternative to {@linkplain CollectionIdJavaType}.  E.g.
 *
 * <pre>
 * &#64;Bag
 * &#64;CollectionId(generator="increment")
 * &#64;CollectionIdJavaClass(Integer.class)
 * Collection&lt;Person&gt; authors;
 * </pre>
 *
 * @since 7.1
 *
 * @author Steve Ebersole
 */
@Incubating
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface CollectionIdJavaClass {
	/**
	 * The Java class to use as the collection-id.
	 */
	Class<?> idType();
}
