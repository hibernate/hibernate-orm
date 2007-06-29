//$Id: ClassicQueryTranslatorFactory.java 9162 2006-01-27 23:40:32Z steveebersole $
package org.hibernate.hql.classic;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.FilterTranslator;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.QueryTranslatorFactory;

import java.util.Map;

/**
 * Generates translators which uses the older hand-written parser to perform
 * the translation.
 *
 * @author Gavin King
 */
public class ClassicQueryTranslatorFactory implements QueryTranslatorFactory {

	/**
	 * @see QueryTranslatorFactory#createQueryTranslator
	 */
	public QueryTranslator createQueryTranslator(
			String queryIdentifier,
	        String queryString,
	        Map filters,
	        SessionFactoryImplementor factory) {
		return new QueryTranslatorImpl( queryIdentifier, queryString, filters, factory );
	}

	/**
	 * @see QueryTranslatorFactory#createFilterTranslator
	 */
	public FilterTranslator createFilterTranslator(
			String queryIdentifier,
			String queryString,
	        Map filters,
	        SessionFactoryImplementor factory) {
		return new QueryTranslatorImpl( queryIdentifier, queryString, filters, factory );
	}

}
