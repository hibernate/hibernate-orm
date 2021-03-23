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
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows specifying the specific {@link JdbcTypeDescriptor}
 * to use for a particular column mapping.  Resolved as a {@link ManagedBean}
 *
 * ````
 * @Entity
 * class User {
 *     ...
 *     @JdbcType ( MyCustomSqlIntegerDescriptor.class )
 *     int getAge() { ... }
 *
 *     @JdbcType ( MyCustomSqlVarcharDescriptor.class )
 *     String getName() { ... }
 * }
 * ````
 *
 * @apiNote Should not be used in combination with {@link JdbcTypeCode}
 *
 * @see JdbcTypeDescriptor
 * @see JdbcTypeDescriptorRegistry
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
public @interface JdbcType {
	Class<? extends JdbcTypeDescriptor> value();
}
