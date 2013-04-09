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
package org.hibernate;
import java.util.Properties;

import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

/**
 * Provides access to the various {@link Type} instances associated with the {@link SessionFactory}.
 * <p/>
 * This is intended for use by application developers.
 *
 * @author Steve Ebersole
 */
public interface TypeHelper {
	/**
	 * Retrieve the basic type registered against the given name.
	 *
	 * @param name The name of the basic type to retrieve
	 *
	 * @return The basic type, or null.
	 */
	public BasicType basic(String name);

	/**
	 * Convenience form of {@link #basic(String)}.  The intended use of this is something like
	 * {@code basic(Integer.class)} or {@code basic(int.class)}
	 *
	 * @param javaType The java type for which to retrieve the type instance.
	 *
	 * @return The basic type, or null.
	 */
	public BasicType basic(Class javaType);

	/**
	 * Uses heuristics to deduce the proper {@link Type} given a string naming the type or Java class.
	 * <p/>
	 * See {@link org.hibernate.type.TypeResolver#heuristicType(java.lang.String)} for a discussion of the
	 * heuristic algorithm.
	 *
	 * @param name The name of the type or Java class
	 *
	 * @return The deduced type, or null.
	 *
	 * @see org.hibernate.type.TypeResolver#heuristicType(java.lang.String)
	 */
	public Type heuristicType(String name);

	/**
	 * Retrieve a type representing the given entity.
	 *
	 * @param entityClass The entity Java type.
	 *
	 * @return The type, or null
	 */
	public Type entity(Class entityClass);

	/**
	 * Retrieve a type representing the given entity.
	 *
	 * @param entityName The entity name.
	 *
	 * @return The type, or null
	 */
	public Type entity(String entityName);

	/**
	 * Retrieve the type for the given user-type class ({@link org.hibernate.usertype.UserType} or
	 * {@link org.hibernate.usertype.CompositeUserType}).
	 *
	 * @param userTypeClass The user type class
	 *
	 * @return The type, or null
	 */
	public Type custom(Class userTypeClass);

	/**
	 * Retrieve the type for the given user-type class ({@link org.hibernate.usertype.UserType} or
	 * {@link org.hibernate.usertype.CompositeUserType}).
	 *
	 * @param userTypeClass The user type class
	 * @param properties Configuration properties.
	 *
	 * @return The type, or null
	 */
	public Type custom(Class userTypeClass, Properties properties);

	/**
	 * Retrieve the type representing an ANY mapping.
	 *
	 * @param metaType The meta type for the ANY
	 * @param identifierType The identifier type for the ANY
	 *
	 * @return The type, or null
	 */
	public Type any(Type metaType, Type identifierType);
}
