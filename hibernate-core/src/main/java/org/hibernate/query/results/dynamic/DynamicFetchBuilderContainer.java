/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
