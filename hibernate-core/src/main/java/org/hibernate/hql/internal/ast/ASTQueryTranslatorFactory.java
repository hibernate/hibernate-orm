/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;

import java.util.Map;

import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.spi.FilterTranslator;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Generates translators which uses the Antlr-based parser to perform
 * the translation.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ASTQueryTranslatorFactory implements QueryTranslatorFactory {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ASTQueryTranslatorFactory.class );

	/**
	 * Singleton access
	 */
	public static final ASTQueryTranslatorFactory INSTANCE = new ASTQueryTranslatorFactory();

	public ASTQueryTranslatorFactory() {
	}

	@Override
	public QueryTranslator createQueryTranslator(
			String queryIdentifier,
			String queryString,
			Map filters,
			SessionFactoryImplementor factory,
			EntityGraphQueryHint entityGraphQueryHint) {
		return new QueryTranslatorImpl( queryIdentifier, queryString, filters, factory, entityGraphQueryHint );
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
