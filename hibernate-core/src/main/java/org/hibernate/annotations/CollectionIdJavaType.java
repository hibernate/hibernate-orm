/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.usertype.UserType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specify an explicit BasicJavaDescriptor to use for a particular
 * column mapping.
 *
 * Resolved as a {@link org.hibernate.resource.beans.spi.ManagedBean}
 *
 * Can be applied in conjunction with the following sources to
 * control the mapping of a particular column in a compositional way:<ul>
 *     <li>{@link JdbcType}</li>
 *     <li>{@link JdbcTypeCode}</li>
 *     <li>{@link Mutability}</li>
 *     <li>{@link jakarta.persistence.AttributeConverter}</li>
 *     <li>{@link jakarta.persistence.Lob}</li>
 *     <li>{@link jakarta.persistence.Enumerated}</li>
 *     <li>{@link jakarta.persistence.Temporal}</li>
 *     <li>{@link Nationalized}</li>
 * </ul>
 *
 * @apiNote Mutually exclusive with {@link CustomType} which is an approach to the
 * mapping the column through the {@link UserType} contract, which performs all the
 * composed functions itself.
 *
 * @since 6.0
 */
@Inherited
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface CollectionIdJavaType {
	/**
	 * The {@link BasicJavaDescriptor} to use for the mapped column
	 */
	Class<? extends BasicJavaDescriptor<?>> value();
}
