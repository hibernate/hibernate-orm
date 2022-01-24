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

import org.hibernate.type.descriptor.java.BasicJavaType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers the BasicJavaType to use for the given {@link #javaType}
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
 * @since 6.0
 */
@java.lang.annotation.Target({PACKAGE, TYPE, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
@Repeatable( JavaTypeRegistrations.class )
public @interface JavaTypeRegistration {
	Class<?> javaType();

	Class<? extends BasicJavaType<?>> descriptorClass();
}
