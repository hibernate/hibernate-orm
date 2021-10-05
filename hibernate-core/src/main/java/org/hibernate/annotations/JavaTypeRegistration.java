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

import org.hibernate.type.descriptor.java.BasicJavaTypeDescriptor;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers the BasicJavaDescriptor to use for the given {@link #javaType}
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@java.lang.annotation.Target({PACKAGE, TYPE, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
@Repeatable( JavaTypeRegistrations.class )
public @interface JavaTypeRegistration {
	Class<?> javaType();

	Class<? extends BasicJavaTypeDescriptor<?>> descriptorClass();
}
