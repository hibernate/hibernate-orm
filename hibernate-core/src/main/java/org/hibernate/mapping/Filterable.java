/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.List;
import java.util.Map;


/**
 * Defines mapping elements to which filters may be applied.
 *
 * @author Steve Ebersole
 */
public interface Filterable {
	void addFilter(
			String name, String condition, boolean autoAliasInjection,
			Map<String,String> aliasTableMap, Map<String,String> aliasEntityMap);

	List<FilterConfiguration> getFilters();
}
