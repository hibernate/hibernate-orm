/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import org.hibernate.type.descriptor.java.BasicJavaDescriptor;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link JavaType} used to describe the foreign-key part of an ANY mapping.
 *
 * @see Any
 * @see AnyKeyJavaClass
 */
@java.lang.annotation.Target({METHOD, FIELD})
@Retention( RUNTIME )
public @interface AnyKeyJavaType {
	Class<? extends BasicJavaDescriptor<?>> value();
}
