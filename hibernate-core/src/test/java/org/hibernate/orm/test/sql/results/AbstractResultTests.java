/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.results;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * @author Steve Ebersole
 */
public class AbstractResultTests {
	protected SelectStatement interpret(String hql, SessionFactoryImplementor sessionFactory) {
		return interpret( hql, QueryParameterBindingsImpl.EMPTY, sessionFactory );
	}

	protected SelectStatement interpret(String hql, QueryParameterBindings parameterBindings, SessionFactoryImplementor sessionFactory) {
		final QueryEngine queryEngine = sessionFactory.getQueryEngine();

		final SqmSelectStatement<Object> sqm = (SqmSelectStatement<Object>)
				queryEngine.getHqlTranslator().translate( hql, null );

		return queryEngine.getSqmTranslatorFactory()
				.createSelectTranslator(
						sqm,
						QueryOptions.NONE,
						DomainParameterXref.from( sqm ),
						parameterBindings,
						new LoadQueryInfluencers( sessionFactory ),
						sessionFactory,
						true
				)
				.translate()
				.getSqlAst();
	}
}
