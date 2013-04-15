/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A type definition.  Much like {@link Type}, but here we can centralize the definition under a name and
 * refer to that name elsewhere.
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
