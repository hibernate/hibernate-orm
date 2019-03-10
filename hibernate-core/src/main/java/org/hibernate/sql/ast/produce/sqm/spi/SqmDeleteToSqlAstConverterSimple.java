/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.produce.internal.NonSelectSqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.DeleteStatement;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmDeleteToSqlAstConverterSimple extends BaseSqmToSqlAstConverter {
	public static DeleteStatement interpret(
			SqmDeleteStatement sqmStatement,
			QueryOptions queryOptions,
			SharedSessionContractImplementor session) {
		final SqmDeleteToSqlAstConverterSimple walker = new SqmDeleteToSqlAstConverterSimple(
				queryOptions,
				session
		);
		walker.visitDeleteStatement( sqmStatement );
		return walker.deleteStatement;
	}

	private NonSelectSqlExpressionResolver expressionResolver;
	private DeleteStatement deleteStatement;

	private SqmDeleteToSqlAstConverterSimple(
			QueryOptions queryOptions,
			SharedSessionContractImplementor session) {
		super( session.getFactory(), queryOptions, session.getLoadQueryInfluencers(), afterLoadAction -> {} );
		this.expressionResolver = new NonSelectSqlExpressionResolver(
				session.getSessionFactory(),
				() -> getQuerySpecStack().getCurrent(),
				this::normalizeSqlExpression,
				this::collectSelection
		);
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return expressionResolver;
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement sqmStatement) {
		final Predicate restriction;
		if ( sqmStatement.getWhereClause() != null && sqmStatement.getWhereClause().getPredicate() != null ) {
			restriction = (Predicate) sqmStatement.getWhereClause().getPredicate().accept( this );
		}
		else {
			restriction = null;
		}

		deleteStatement = new DeleteStatement(
				new TableReference(
						sqmStatement.getTarget().getReferencedNavigable().getEntityDescriptor().getPrimaryTable(),
						null,
						false
				),
				restriction
		);
		return deleteStatement;
	}
}
