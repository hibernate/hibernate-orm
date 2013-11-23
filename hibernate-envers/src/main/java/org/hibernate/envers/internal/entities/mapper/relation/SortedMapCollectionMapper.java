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

import java.util.Comparator;
import java.util.SortedMap;

import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.SortedMapCollectionInitializor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public final class SortedMapCollectionMapper extends MapCollectionMapper<SortedMap> {
	private final Comparator comparator;

	public SortedMapCollectionMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			Class<? extends SortedMap> collectionClass, Class<? extends SortedMap> proxyClass,
			MiddleComponentData elementComponentData, MiddleComponentData indexComponentData, Comparator comparator,
			boolean revisionTypeInId) {
		super(
				commonCollectionMapperData,
				collectionClass,
				proxyClass,
				elementComponentData,
				indexComponentData,
				revisionTypeInId
		);
		this.comparator = comparator;
	}

	@Override
	protected Initializor<SortedMap> getInitializor(
			AuditConfiguration verCfg, AuditReaderImplementor versionsReader,
			Object primaryKey, Number revision, boolean removed) {
		return new SortedMapCollectionInitializor(
				verCfg, versionsReader, commonCollectionMapperData.getQueryGenerator(),
				primaryKey, revision, removed, collectionClass, elementComponentData, indexComponentData, comparator
		);
	}

}
