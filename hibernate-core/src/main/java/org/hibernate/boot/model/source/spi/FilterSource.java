/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.util.Map;

/**
 * Defines the source of filter information.  May have an associated {@link org.hibernate.engine.spi.FilterDefinition}.
 * Relates to both {@code <filter/>} and {@link org.hibernate.annotations.Filter @Filter}
 *
 * @author Steve Ebersole
 */
public interface FilterSource {
	/**
	 * Get the name of the filter being described.
	 *
	 * @return The name.
	 */
	String getName();

	/**
	 * Get the condition associated with the filter.  Can be {@code null} in the case of a filter described
	 * further by a "filter def" which contains the condition text.
	 *
	 * @return The condition defined on the filter.
	 *
	 * @see org.hibernate.boot.model.source.internal.hbm.FilterSourceImpl#getCondition()
	 */
	String getCondition();

	/**
	 * Should Hibernate perform automatic alias injection into the supplied condition string?  The default is to
	 * perform auto injection *unless* explicit alias(es) are supplied.
	 *
	 * @return {@code true} indicates auto injection should occur; {@code false} that it should not
	 */
	boolean shouldAutoInjectAliases();

	/**
	 * Get the map of explicit alias to table name mappings.
	 *
	 * @return The alias to table map
	 */
	Map<String, String> getAliasToTableMap();

	/**
	 * Get the map of explicit alias to entity name mappings.
	 *
	 * @return The alias to entity map
	 */
	Map<String, String> getAliasToEntityMap();
}
