/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.metamodel.model.relational.spi.Size;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Pluggable strategy for determining the Size to use for columns of
 * a given SQL type when no explicit Size has been given.
 *
 * Allows Dialects, integrators and users a chance to apply default
 * column size limits in certain situations based on the mapped
 * SQL and Java types.  E.g. when mapping a UUID to a VARCHAR column
 * we know the default Size should be `Size#length == 36`.
 */
public interface DefaultSizeStrategy {
	/**
	 * Resolve the default Size to use for columns of type `sqlType`.
	 *
	 * The corresponding `javaType` is passed to allow customization
	 * when mapping to attributes of that `javaType`.
	 *
	 * `null` is a valid return value indicating that no Size should
	 * be applied.
	 */
	Size resolveDefaultSize(SqlTypeDescriptor sqlType, JavaTypeDescriptor javaType);
}
