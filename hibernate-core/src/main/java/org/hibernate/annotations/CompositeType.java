/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import org.hibernate.usertype.CompositeUserType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Applies a custom {@link CompositeUserType} for the attribute mapping.
 */
@java.lang.annotation.Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface CompositeType {

	/**
	 * The custom type implementor class
	 */
	Class<? extends CompositeUserType<?>> value();
}
