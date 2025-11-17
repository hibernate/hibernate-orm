/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.java.BasicJavaType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specify an explicit {@link BasicJavaType} to use for a particular
 * column mapping.  <ul>
 *     <li>
 *         When applied to a Map-valued attribute, describes the Map value. Use
 *         {@link MapKeyJavaType} to describe the key instead
 *     </li>
 *     <li>
 *         When applied to a List of array-valued attribute, describes the element. Use
 *         {@link ListIndexJavaType} to describe the index instead
 *     </li>
 *     <li>
 *         When mapping an id-bag, describes the collection element.  Use {@link CollectionIdJavaType}
 *         to describe the collection-id
 *     </li>
 *     <li>
 *         For other collection mappings, describes the elements
 *     </li>
 * </ul>
 *
 * Resolved as a {@link org.hibernate.resource.beans.spi.ManagedBean}
 *
 * See <a href="package-summary.html#basic-value-mapping">basic-value-mapping</a> for high-level discussion
 * of basic value mapping.
 *
 * @see MapKeyJavaType
 * @see CollectionIdJavaType
 * @see ListIndexJavaType
 * @see AnyKeyJavaType
 *
 * @since 6.0
 */
@Inherited
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface JavaType {
	/**
	 * The {@link BasicJavaType} to use for the mapped column
	 */
	Class<? extends BasicJavaType<?>> value();
}
