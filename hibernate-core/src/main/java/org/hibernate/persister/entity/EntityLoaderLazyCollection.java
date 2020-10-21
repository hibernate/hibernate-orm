/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.internal.util.collections.LazyIndexedMap;
import org.hibernate.loader.entity.UniqueEntityLoader;

final class EntityLoaderLazyCollection extends LazyIndexedMap<Object,UniqueEntityLoader> {

	/**
	 * The need here is weird: we need to store an instance of UniqueEntityLoader
	 * for each value of the LockMode enum, but also store two special internal
	 * fetch profiles called "merge" and "refresh".
	 * We assign these two their own ordinal ids such as to be treated as two
	 * additional enum states; but to access these they will have their own
	 * dedicated method implementations.
	 */
	private static final int MERGE_INDEX = LockMode.values().length;
	private static final int REFRESH_INDEX = MERGE_INDEX + 1;
	private static final int TOTAL_STORAGE_SIZE = REFRESH_INDEX + 1;

	public EntityLoaderLazyCollection() {
		super( TOTAL_STORAGE_SIZE );
	}

	UniqueEntityLoader getOrBuildByLockMode(LockMode lockMode, Function<LockMode,UniqueEntityLoader> builderFunction) {
		return super.computeIfAbsent( lockMode.ordinal(), lockMode, builderFunction );
	}

	UniqueEntityLoader getOrCreateByInternalFetchProfileMerge(Function<LockMode,UniqueEntityLoader> builderFunction) {
		return super.computeIfAbsent( MERGE_INDEX, null, builderFunction );
	}

	UniqueEntityLoader getOrCreateByInternalFetchProfileRefresh(Function<LockMode,UniqueEntityLoader> builderFunction) {
		return super.computeIfAbsent( REFRESH_INDEX, null, builderFunction );
	}

}
