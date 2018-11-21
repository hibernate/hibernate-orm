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
 * @author Steve Ebersole
 */
@Inherited
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface JavaTypeDescriptor {
	/**
	 * The JavaTypeDescriptor to use
	 */
	Class<? extends org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor> value();
}
