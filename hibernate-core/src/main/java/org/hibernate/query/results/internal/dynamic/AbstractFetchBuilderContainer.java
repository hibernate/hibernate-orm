/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.sql.results.graph.Fetchable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchBuilderContainer<T extends AbstractFetchBuilderContainer<T>>
		implements DynamicFetchBuilderContainer {
	private Map<Fetchable, FetchBuilder> fetchBuilderMap;

	protected AbstractFetchBuilderContainer() {
	}

	protected AbstractFetchBuilderContainer(AbstractFetchBuilderContainer<T> original) {
		if ( original.fetchBuilderMap != null ) {
			fetchBuilderMap = new HashMap<>( original.fetchBuilderMap.size() );
			for ( Map.Entry<Fetchable, FetchBuilder> entry : original.fetchBuilderMap.entrySet() ) {
				final FetchBuilder fetchBuilder =
						entry.getValue() instanceof DynamicFetchBuilderStandard dynamicFetchBuilderStandard
								? dynamicFetchBuilderStandard.cacheKeyInstance( this )
								: entry.getValue().cacheKeyInstance();
				fetchBuilderMap.put( entry.getKey(), fetchBuilder );
			}
		}
	}

	protected abstract String getPropertyBase();

	@Override
	public FetchBuilder findFetchBuilder(Fetchable fetchable) {
		return fetchBuilderMap == null ? null : fetchBuilderMap.get( fetchable );
	}

	@Override
	public T addProperty(Fetchable fetchable, String columnAlias) {
		final DynamicFetchBuilder fetchBuilder = addProperty( fetchable );
		fetchBuilder.addColumnAlias( columnAlias );
		return (T) this;
	}

	@Override
	public T addProperty(Fetchable fetchable, String... columnAliases) {
		final DynamicFetchBuilder fetchBuilder = addProperty( fetchable );
		ArrayHelper.forEach( columnAliases, fetchBuilder::addColumnAlias );
		return (T) this;
	}

	@Override
	public DynamicFetchBuilder addProperty(Fetchable fetchable) {
		if ( fetchBuilderMap == null ) {
			fetchBuilderMap = new HashMap<>();
		}
		else {
			final FetchBuilder existing = fetchBuilderMap.get( fetchable );
			if ( existing != null ) {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Fetch was already defined for %s.%s : %s",
								getPropertyBase(),
								fetchable,
								existing
						)
				);
			}
		}

		final DynamicFetchBuilderStandard fetchBuilder = new DynamicFetchBuilderStandard( fetchable	);
		fetchBuilderMap.put( fetchable, fetchBuilder );
		return fetchBuilder;
	}

	@Override
	public void addFetchBuilder(Fetchable fetchable, FetchBuilder fetchBuilder) {
		if ( fetchBuilderMap == null ) {
			fetchBuilderMap = new HashMap<>();
		}
		fetchBuilderMap.put( fetchable, fetchBuilder );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final AbstractFetchBuilderContainer<?> that = (AbstractFetchBuilderContainer<?>) o;
		return Objects.equals( fetchBuilderMap, that.fetchBuilderMap );
	}

	@Override
	public int hashCode() {
		return fetchBuilderMap != null ? fetchBuilderMap.hashCode() : 0;
	}
}
