/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Map;

/**
 * Defines the source of filter information.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.engine.spi.FilterDefinition
 */
public interface FilterSource {
	/**
	 * Get the name of the filter being described.
	 *
	 * @return The name.
	 */
	String getName();

	/**
	 * Get the condition associated with the filter.  Can be {@code null}
	 * in the case of a filter described further by a "filter def" which
	 * contains the condition text.
	 *
	 * @return The condition defined on the filter.
	 */
	String getCondition();

	/**
	 * Should Hibernate perform automatic alias injection into the supplied
	 * condition string?  The default it to perform auto injection *unless*
	 * explicit alias(es) are supplied.
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
