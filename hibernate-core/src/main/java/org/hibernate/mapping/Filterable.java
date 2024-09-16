/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;


import org.hibernate.internal.FilterConfiguration;

/**
 * Defines mapping elements to which filters may be applied.
 *
 * @author Steve Ebersole
 */
public interface Filterable {
	void addFilter(String name, String condition, boolean autoAliasInjection, java.util.Map<String,String> aliasTableMap, java.util.Map<String,String> aliasEntityMap);

	java.util.List<FilterConfiguration> getFilters();
}
