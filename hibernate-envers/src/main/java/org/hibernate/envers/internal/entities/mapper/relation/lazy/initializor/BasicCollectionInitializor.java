/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * Initializes a non-indexed java collection (set or list, eventually sorted).
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicCollectionInitializor<T extends Collection> extends AbstractCollectionInitializor<T> {
	protected final Class<? extends T> collectionClass;
	private final MiddleComponentData elementComponentData;

	public BasicCollectionInitializor(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			RelationQueryGenerator queryGenerator,
			Object primaryKey, Number revision, boolean removed,
			Class<? extends T> collectionClass,
			MiddleComponentData elementComponentData) {
		super( enversService, versionsReader, queryGenerator, primaryKey, revision, removed );

		this.collectionClass = collectionClass;
		this.elementComponentData = elementComponentData;
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
		if ( collectionRow instanceof List ) {
			elementData = ( (List) collectionRow ).get( elementComponentData.getComponentIndex() );
		}

		// If the target entity is not audited, the elements may be the entities already, so we have to check
		// if they are maps or not.
		Object element;
		if ( elementData instanceof Map ) {
			element = elementComponentData.getComponentMapper().mapToObjectFromFullMap(
					entityInstantiator,
					(Map<String, Object>) elementData, null, revision
			);
		}
		else {
			element = elementData;
		}
		collection.add( element );
	}
}
