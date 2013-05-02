/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.entities.mapper;

import java.util.Map;

import org.hibernate.envers.tools.Pair;

/**
 * Data describing the change of a single object in a persistent collection (when the object was added, removed or
 * modified in the collection).
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class PersistentCollectionChangeData {
	private final String entityName;
	private final Map<String, Object> data;
	private final Object changedElement;

	public PersistentCollectionChangeData(String entityName, Map<String, Object> data, Object changedElement) {
		this.entityName = entityName;
		this.data = data;
		this.changedElement = changedElement;
	}

	/**
	 * @return Name of the (middle) entity that holds the collection data.
	 */
	public String getEntityName() {
		return entityName;
	}

	public Map<String, Object> getData() {
		return data;
	}

	/**
	 * @return The affected element, which was changed (added, removed, modified) in the collection.
	 */
	public Object getChangedElement() {
		if ( changedElement instanceof Pair ) {
			return ((Pair) changedElement).getSecond();
		}

		if ( changedElement instanceof Map.Entry ) {
			return ((Map.Entry) changedElement).getValue();
		}

		return changedElement;
	}

	/**
	 * @return Index of the affected element, or {@code null} if the collection isn't indexed.
	 */
	public Object getChangedElementIndex() {
		if ( changedElement instanceof Pair ) {
			return ((Pair) changedElement).getFirst();
		}

		if ( changedElement instanceof Map.Entry ) {
			return ((Map.Entry) changedElement).getKey();
		}

		return null;
	}
}
