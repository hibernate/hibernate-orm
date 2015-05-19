/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Map;

/**
 * Defines the source of filter information.  May have an associated {@link FilterDefinitionSource}.
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
	public String getName();

	/**
	 * Get the condition associated with the filter.  Can be {@code null} in the case of a filter described
	 * further by a "filter def" which contains the condition text.
	 *
	 * @return The condition defined on the filter.
	 *
	 * @see {@link FilterDefinitionSource#getCondition()}
	 */
	public String getCondition();

	/**
	 * Should Hibernate perform automatic alias injection into the supplied condition string?  The default it to
	 * perform auto injection *unless* explicit alias(es) are supplied.
	 *
	 * @return {@code true} indicates auto injection should occur; {@code false} that it should not
	 */
	public boolean shouldAutoInjectAliases();

	/**
	 * Get the map of explicit alias to table name mappings.
	 *
	 * @return The alias to table map
	 */
	public Map<String, String> getAliasToTableMap();

	/**
	 * Get the map of explicit alias to entity name mappings.
	 *
	 * @return The alias to entity map
	 */
	public Map<String, String> getAliasToEntityMap();
}
