/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import org.hibernate.query.results.FetchBuilder;
import org.hibernate.sql.results.graph.Fetchable;

/**
 * @author Steve Ebersole
 */
public interface DynamicFetchBuilderContainer {
	/**
	 * Locate an explicit fetch definition for the named fetchable
	 */
	FetchBuilder findFetchBuilder(Fetchable fetchable);

	/**
	 * Add a property mapped to a single column.
	 */
	DynamicFetchBuilderContainer addProperty(Fetchable fetchable, String columnAlias);

	/**
	 * Add a property mapped to multiple columns
	 */
	DynamicFetchBuilderContainer addProperty(Fetchable fetchable, String... columnAliases);

	/**
	 * Add a property whose columns can later be defined using {@link DynamicFetchBuilder#addColumnAlias}
	 */
	DynamicFetchBuilder addProperty(Fetchable fetchable);

	void addFetchBuilder(Fetchable fetchable, FetchBuilder fetchBuilder);
}
