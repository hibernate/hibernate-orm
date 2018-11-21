/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows specifying the SQL type code to use for a column mapping.  Must
 * resolve to a {@link org.hibernate.type.descriptor.sql.SqlTypeDescriptor}.
 *
 * The SQL type code is defined by a {@linkplain java.sql.Types JDBC Types} code
 * or a custom code.
 *
 * @apiNote Should not be used in combination with {@link SqlTypeDescriptor}
 *
 * @see org.hibernate.type.descriptor.sql.SqlTypeDescriptor
 * @see org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry
 *
 * @SqlTypeDef( 2001, MySqlTypeDescriptor.class )
 *
 * @Entity
 * class Person {
 * 	   @Basic
 *     @SqlType(2001)
 *     String getName() {...}
 * }
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
public @interface SqlType {
	/**
	 * The standard {@linkplain java.sql.Types JDBC Types} code or a custom code.
	 * This ultimately decides which {@link org.hibernate.type.descriptor.sql.SqlTypeDescriptor}
	 * is used to "understand" the described SQL data type
	 */
	int value();
}
