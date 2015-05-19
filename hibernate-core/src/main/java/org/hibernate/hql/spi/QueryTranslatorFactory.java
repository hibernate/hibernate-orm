/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi;
import java.util.Map;

import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.Service;

/**
 * Facade for generation of {@link QueryTranslator} and {@link FilterTranslator} instances.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface QueryTranslatorFactory extends Service {
	/**
	 * Construct a {@link QueryTranslator} instance capable of translating
	 * an HQL query string.
	 *
	 * @param queryIdentifier The query-identifier (used in
	 * {@link org.hibernate.stat.QueryStatistics} collection). This is
	 * typically the same as the queryString parameter except for the case of
	 * split polymorphic queries which result in multiple physical sql
	 * queries.
	 * @param queryString The query string to be translated
	 * @param filters Currently enabled filters
	 * @param factory The session factory.
	 * @param entityGraphQueryHint
	 * @return an appropriate translator.
	 */
	public QueryTranslator createQueryTranslator(
			String queryIdentifier,
			String queryString,
			Map filters,
			SessionFactoryImplementor factory,
			EntityGraphQueryHint entityGraphQueryHint);

	/**
	 * Construct a {@link FilterTranslator} instance capable of translating
	 * an HQL filter string.
	 *
	 * @see #createQueryTranslator
	 */
	public FilterTranslator createFilterTranslator(
			String queryIdentifier,
			String queryString,
			Map filters,
			SessionFactoryImplementor factory);
}
