//$Id: QueryTranslatorFactory.java 9162 2006-01-27 23:40:32Z steveebersole $
package org.hibernate.hql;

import org.hibernate.engine.SessionFactoryImplementor;

import java.util.Map;

/**
 * Facade for generation of {@link QueryTranslator} and {@link FilterTranslator} instances.
 *
 * @author Gavin King
 */
public interface QueryTranslatorFactory {
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
	 * @return an appropriate translator.
	 */
	public QueryTranslator createQueryTranslator(String queryIdentifier, String queryString, Map filters, SessionFactoryImplementor factory);

	/**
	 * Construct a {@link FilterTranslator} instance capable of translating
	 * an HQL filter string.
	 *
	 * @see #createQueryTranslator
	 */
	public FilterTranslator createFilterTranslator(String queryIdentifier, String queryString, Map filters, SessionFactoryImplementor factory);
}
