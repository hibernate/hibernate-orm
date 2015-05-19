/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
