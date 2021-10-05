/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the JDBC type-code to use for the column mapping.
 *
 * This code is generally one of the values defined in
 * {@link java.sql.Types}, but are not limited to those.  See the
 * user-guide for additional details.
 *
 * The code is resolved against an internal registry of
 * {@link JdbcTypeDescriptor} references.
 *
 * Can be applied in conjunction with the following sources to
 * control the mapping of a particular column in a compositional way:<ul>
 *     <li>{@link JavaType}</li>
 *     <li>{@link Mutability}</li>
 *     <li>{@link jakarta.persistence.AttributeConverter}</li>
 *     <li>{@link jakarta.persistence.Enumerated}</li>
 *     <li>{@link jakarta.persistence.Temporal}</li>
 * </ul>
 *
 * Should not be used with some forms of influencing the JDBC type used:<ul>
 *     <li>{@link JavaType}</li>
 *     <li>{@link jakarta.persistence.Lob}</li>
 *     <li>{@link Nationalized}</li>
 * </ul>
 *
 * @see JdbcTypeDescriptor
 * @see JdbcTypeDescriptorRegistry
 *
 * @since 6.0
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
public @interface ListIndexJdbcTypeCode {
	/**
	 * The standard {@linkplain java.sql.Types JDBC Types} code or a custom code.
	 * This ultimately decides which {@link JdbcTypeDescriptor}
	 * is used to "understand" the described SQL data type
	 */
	int value();
}
