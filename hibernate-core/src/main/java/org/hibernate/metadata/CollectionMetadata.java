/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metadata;
import org.hibernate.type.Type;

/**
 * Exposes collection metadata to the application
 *
 * @author Gavin King
 *
 * @deprecated (since 6.0) Use Hibernate's mapping model {@link org.hibernate.metamodel.MappingMetamodel}
 */
@Deprecated
public interface CollectionMetadata {
	/**
	 * The collection key type
	 */
	Type getKeyType();
	/**
	 * The collection element type
	 */
	Type getElementType();
	/**
	 * The collection index type (or null if the collection has no index)
	 */
	Type getIndexType();
	/**
	 * Is this collection indexed?
	 */
	boolean hasIndex();
	/**
	 * The name of this collection role
	 */
	String getRole();
	/**
	 * Is the collection an array?
	 */
	boolean isArray();
	/**
	 * Is the collection a primitive array?
	 */
	boolean isPrimitiveArray();
	/**
	 * Is the collection lazily initialized?
	 */
	boolean isLazy();
}
