/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.ast;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.internal.StandardSqmTranslator;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * @author Steve Ebersole
 */
public class SqlAstHelper {
	public static SelectStatement translateHqlSelectQuery(String hql, Class<?> returnType, SessionImplementor session) {
		final QueryImplementor<?> query = session.createQuery( hql, returnType );
		final QuerySqmImpl<?> hqlQuery = (QuerySqmImpl<?>) query;
		final SqmSelectStatement<?> sqmStatement = (SqmSelectStatement<?>) hqlQuery.getSqmStatement();

		final StandardSqmTranslator<SelectStatement> sqmConverter = new StandardSqmTranslator<>(
				sqmStatement,
				hqlQuery.getQueryOptions(),
				hqlQuery.getDomainParameterXref(),
				query.getParameterBindings(),
				session.getLoadQueryInfluencers(),
				session.getFactory(),
				true
		);

		final SqmTranslation<SelectStatement> sqmInterpretation = sqmConverter.translate();
		return sqmInterpretation.getSqlAst();
	}
}
