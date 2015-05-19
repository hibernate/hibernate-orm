/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.query.RelationQueryGenerator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.internal.util.ReflectHelper;

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
		try {
			return (T) ReflectHelper.getDefaultConstructor( collectionClass ).newInstance();
		}
		catch (InstantiationException e) {
			throw new AuditException( e );
		}
		catch (IllegalAccessException e) {
			throw new AuditException( e );
		}
		catch (InvocationTargetException e) {
			throw new AuditException( e );
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	protected void addToCollection(T collection, Object collectionRow) {
		// collectionRow will be the actual object if retrieved from audit relation or middle table
		// otherwise it will be a List
		Object elementData = collectionRow;
		Object indexData = collectionRow;
		if ( collectionRow instanceof java.util.List ) {
			elementData = ((List) collectionRow).get( elementComponentData.getComponentIndex() );
			indexData = ((List) collectionRow).get( indexComponentData.getComponentIndex() );
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
