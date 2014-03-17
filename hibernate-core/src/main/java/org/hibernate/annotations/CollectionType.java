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
 * Names a custom collection type for a persistent collection.  The collection
 * can also name a @Type, which defines the Hibernate Type of the collection
 * elements.  If the collection is a Map, can als use @MapKeyType to define the
 * Hibernate Type of the map key.
 *
 * @see org.hibernate.type.CollectionType
 * @see org.hibernate.usertype.UserCollectionType
 * @see org.hibernate.annotations.Type
 * @see org.hibernate.annotations.MapKeyType
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface CollectionType {
	/**
	 * Name of the collection type to use.
	 *
	 * Could name either:<ul>
	 *     <li>the implementation class (an implementation of {@link org.hibernate.type.CollectionType}</li>
	 *     <li>the implementation class (an implementation of {@link org.hibernate.usertype.UserCollectionType}</li>
	 *     <li>a type defined via a {@link TypeDef @TypeDef}</li>
	 * </ul>
	 */
	String type();

	/**
	 * Specifies configuration information for the type.  Note that if the named
	 * type is a {@link org.hibernate.usertype.UserCollectionType}, it must also
	 * implement {@link org.hibernate.usertype.ParameterizedType} in order to
	 * receive these values.
	 */
	Parameter[] parameters() default {};
}
