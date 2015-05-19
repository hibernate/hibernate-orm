/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.spi;

import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public interface CollectionIndexDefinition {
	/**
	 * Returns the collection definition.
	 * @return  the collection definition.
	 */
	public CollectionDefinition getCollectionDefinition();
	/**
	 * Returns the collection index type.
	 * @return the collection index type
	 */
	public Type getType();
	/**
	 * If the index type returned by {@link #getType()} is an
	 * {@link org.hibernate.type.EntityType}, then the entity
	 * definition for the collection index is returned;
	 * otherwise, IllegalStateException is thrown.
	 *
	 * @return the entity definition for the collection index.
	 *
	 * @throws IllegalStateException if the collection index type
	 * returned by {@link #getType()} is not of type
	 * {@link org.hibernate.type.EntityType}.
	 */
	public EntityDefinition toEntityDefinition();
	/**
	 * If the index type returned by {@link #getType()} is a
	 * {@link org.hibernate.type.CompositeType}, then the composite
	 * index definition for the collection index is returned;
	 * otherwise, IllegalStateException is thrown.
	 *
	 * @return the composite index definition for the collection index.
	 *
	 * @throws IllegalStateException if the collection index type
	 * returned by {@link #getType()} is not of type
	 * {@link org.hibernate.type.CompositeType}.
	 */
	public CompositionDefinition toCompositeDefinition();

	/**
	 * If the index type returned by {@link #getType()} is an
	 * {@link org.hibernate.type.AnyType}, then the any mapping
	 * definition for the collection index is returned;
	 * otherwise, IllegalStateException is thrown.
	 *
	 * @return the any mapping definition for the collection index.
	 *
	 * @throws IllegalStateException if the collection index type
	 * returned by {@link #getType()} is not of type
	 * {@link org.hibernate.type.AnyType}.
	 */
	public AnyMappingDefinition toAnyMappingDefinition();
}
