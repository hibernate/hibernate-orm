/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.dynamic;

import org.hibernate.query.results.FetchBuilder;

/**
 * @author Steve Ebersole
 */
public interface DynamicFetchBuilderContainer {
	/**
	 * Locate an explicit fetch definition for the named fetchable
	 */
	FetchBuilder findFetchBuilder(String fetchableName);

	/**
	 * Add a property mapped to a single column.
	 */
	DynamicFetchBuilderContainer addProperty(String propertyName, String columnAlias);

	/**
	 * Add a property mapped to multiple columns
	 */
	DynamicFetchBuilderContainer addProperty(String propertyName, String... columnAliases);

	/**
	 * Add a property whose columns can later be defined using {@link DynamicFetchBuilder#addColumnAlias}
	 */
	DynamicFetchBuilder addProperty(String propertyName);

	void addFetchBuilder(String propertyName, FetchBuilder fetchBuilder);
}
