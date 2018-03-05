/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * Initializes a map.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class ListCollectionInitializor extends AbstractCollectionInitializor<List> {
	private final MiddleComponentData elementComponentData;
	private final MiddleComponentData indexComponentData;

	public ListCollectionInitializor(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			RelationQueryGenerator queryGenerator,
			Object primaryKey,
			Number revision,
			boolean removed,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData) {
		super( enversService, versionsReader, queryGenerator, primaryKey, revision, removed );

		this.elementComponentData = elementComponentData;
		this.indexComponentData = indexComponentData;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	protected List initializeCollection(int size) {
		// There are two types of List collections that this class may generate
		//
		// 		1. Those which are not-indexed, thus the entries in the list are equal to the size argument passed.
		//		2. Those which are indexed, thus the entries in the list are based on the highest result-set index value.
		//		   In this use case, the supplied size value is irrelevant other than to minimize allocations.
		//
		// So what we're going to do is to build an ArrayList based on the supplied size as the best of the two
		// worlds.  When adding elements to the collection, we cannot make any assumption that the slot for which
		// we are inserting is valid, so we must continually expand the list as needed for (2); however we can
		// avoid unnecessary memory allocations by using the size parameter as an optimization for both (1) and (2).
		final List list = new ArrayList( size );
		for ( int i = 0; i < size; i++ ) {
			list.add( i, null );
		}
		return list;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	protected void addToCollection(List collection, Object collectionRow) {
		// collectionRow will be the actual object if retrieved from audit relation or middle table
		// otherwise it will be a List
		Object elementData = collectionRow;
		Object indexData = collectionRow;
		if ( java.util.List.class.isInstance( collectionRow ) ) {
			final java.util.List row = java.util.List.class.cast( collectionRow );
			elementData = row.get( elementComponentData.getComponentIndex() );
			indexData = row.get( indexComponentData.getComponentIndex() );
		}

		final Object element;
		if ( Map.class.isInstance( elementData ) ) {
			element = elementComponentData.getComponentMapper().mapToObjectFromFullMap(
					entityInstantiator,
					(Map<String, Object>) elementData,
					null,
					revision
			);
		}
		else {
			element = elementData;
		}

		final Object indexObj = indexComponentData.getComponentMapper().mapToObjectFromFullMap(
				entityInstantiator,
				(Map<String, Object>) indexData,
				element,
				revision
		);

		final int index = ( (Number) indexObj ).intValue();

		// This is copied from PersistentList#readFrom
		// For the indexed list use case to make sure the slot that we're going to set actually exists.
		for ( int i = collection.size(); i <= index; ++i ) {
			collection.add( i, null );
		}

		collection.set( index, element );
	}
}
