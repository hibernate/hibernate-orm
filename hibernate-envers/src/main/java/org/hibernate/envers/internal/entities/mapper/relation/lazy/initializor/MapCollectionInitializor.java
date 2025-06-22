/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;

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
public class MapCollectionInitializor<T extends Map> extends AbstractCollectionInitializor<T> {
	protected final Class<? extends T> collectionClass;
	private final MiddleComponentData elementComponentData;
	private final MiddleComponentData indexComponentData;

	public MapCollectionInitializor(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			RelationQueryGenerator queryGenerator,
			Object primaryKey, Number revision, boolean removed,
			Class<? extends T> collectionClass,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData) {
		super( enversService, versionsReader, queryGenerator, primaryKey, revision, removed );

		this.collectionClass = collectionClass;
		this.elementComponentData = elementComponentData;
		this.indexComponentData = indexComponentData;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T initializeCollection(int size) {
		return newObjectInstance( collectionClass );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addToCollection(T collection, Object collectionRow) {
		// collectionRow will be the actual object if retrieved from audit relation or middle table
		// otherwise it will be a List
		Object elementData = collectionRow;
		Object indexData = collectionRow;
		if ( collectionRow instanceof List ) {
			elementData = ( (List) collectionRow ).get( elementComponentData.getComponentIndex() );
			indexData = ( (List) collectionRow ).get( indexComponentData.getComponentIndex() );
		}
		final Object element = elementComponentData.getComponentMapper().mapToObjectFromFullMap(
				entityInstantiator,
				(Map<String, Object>) elementData, null, revision
		);

		final Object index = indexComponentData.getComponentMapper().mapToObjectFromFullMap(
				entityInstantiator,
				(Map<String, Object>) indexData, element, revision
		);

		collection.put( index, element );
	}
}
