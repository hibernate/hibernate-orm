/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.internal.AbstractCollectionInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Internal helper to keep track of the various
 * Initializer instances being used during RowReader processing:
 * different types of initializers need to be invoked in different orders,
 * so rather than having to identify the order of initializers again during
 * the processing of each row we keep separated lists of initializers defined
 * upfront and then reuse these sets for the scope of the whole resultset's
 * processing.
 * @author Sanne Grinovero
 */
public final class InitializersList {

	private final Initializer[] initializers;
	private final Initializer[] sortedNonCollectionsFirst;
	private final Initializer[] sortedForResolveInstance;
	private final boolean hasCollectionInitializers;
	private final Map<NavigablePath, Initializer> initializerMap;

	private InitializersList(
			Initializer[] initializers,
			Initializer[] sortedNonCollectionsFirst,
			Initializer[] sortedForResolveInstance,
			boolean hasCollectionInitializers,
			Map<NavigablePath, Initializer> initializerMap) {
		this.initializers = initializers;
		this.sortedNonCollectionsFirst = sortedNonCollectionsFirst;
		this.sortedForResolveInstance = sortedForResolveInstance;
		this.hasCollectionInitializers = hasCollectionInitializers;
		this.initializerMap = initializerMap;
	}

	@Deprecated //for simpler migration to the new SPI
	public List<Initializer> asList() {
		return Arrays.asList( initializers );
	}

	public Initializer resolveInitializer(final NavigablePath path) {
		return initializerMap.get( path );
	}

	public void finishUpRow(final RowProcessingState rowProcessingState) {
		for ( Initializer init : initializers ) {
			init.finishUpRow( rowProcessingState );
		}
	}

	public void initializeInstance(final RowProcessingState rowProcessingState) {
		for ( Initializer init : initializers ) {
			init.initializeInstance( rowProcessingState );
		}
	}

	public void endLoading(final ExecutionContext executionContext) {
		for ( Initializer initializer : initializers ) {
			initializer.endLoading( executionContext );
		}
	}

	public void resolveKeys(final RowProcessingState rowProcessingState) {
		for ( Initializer init : sortedNonCollectionsFirst ) {
			init.resolveKey( rowProcessingState );
		}
	}

	public void resolveInstances(final RowProcessingState rowProcessingState) {
		for ( Initializer init : sortedForResolveInstance ) {
			init.resolveInstance( rowProcessingState );
		}
	}

	public boolean hasCollectionInitializers() {
		return this.hasCollectionInitializers;
	}

	static class Builder {
		private ArrayList<Initializer> initializers = new ArrayList<>();
		int nonCollectionInitializersNum = 0;
		int resolveFirstNum = 0;

		public Builder() {}

		public void addInitializer(final Initializer initializer) {
			initializers.add( initializer );
			//in this method we perform these checks merely to learn the sizing hints,
			//so to not need dynamically scaling collections.
			//This implies performing both checks twice but since they're cheap it's preferrable
			//to multiple allocations; not least this allows using arrays, which makes iteration
			//cheaper during the row processing - which is very hot.
			if ( !initializer.isCollectionInitializer() ) {
				nonCollectionInitializersNum++;
			}
			if ( initializeFirst( initializer ) ) {
				resolveFirstNum++;
			}
		}

		private static boolean initializeFirst(final Initializer initializer) {
			return !( initializer instanceof EntityDelayedFetchInitializer )
					&& !( initializer instanceof EntitySelectFetchInitializer )
					&& !( initializer instanceof AbstractCollectionInitializer );
		}

		InitializersList build(final Map<NavigablePath, Initializer> initializerMap) {
			final int size = initializers.size();
			final Initializer[] sortedNonCollectionsFirst = new Initializer[size];
			final Initializer[] sortedForResolveInstance = new Initializer[size];
			int nonCollectionIdx = 0;
			int collectionIdx = nonCollectionInitializersNum;
			int resolveFirstIdx = 0;
			int resolveLaterIdx = resolveFirstNum;
			final Initializer[] originalSortInitializers = toArray( initializers );
			for ( Initializer initializer : originalSortInitializers ) {
				if ( initializer.isCollectionInitializer() ) {
					sortedNonCollectionsFirst[collectionIdx++] = initializer;
				}
				else {
					sortedNonCollectionsFirst[nonCollectionIdx++] = initializer;
				}
				if ( initializeFirst( initializer ) ) {
					sortedForResolveInstance[resolveFirstIdx++] = initializer;
				}
				else {
					sortedForResolveInstance[resolveLaterIdx++] = initializer;
				}
			}
			final boolean hasCollectionInitializers = ( nonCollectionInitializersNum != initializers.size() );
			return new InitializersList(
					originalSortInitializers,
					sortedNonCollectionsFirst,
					sortedForResolveInstance,
					hasCollectionInitializers,
					initializerMap
			);
		}

		private Initializer[] toArray(final ArrayList<Initializer> initializers) {
			return initializers.toArray( new Initializer[initializers.size()] );
		}

	}

}
