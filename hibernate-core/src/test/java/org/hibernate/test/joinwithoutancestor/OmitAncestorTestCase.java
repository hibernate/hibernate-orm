/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinwithoutancestor;

import org.hibernate.Session;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.internal.SessionImpl;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;

public abstract class OmitAncestorTestCase extends BaseCoreFunctionalTestCase {

	protected void assertFromTables(String query, String... tables) {
		try {
			TransactionUtil.doInHibernate( this::sessionFactory, session -> {
				String sql = getSql( session, query );
				SqlAsserts.assertFromTables( sql, tables );
				session.createQuery( query ).getResultList();
			} );
		}
		catch (AssertionError e) {
			throw e;
		}
	}

	protected String getSql(Session session, String hql) {
		// Create query
		session.createQuery( hql );

		// Get plan from cache
		QueryPlanCache queryPlanCache = sessionFactory().getQueryPlanCache();
		HQLQueryPlan hqlQueryPlan = queryPlanCache.getHQLQueryPlan(
				hql,
				false,
				( (SessionImpl) session ).getLoadQueryInfluencers().getEnabledFilters()
		);
		QueryTranslator queryTranslator = hqlQueryPlan.getTranslators()[0];
		String sql = queryTranslator.getSQLString();
		return sql;
	}
}
