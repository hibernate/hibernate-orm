/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the JDBC type-code to use for the column mapping.<ul>
 *     <li>
 *         When applied to a Map-valued attribute, describes the Map value. Use
 *         {@link MapKeyJdbcTypeCode} to describe the key instead
 *     </li>
 *     <li>
 *         When applied to a List of array-valued attribute, describes the element. Use
 *         {@link ListIndexJdbcTypeCode} to describe the index instead
 *     </li>
 *     <li>
 *         When mapping an id-bag, describes the collection element.  Use {@link CollectionIdJdbcTypeCode}
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
 * <p>
 * This code is generally as one of the values defined in {@link java.sql.Types}, but are not
 * limited to these.  The code is resolved against an internal registry of {@link JdbcType}
 * references.  See the user guide for additional details.
 * <p>
 * See <a href="package-summary.html#basic-value-mapping">basic-value-mapping</a> for high-level discussion
 * of basic value mapping.
 *
 * @see JdbcType
 * @see JdbcTypeRegistry
 * @see MapKeyJdbcTypeCode
 * @see CollectionIdJdbcTypeCode
 * @see ListIndexJdbcTypeCode
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
public @interface JdbcTypeCode {
	/**
	 * The standard {@linkplain java.sql.Types JDBC Types} code or a custom code.
	 * This ultimately decides which {@link JdbcType} is used to "understand" the
	 * described SQL data type.
	 */
	int value();
}
