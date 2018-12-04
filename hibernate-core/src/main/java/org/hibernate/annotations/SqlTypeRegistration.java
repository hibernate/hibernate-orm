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

import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Describes a SqlTypeDescriptor to be added to the
 * {@link org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptorRegistry}
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({TYPE, METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
@Repeatable( SqlTypeRegistrations.class )
public @interface SqlTypeRegistration {
	/**
	 * The descriptor to use
	 */
	Class<? extends SqlTypeDescriptor> value();
}
