/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Extends an HQLQueryPlan to maintain a reference to the collection-role name
 * being filtered.
 *
 * @author Steve Ebersole
 */
public class FilterQueryPlan extends HQLQueryPlan implements Serializable {

	private final String collectionRole;

	/**
	 * Constructs a query plan for an HQL filter
	 *
	 * @param hql The HQL fragment
	 * @param collectionRole The collection role being filtered
	 * @param shallow Is the query shallow?
	 * @param enabledFilters All enabled filters from the Session
	 * @param factory The factory
	 */
	public FilterQueryPlan(
			String hql,
			String collectionRole,
			boolean shallow,
			Map enabledFilters,
			SessionFactoryImplementor factory) {
		super( hql, collectionRole, shallow, enabledFilters, factory, null );
		this.collectionRole = collectionRole;
	}

	public String getCollectionRole() {
		return collectionRole;
	}
}
