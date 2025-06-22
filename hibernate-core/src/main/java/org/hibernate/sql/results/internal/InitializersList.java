/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.internal.EntityInitializerImpl;

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

	private final Initializer<?>[] initializers;
	private final Initializer<?>[] sortedForResolveInstance;
	private final boolean hasCollectionInitializers;

	private InitializersList(
			Initializer<?>[] initializers,
			Initializer<?>[] sortedForResolveInstance,
			boolean hasCollectionInitializers) {
		this.initializers = initializers;
		this.sortedForResolveInstance = sortedForResolveInstance;
		this.hasCollectionInitializers = hasCollectionInitializers;
	}

	public Initializer<?>[] getInitializers() {
		return initializers;
	}

	public Initializer<?>[] getSortedForResolveInstance() {
		return sortedForResolveInstance;
	}

	@Deprecated //for simpler migration to the new SPI
	public List<Initializer<?>> asList() {
		return Arrays.asList( initializers );
	}

	public int size() {
		return initializers.length;
	}

	public static class Builder {
		private final ArrayList<Initializer<?>> initializers;
		int nonCollectionInitializersNum = 0;
		int resolveFirstNum = 0;

		public Builder() {
			initializers = new ArrayList<>();
		}

		public Builder(int size) {
			initializers = new ArrayList<>( size );
		}

		public void addInitializer(final Initializer<?> initializer) {
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

		private static boolean initializeFirst(final Initializer<?> initializer) {
			return initializer instanceof EntityInitializerImpl;
		}

		public InitializersList build() {
			final int size = initializers.size();
			final Initializer<?>[] sortedForResolveInstance = new Initializer<?>[size];
			int resolveFirstIdx = 0;
			int resolveLaterIdx = resolveFirstNum;
			final Initializer<?>[] originalSortInitializers = toArray( initializers );
			for ( Initializer<?> initializer : originalSortInitializers ) {
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
					sortedForResolveInstance,
					hasCollectionInitializers
			);
		}

		private Initializer<?>[] toArray(final ArrayList<Initializer<?>> initializers) {
			return initializers.toArray( new Initializer<?>[initializers.size()] );
		}

	}

}
