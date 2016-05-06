/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
			return ( (Pair) changedElement ).getSecond();
		}

		if ( changedElement instanceof Map.Entry ) {
			return ( (Map.Entry) changedElement ).getValue();
		}

		return changedElement;
	}

	/**
	 * @return Index of the affected element, or {@code null} if the collection isn't indexed.
	 */
	public Object getChangedElementIndex() {
		if ( changedElement instanceof Pair ) {
			return ( (Pair) changedElement ).getFirst();
		}

		if ( changedElement instanceof Map.Entry ) {
			return ( (Map.Entry) changedElement ).getKey();
		}

		return null;
	}
}
