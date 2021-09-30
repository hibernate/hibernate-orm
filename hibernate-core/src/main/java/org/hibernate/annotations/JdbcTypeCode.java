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
 * @asciidoc
 *
 * Allows specifying the {@link JdbcTypeDescriptor} to
 * use based on the type-code.  This might be a standard {@linkplain java.sql.Types JDBC Type}
 * code or a custom code.  Either way, there must be an entry in the
 * {@link JdbcTypeDescriptorRegistry} registered under this code
 *
 * ````
 * @Entity
 * class User {
 *     ...
 *     // By default Hibernate maps Java's Integer to JDBC's INTEGER
 *     // but here we want to use JDBC's TINYINT instead.
 *     @JdbcTypeCode ( Types.TINYINT )
 *     int getAge() { ... }
 *
 *     // By default Hibernate maps Java's String to JDBC's VARCHAR
 *     // but here we want to use JDBC's NVARCHAR instead.
 *     @JdbcTypeCode ( Types.NVARCHAR )
 *     String getName() { ... }
 * }
 * ````
 *
 * Other forms of influencing the JDBC type used include:<ul>
 *     <li>{@link jakarta.persistence.Enumerated} / {@link jakarta.persistence.EnumType}</li>
 *     <li>{@link jakarta.persistence.TemporalType}</li>
 *     <li>{@link jakarta.persistence.Lob}</li>
 *     <li>{@link Nationalized}</li>
 *     <li>{@link JdbcType}</li>
 * </ul>
 *
 * These forms should not be mixed on the same mapping.  The result is not defined
 *
 * @apiNote Should not be used in combination with the other forms of influencing the JDBC type used
 *
 * @see JdbcTypeRegistration
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
public @interface JdbcTypeCode {
	/**
	 * The standard {@linkplain java.sql.Types JDBC Types} code or a custom code.
	 * This ultimately decides which {@link JdbcTypeDescriptor}
	 * is used to "understand" the described SQL data type
	 */
	int value();
}
