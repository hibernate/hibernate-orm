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

import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describes a SqlTypeDescriptor to be added to the {@link JdbcTypeRegistry}
 * <p/>
 * Registrations applied to a package are processed before Hibernate begins to process
 * any attributes, etc.
 * <p/>
 * Registrations applied to a class are only applied once Hibernate begins to process
 * that class; it will also affect all future processing.  However, it will not change
 * previous resolutions to use this newly registered one.  Because of this randomness
 * it is recommended to only apply registrations to packages or to use a
 * {@link org.hibernate.boot.model.TypeContributor}.
 *
 * @see org.hibernate.boot.model.TypeContributor
 *
 * @since 6.0
 */
@java.lang.annotation.Target({PACKAGE, TYPE})
@Inherited
@Retention(RUNTIME)
@Repeatable( JdbcTypeRegistrations.class )
public @interface JdbcTypeRegistration {
	/**
	 * The descriptor to register
	 */
	Class<? extends JdbcType> value();

	/**
	 * The type-code under which to register this descriptor.  Can either add a new descriptor
	 * or override an existing one.
	 *
	 * By default we will use {@link JdbcType#getJdbcTypeCode}
	 */
	int registrationCode() default Integer.MIN_VALUE;
}
