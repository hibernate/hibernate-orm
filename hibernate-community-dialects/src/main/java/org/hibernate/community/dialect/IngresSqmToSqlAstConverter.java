/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;

/**
 * A SQM to SQL AST translator for Ingres.
 *
 * @author Christian Beikov
 */
public class IngresSqmToSqlAstConverter<T extends Statement> extends BaseSqmToSqlAstConverter<T> {

	private boolean needsDummyTableGroup;

	public IngresSqmToSqlAstConverter(
			SqmStatement<?> statement,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers fetchInfluencers,
			SqlAstCreationContext creationContext,
			boolean deduplicateSelectionItems) {
		super(
				creationContext,
				statement,
				queryOptions,
				fetchInfluencers,
				domainParameterXref,
				domainParameterBindings,
				deduplicateSelectionItems
		);
	}

	@Override
	public QuerySpec visitQuerySpec(SqmQuerySpec<?> sqmQuerySpec) {
		final boolean needsDummy = this.needsDummyTableGroup;
		this.needsDummyTableGroup = false;
		try {
			final QuerySpec querySpec = super.visitQuerySpec( sqmQuerySpec );
			if ( this.needsDummyTableGroup ) {
				querySpec.getFromClause().addRoot(
						new StandardTableGroup(
								true,
								null,
								null,
								null,
								new NamedTableReference( "(select 1)", "dummy_(x)" ),
								null,
								getCreationContext().getSessionFactory()
						)
				);
			}
			return querySpec;
		}
		finally {
			this.needsDummyTableGroup = needsDummy;
		}
	}

	@Override
	protected Expression resolveGroupOrOrderByExpression(SqmExpression<?> groupByClauseExpression) {
		final Expression expression = super.resolveGroupOrOrderByExpression( groupByClauseExpression );
		if ( expression instanceof Literal ) {
			// Note that SqlAstTranslator.renderPartitionItem depends on this
			this.needsDummyTableGroup = true;
		}
		return expression;
	}
}
