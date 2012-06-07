/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) ${year}, Red Hat Inc. or third-party contributors as
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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Names a custom collection type for a persistent collection.
 *
 * @see org.hibernate.type.CollectionType
 * @see org.hibernate.usertype.UserCollectionType
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface CollectionType {
	/**
	 * Names the type (either {@link org.hibernate.type.CollectionType} or
	 * {@link org.hibernate.usertype.UserCollectionType} implementation class.  Could also name a
	 * custom type defined via a {@link TypeDef @TypeDef}
	 * 
	 * @return The implementation class to use.
	 */
	String type();

	/**
	 * Specifies configuration information for the type.  Note that if the named type is a
	 * {@link org.hibernate.usertype.UserCollectionType}, it must also implement 
	 * {@link org.hibernate.usertype.ParameterizedType} in order to receive these values.
	 *
	 * @return The configuration parameters.
	 */
	Parameter[] parameters() default {};
}
