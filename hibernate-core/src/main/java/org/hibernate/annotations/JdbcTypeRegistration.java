/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describes a SqlTypeDescriptor to be added to the
 * {@link JdbcTypeDescriptorRegistry}
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@java.lang.annotation.Target({PACKAGE, TYPE, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
@Repeatable( JdbcTypeRegistrations.class )
public @interface JdbcTypeRegistration {
	/**
	 * The descriptor to register
	 */
	Class<? extends JdbcTypeDescriptor> value();

	/**
	 * The type-code under which to register this descriptor.  Can either add a new descriptor
	 * or override an existing one.
	 *
	 * By default we will use {@link JdbcTypeDescriptor#getJdbcTypeCode}
	 */
	int registrationCode() default Integer.MIN_VALUE;
}
