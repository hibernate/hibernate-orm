/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies an explicit {@link JdbcTypeDescriptor} to use for
 * a particular column mapping.
 *
 * Resolved as a {@link ManagedBean}
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
 * @since 6.0
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
public @interface CollectionIdJdbcType {
	/**
	 * The {@link JdbcTypeDescriptor} to use for the mapped column
	 */
	Class<? extends JdbcTypeDescriptor> value();
}
