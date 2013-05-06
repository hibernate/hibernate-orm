/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.spi.AuditConfiguration;
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
			AuditConfiguration verCfg,
			AuditReaderImplementor versionsReader,
			RelationQueryGenerator queryGenerator,
			Object primaryKey, Number revision, boolean removed,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData) {
		super( verCfg, versionsReader, queryGenerator, primaryKey, revision, removed );

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
			elementData = ((List) collectionRow).get( elementComponentData.getComponentIndex() );
			indexData = ((List) collectionRow).get( indexComponentData.getComponentIndex() );
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
		final int index = ((Number) indexObj).intValue();

		collection.set( index, element );
	}
}
