/*
 * jDocBook, processing of DocBook sources
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.persister.walking.spi;

import org.hibernate.type.Type;

/**
 * Represents a collection element.
 *
 * @author Steve Ebersole
 */
public interface CollectionElementDefinition {

	/**
	 * Returns the collection definition.
	 * @return  the collection definition.
	 */
	public CollectionDefinition getCollectionDefinition();

	/**
	 * Returns the collection element type.
	 * @return the collection element type
	 */
	public Type getType();

	/**
	 * If the element type returned by {@link #getType()} is an
	 * {@link org.hibernate.type.AnyType}, then the any mapping
	 * definition for the collection element is returned;
	 * otherwise, IllegalStateException is thrown.
	 *
	 * @return the any mapping definition for the collection element.
	 *
	 * @throws IllegalStateException if the collection element type
	 * returned by {@link #getType()} is not of type
	 * {@link org.hibernate.type.AnyType}.
	 */
	public AnyMappingDefinition toAnyMappingDefinition();

	/**
	 * If the element type returned by {@link #getType()} is an
	 * {@link org.hibernate.type.EntityType}, then the entity
	 * definition for the collection element is returned;
	 * otherwise, IllegalStateException is thrown.
	 *
	 * @return the entity definition for the collection element.
	 *
	 * @throws IllegalStateException if the collection element type
	 * returned by {@link #getType()} is not of type
	 * {@link org.hibernate.type.EntityType}.
	 */
	public EntityDefinition toEntityDefinition();

	/**
	 * If the element type returned by {@link #getType()} is a
	 * {@link org.hibernate.type.CompositeType}, then the composite
	 * element definition for the collection element is returned;
	 * otherwise, IllegalStateException is thrown.
	 *
	 * @return the composite element definition for the collection element.
	 *
	 * @throws IllegalStateException if the collection element type
	 * returned by {@link #getType()} is not of type
	 * {@link org.hibernate.type.CompositeType}.
	 */
	public CompositeCollectionElementDefinition toCompositeElementDefinition();
}
