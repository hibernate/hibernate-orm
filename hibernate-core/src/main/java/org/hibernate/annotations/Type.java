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
