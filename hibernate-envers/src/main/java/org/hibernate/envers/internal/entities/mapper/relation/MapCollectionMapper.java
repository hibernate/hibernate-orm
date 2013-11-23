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
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
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
			AuditConfiguration verCfg, AuditReaderImplementor versionsReader,
			Object primaryKey, Number revision, boolean removed) {
		return new MapCollectionInitializor<T>(
				verCfg, versionsReader, commonCollectionMapperData.getQueryGenerator(),
				primaryKey, revision, removed, collectionClass, elementComponentData, indexComponentData
		);
	}

	@Override
	protected Collection getNewCollectionContent(PersistentCollection newCollection) {
		if ( newCollection == null ) {
			return null;
		}
		else {
			return ((Map) newCollection).entrySet();
		}
	}

	@Override
	protected Collection getOldCollectionContent(Serializable oldCollection) {
		if ( oldCollection == null ) {
			return null;
		}
		else {
			return ((Map) oldCollection).entrySet();
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
				((Map.Entry) changed).getValue()
		);
		indexComponentData.getComponentMapper().mapToMapFromObject(
				session,
				idData,
				data,
				((Map.Entry) changed).getKey()
		);
	}
}
