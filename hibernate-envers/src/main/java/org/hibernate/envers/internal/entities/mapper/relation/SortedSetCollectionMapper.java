/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.util.Comparator;
import java.util.SortedSet;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.SortedSetCollectionInitializor;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public final class SortedSetCollectionMapper extends BasicCollectionMapper<SortedSet> {
	private final Comparator comparator;

	public SortedSetCollectionMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			Class<? extends SortedSet> collectionClass, Class<? extends SortedSet> proxyClass,
			MiddleComponentData elementComponentData, Comparator comparator, boolean ordinalInId,
			boolean revisionTypeInId) {
		super(
				commonCollectionMapperData,
				collectionClass,
				proxyClass,
				elementComponentData,
				ordinalInId,
				revisionTypeInId
		);
		this.comparator = comparator;
	}

	@Override
	protected Initializor<SortedSet> getInitializor(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Object primaryKey,
			Number revision,
			boolean removed) {
		return new SortedSetCollectionInitializor(
				enversService,
				versionsReader,
				commonCollectionMapperData.getQueryGenerator(),
				primaryKey,
				revision,
				removed,
				collectionClass,
				elementComponentData,
				comparator
		);
	}
}
