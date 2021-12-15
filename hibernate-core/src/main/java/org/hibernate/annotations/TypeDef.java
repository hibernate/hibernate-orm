/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A type definition.  Much like {@link Type}, but here we can centralize the definition under a name and
 * refer to that name elsewhere.
 *
 * The plural form is {@link TypeDefs}.
 *
 * @see org.hibernate.type.Type
 * @see org.hibernate.usertype.UserType
 * @see org.hibernate.usertype.CompositeUserType
 *
 * @see Type
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Repeatable(TypeDefs.class)
public @interface TypeDef {
	/**
	 * The type name.  This is the name that would be used in other locations.
	 */
	String name() default "";

	/**
	 * The type implementation class.
	 */
	Class<?> typeClass();

	/**
	 * Name a java type for which this defined type should be the default mapping.
	 */
	Class<?> defaultForType() default void.class;

	/**
	 * Any configuration parameters for this type definition.
	 */
	Parameter[] parameters() default {};
}
