/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.metadata;
import org.hibernate.type.Type;

/**
 * Exposes collection metadata to the application
 *
 * @author Gavin King
 */
public interface CollectionMetadata {
	/**
	 * The collection key type
	 */
	public Type getKeyType();
	/**
	 * The collection element type
	 */
	public Type getElementType();
	/**
	 * The collection index type (or null if the collection has no index)
	 */
	public Type getIndexType();
	/**
	 * Is this collection indexed?
	 */
	public boolean hasIndex();
	/**
	 * The name of this collection role
	 */
	public String getRole();
	/**
	 * Is the collection an array?
	 */
	public boolean isArray();
	/**
	 * Is the collection a primitive array?
	 */
	public boolean isPrimitiveArray();
	/**
	 * Is the collection lazily initialized?
	 */
	public boolean isLazy();
}






