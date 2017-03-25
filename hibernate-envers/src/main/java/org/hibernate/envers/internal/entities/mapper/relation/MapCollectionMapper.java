/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.MapCollectionInitializor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MapCollectionMapper<T extends Map> extends AbstractCollectionMapper<T> implements PropertyMapper {
	protected final MiddleComponentData elementComponentData;
	protected final MiddleComponentData indexComponentData;

	public MapCollectionMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			Class<? extends T> collectionClass, Class<? extends T> proxyClass,
			MiddleComponentData elementComponentData, MiddleComponentData indexComponentData,
			boolean revisionTypeInId) {
		super( commonCollectionMapperData, collectionClass, proxyClass, false, revisionTypeInId );
		this.elementComponentData = elementComponentData;
		this.indexComponentData = indexComponentData;
	}

	@Override
	protected Initializor<T> getInitializor(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Object primaryKey,
			Number revision,
			boolean removed) {
		return new MapCollectionInitializor<>(
				enversService,
				versionsReader,
				commonCollectionMapperData.getQueryGenerator(),
				primaryKey,
				revision,
				removed,
				collectionClass,
				elementComponentData,
				indexComponentData
		);
	}

	@Override
	protected Collection getNewCollectionContent(PersistentCollection newCollection) {
		if ( newCollection == null ) {
			return null;
		}
		else {
			return ( (Map) newCollection ).entrySet();
		}
	}

	@Override
	protected Collection getOldCollectionContent(Serializable oldCollection) {
		if ( oldCollection == null ) {
			return null;
		}
		else {
			return ( (Map) oldCollection ).entrySet();
		}
	}

	@Override
	protected void mapToMapFromObject(
			SessionImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object changed) {
		elementComponentData.getComponentMapper().mapToMapFromObject(
				session,
				idData,
				data,
				( (Map.Entry) changed ).getValue()
		);
		indexComponentData.getComponentMapper().mapToMapFromObject(
				session,
				idData,
				data,
				( (Map.Entry) changed ).getKey()
		);
	}
}
