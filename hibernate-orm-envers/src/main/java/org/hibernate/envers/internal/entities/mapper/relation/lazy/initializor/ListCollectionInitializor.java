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
		// Creating a list of the given capacity with all elements null initially. This ensures that we can then
		// fill the elements safely using the <code>List.set</code> method.
		final List list = new ArrayList( size );
		for ( int i = 0; i < size; i++ ) {
			list.add( null );
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
		if ( collectionRow instanceof java.util.List ) {
			elementData = ( (List) collectionRow ).get( elementComponentData.getComponentIndex() );
			indexData = ( (List) collectionRow ).get( indexComponentData.getComponentIndex() );
		}
		final Object element = elementData instanceof Map
				? elementComponentData.getComponentMapper().mapToObjectFromFullMap(
						entityInstantiator,
						(Map<String, Object>) elementData, null, revision
				)
				: elementData;

		final Object indexObj = indexComponentData.getComponentMapper().mapToObjectFromFullMap(
				entityInstantiator,
				(Map<String, Object>) indexData, element, revision
		);
		final int index = ( (Number) indexObj ).intValue();

		collection.set( index, element );
	}
}
