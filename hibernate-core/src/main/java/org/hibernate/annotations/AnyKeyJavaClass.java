/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the Java Class to use for the foreign-key handling related to an ANY mapping.
 *
 * The specified class is resolved to a {@link org.hibernate.type.descriptor.java.BasicJavaDescriptor}
 * via the {@link org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry}
 *
 * @see Any
 * @see AnyKeyJavaType
 */
@java.lang.annotation.Target({METHOD, FIELD})
@Retention( RUNTIME )
public @interface AnyKeyJavaClass {
	/**
	 * The Java Class
	 */
	Class<?> value();
}
