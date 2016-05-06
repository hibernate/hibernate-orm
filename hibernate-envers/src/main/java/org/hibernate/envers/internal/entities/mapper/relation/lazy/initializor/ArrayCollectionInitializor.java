/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
public class ArrayCollectionInitializor extends AbstractCollectionInitializor<Object[]> {
	private final MiddleComponentData elementComponentData;
	private final MiddleComponentData indexComponentData;

	public ArrayCollectionInitializor(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			RelationQueryGenerator queryGenerator,
			Object primaryKey, Number revision, boolean removed,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData) {
		super( enversService, versionsReader, queryGenerator, primaryKey, revision, removed );

		this.elementComponentData = elementComponentData;
		this.indexComponentData = indexComponentData;
	}

	@Override
	protected Object[] initializeCollection(int size) {
		return new Object[size];
	}

	@Override
	@SuppressWarnings({"unchecked"})
	protected void addToCollection(Object[] collection, Object collectionRow) {
		final Object elementData = ( (List) collectionRow ).get( elementComponentData.getComponentIndex() );
		final Object element = elementComponentData.getComponentMapper().mapToObjectFromFullMap(
				entityInstantiator,
				(Map<String, Object>) elementData, null, revision
		);

		final Object indexData = ( (List) collectionRow ).get( indexComponentData.getComponentIndex() );
		final Object indexObj = indexComponentData.getComponentMapper().mapToObjectFromFullMap(
				entityInstantiator,
				(Map<String, Object>) indexData, element, revision
		);
		final int index = ( (Number) indexObj ).intValue();

		collection[index] = element;
	}
}
