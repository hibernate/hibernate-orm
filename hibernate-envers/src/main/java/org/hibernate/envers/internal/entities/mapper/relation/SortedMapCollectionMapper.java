/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.util.Comparator;
import java.util.SortedMap;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.SortedMapCollectionInitializor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public final class SortedMapCollectionMapper extends MapCollectionMapper<SortedMap> {
	private final Comparator comparator;

	public SortedMapCollectionMapper(
			Configuration configuration,
			CommonCollectionMapperData commonCollectionMapperData,
			Class<? extends SortedMap> collectionClass,
			Class<? extends SortedMap> proxyClass,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData,
			Comparator comparator,
			boolean revisionTypeInId) {
		super(
				configuration,
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
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Object primaryKey,
			Number revision,
			boolean removed) {
		return new SortedMapCollectionInitializor(
				enversService,
				versionsReader,
				commonCollectionMapperData.getQueryGenerator(),
				primaryKey,
				revision,
				removed,
				collectionClass,
				elementComponentData,
				indexComponentData,
				comparator
		);
	}

}
