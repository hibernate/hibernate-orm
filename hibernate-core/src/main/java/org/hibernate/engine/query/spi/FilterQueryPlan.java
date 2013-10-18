/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
