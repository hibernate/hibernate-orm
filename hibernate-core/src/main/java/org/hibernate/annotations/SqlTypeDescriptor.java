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
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows specifying the specific {@link org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor}
 * to use for a particular column mapping.  Can reference a {@link ManagedBean} - see
 * {@link ManagedBeanRegistry}
 *
 * @apiNote Should not be used in combination with {@link SqlType}
 *
 * @see org.hibernate.type.descriptor.sql.SqlTypeDescriptor
 * @see org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
public @interface SqlTypeDescriptor {
	Class<? extends org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor> value();

	String columnName() default "";
}
