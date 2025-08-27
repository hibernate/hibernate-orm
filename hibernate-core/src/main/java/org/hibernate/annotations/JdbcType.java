/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies an explicit {@link org.hibernate.type.descriptor.jdbc.JdbcType} to use for a particular column mapping.<ul>
 *     <li>
 *         When applied to a Map-valued attribute, describes the Map value. Use
 *         {@link MapKeyJdbcType} to describe the key instead
 *     </li>
 *     <li>
 *         When applied to a List of array-valued attribute, describes the element. Use
 *         {@link ListIndexJdbcType} to describe the index instead
 *     </li>
 *     <li>
 *         When mapping an id-bag, describes the collection element.  Use {@link CollectionIdJdbcType}
 *         to describe the collection-id
 *     </li>
 *     <li>
 *         For other collection mappings, describes the elements
 *     </li>
 *     <li>
 *         For discriminated association mappings ({@link Any @Any} and {@link ManyToAny @ManyToAny}), describes the
 *         discriminator value.
 *     </li>
 * </ul>
 *
 * Resolved as a {@link org.hibernate.resource.beans.spi.ManagedBean}
 *
 * See <a href="package-summary.html#basic-value-mapping">basic-value-mapping</a>  for high-level discussion
 * of basic value mapping.
 *
 * @see MapKeyJdbcType
 * @see CollectionIdJdbcType
 * @see ListIndexJdbcType
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
public @interface JdbcType {
	/**
	 * The {@link org.hibernate.type.descriptor.jdbc.JdbcType} to use for the mapped column
	 */
	Class<? extends org.hibernate.type.descriptor.jdbc.JdbcType> value();
}
