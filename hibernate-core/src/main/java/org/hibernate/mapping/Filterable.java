/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;


/**
 * Defines mapping elements to which filters may be applied.
 *
 * @author Steve Ebersole
 */
public interface Filterable {
	public void addFilter(String name, String condition, boolean autoAliasInjection, java.util.Map<String,String> aliasTableMap, java.util.Map<String,String> aliasEntityMap);

	public java.util.List getFilters();
}
