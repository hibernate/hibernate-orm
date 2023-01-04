/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Internal helper to keep track of the various
 * Initializer instances being used during RowReader processing:
 * different types of initializers need to be invoked in different orders,
 * so rather than finding them during each row we keep separated lists
 * of initializers defined upfront and then reused for the scope of the whole
 * resultset.
 */
public final class InitializersList {
	private final List<Initializer> initializers;
	private final List<Initializer> collectionInitializers;
	private final List<Initializer> nonCollectionInitializers;
	private final List<Initializer> resolveInstanceFirstInitializers;
	private final List<Initializer> resolveInstanceLaterInitializers;
	private final boolean hasCollectionInitializers;
	private final Map<NavigablePath, Initializer> initializerMap;

	private InitializersList(
			List<Initializer> initializers,
			List<Initializer> collectionInitializers,
			List<Initializer> nonCollectionInitializers,
			List<Initializer> resolveInstanceFirstInitializers,
			List<Initializer> resolveInstanceLaterInitializers,
			Map<NavigablePath, Initializer> initializerMap) {
		this.initializers = initializers;
		this.collectionInitializers = collectionInitializers;
		this.nonCollectionInitializers = nonCollectionInitializers;
		this.resolveInstanceFirstInitializers = resolveInstanceFirstInitializers;
		this.resolveInstanceLaterInitializers = resolveInstanceLaterInitializers;
		this.hasCollectionInitializers = ! collectionInitializers.isEmpty();
		this.initializerMap = initializerMap;
	}

	@Deprecated //for simpler migration to the new SPI
	public List<Initializer> asList() {
		return initializers;
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
		for ( Initializer init : nonCollectionInitializers ) {
			init.resolveKey( rowProcessingState );
		}
		for ( Initializer init : collectionInitializers ) {
			init.resolveKey( rowProcessingState );
		}
	}

	public void resolveInstances(final RowProcessingState rowProcessingState) {
		for ( Initializer init : resolveInstanceFirstInitializers ) {
			init.resolveInstance( rowProcessingState );
		}
		for ( Initializer init : resolveInstanceLaterInitializers ) {
			init.resolveInstance( rowProcessingState );
		}
	}

	public boolean hasCollectionInitializers() {
		return this.hasCollectionInitializers;
	}

	static class Builder {
		private List<Initializer> initializers = new ArrayList<>();
		private List<Initializer> collectionInitializers;
		private List<Initializer> nonCollectionInitializers;
		private List<Initializer> resolveInstanceFirstInitializers;
		private List<Initializer> resolveInstanceLaterInitializers;

		public Builder() {}

		public void addInitializer(Initializer initializer) {
			initializers.add( initializer );
			if ( initializer.isCollectionInitializer() ) {
				if ( collectionInitializers == null ) {
					collectionInitializers = new ArrayList<>();
				}
				collectionInitializers.add( initializer );
			}
			else {
				if ( nonCollectionInitializers == null ) {
					nonCollectionInitializers = new ArrayList<>();
				}
				nonCollectionInitializers.add( initializer );
			}
			if ( !( initializer instanceof EntityDelayedFetchInitializer ) && ! (initializer instanceof EntitySelectFetchInitializer ) ) {
				if ( resolveInstanceFirstInitializers == null ) {
					resolveInstanceFirstInitializers = new ArrayList<>();
				}
				resolveInstanceFirstInitializers.add( initializer );
			}
			else {
				if ( resolveInstanceLaterInitializers == null ) {
					resolveInstanceLaterInitializers = new ArrayList<>();
				}
				resolveInstanceLaterInitializers.add( initializer );
			}
		}

		InitializersList build(Map<NavigablePath, Initializer> initializerMap) {
			return new InitializersList(
					initializers,
					collectionInitializers == null ? Collections.EMPTY_LIST : collectionInitializers,
					nonCollectionInitializers == null ? Collections.EMPTY_LIST : nonCollectionInitializers,
					resolveInstanceFirstInitializers == null ? Collections.EMPTY_LIST : resolveInstanceFirstInitializers,
					resolveInstanceLaterInitializers == null ? Collections.EMPTY_LIST : resolveInstanceLaterInitializers,
					initializerMap
			);
		}

	}

}
