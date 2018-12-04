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
 * @asciidoc
 *
 * Allows specifying the SQL type code to use for a column mapping.  Must
 * resolve to a {@link org.hibernate.type.descriptor.sql.SqlTypeDescriptor}.
 *
 * The SQL type code is defined by a {@linkplain java.sql.Types JDBC Types} code
 * or a custom code.
 *
 * ````
 * @Entity
 * class User {
 *     ...
 *     // By default Hibernate maps Java's Integer to JDBC's INTEGER
 *     // but here we want to use JDBC's TINYINT instead.
 *     @SqlType( Types.TINYINT )
 *     int getAge() { ... }
 *
 *     // By default Hibernate maps Java's String to JDBC's VARCHAR
 *     // but here we want to use JDBC's NVARCHAR instead.
 *     @SqlType( Types.NVARCHAR )
 *     String getName() { ... }
 * }
 *
 * ````
 * Other forms of influencing the JDBC type used include:<ul>
 *     <li>{@link javax.persistence.Enumerated} / {@link javax.persistence.EnumType}</li>
 *     <li>{@link javax.persistence.TemporalType}</li>
 *     <li>{@link javax.persistence.Lob}</li>
 *     <li>{@link Nationalized}</li>
 *     <li>{@link SqlTypeRegistration}</li>
 * </ul>
 *
 * These forms should not be mixed on the same mapping.  The result is not defined
 *
 * @see org.hibernate.type.descriptor.sql.SqlTypeDescriptor
 * @see org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry
 * @see SqlTypeRegistration
 *
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
