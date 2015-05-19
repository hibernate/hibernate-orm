/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a Hibernate type mapping.
 *
 * @see org.hibernate.type.Type
 * @see org.hibernate.usertype.UserType
 * @see org.hibernate.usertype.CompositeUserType
 *
 * @see TypeDef
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface Type {
	/**
	 * The Hibernate type name.  Usually the fully qualified name of an implementation class for
	 * {@link org.hibernate.type.Type}, {@link org.hibernate.usertype.UserType} or
	 * {@link org.hibernate.usertype.CompositeUserType}.  May also refer to a type definition by name
	 * {@link TypeDef#name()}
	 */
	String type();

	/**
	 * Any configuration parameters for the named type.
	 */
	Parameter[] parameters() default {};
}
