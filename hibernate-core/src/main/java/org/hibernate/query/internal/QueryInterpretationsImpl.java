/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryInterpretations;
import org.hibernate.query.spi.SelectQueryPlan;

/**
 * Standard QueryInterpretations implementation
 *
 * @author Steve Ebersole
 */
public class QueryInterpretationsImpl implements QueryInterpretations {
	private final SessionFactoryImplementor sessionFactory;

	public QueryInterpretationsImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public SelectQueryPlan getSelectQueryPlan(Key key) {
		// todo (6.0) : implement
		return null;
	}

	@Override
	public void cacheSelectQueryPlan(Key key, SelectQueryPlan plan) {
		// todo (6.0) : implement
	}

	@Override
	public NonSelectQueryPlan getNonSelectQueryPlan(Key key) {
		// todo (6.0) : implement
		return null;
	}

	@Override
	public void cacheNonSelectQueryPlan(Key key, NonSelectQueryPlan plan) {
		// todo (6.0) : implement
	}
}
