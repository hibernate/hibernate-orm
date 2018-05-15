/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Steve Ebersole
 */
class QueryResultsRegionImpl extends AbstractDirectAccessRegion implements QueryResultsRegion {
	private final SessionFactoryImplementor sessionFactory;

	QueryResultsRegionImpl(String name, SessionFactoryImplementor sessionFactory) {
		super( name );
		this.sessionFactory = sessionFactory;
	}

	@Override
	public RegionFactory getRegionFactory() {
		return sessionFactory.getCache().getRegionFactory();
	}
}
