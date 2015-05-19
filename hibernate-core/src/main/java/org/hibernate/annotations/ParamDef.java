/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A parameter definition.
 *
 * @author Emmanuel Bernard
 */
@Target({})
@Retention(RUNTIME)
public @interface ParamDef {
	/**
	 * The name of the parameter definition.
	 */
	String name();

	/**
	 * The type being defined,  Typically this is the fully-qualified name of the {@link org.hibernate.type.Type},
	 * {@link org.hibernate.usertype.UserType} or {@link org.hibernate.usertype.CompositeUserType} implementation
	 * class.
	 */
	String type();
}
