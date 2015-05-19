/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;

import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.spi.FilterTranslator;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;

/**
 * Generates translators which uses the older hand-written parser to perform
 * the translation.
 *
 * @author Gavin King
 */
public class ClassicQueryTranslatorFactory implements QueryTranslatorFactory {
	@Override
	public QueryTranslator createQueryTranslator(
			String queryIdentifier,
			String queryString,
			Map filters,
			SessionFactoryImplementor factory,
			EntityGraphQueryHint entityGraphQueryHint) {
		if ( entityGraphQueryHint != null ) {
			throw new QueryException( "EntityGraphs cannot be applied queries using the classic QueryTranslator!" );
		}
		return new QueryTranslatorImpl( queryIdentifier, queryString, filters, factory );
	}

	@Override
	public FilterTranslator createFilterTranslator(
			String queryIdentifier,
			String queryString,
			Map filters,
			SessionFactoryImplementor factory) {
		return new QueryTranslatorImpl( queryIdentifier, queryString, filters, factory );
	}

}
