/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.Collections;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.produce.internal.SqlAstProcessingStateImpl;
import org.hibernate.sql.ast.produce.sqm.internal.SqmDeleteInterpretationImpl;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmDeleteToSqlAstConverterSimple extends BaseSqmToSqlAstConverter {
	public static SqmDeleteInterpretation interpret(
			SqmDeleteStatement sqmStatement,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			SharedSessionContractImplementor session) {
		final SqmDeleteToSqlAstConverterSimple walker = new SqmDeleteToSqlAstConverterSimple(
				queryOptions,
				domainParameterXref,
				domainParameterBindings,
				session
		);

		walker.visitDeleteStatement( sqmStatement );

		return new SqmDeleteInterpretationImpl(
				Collections.singletonList( walker.deleteStatement ),
				Collections.singleton(
						walker.deleteStatement.getTargetTable().getTable().getTableExpression()
				),
				walker.getJdbcParamsBySqmParam()
		);
	}

	private DeleteStatement deleteStatement;

	public SqmDeleteToSqlAstConverterSimple(
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			SharedSessionContractImplementor session) {
		super( session.getFactory(), queryOptions, domainParameterXref, domainParameterBindings, session.getLoadQueryInfluencers(), afterLoadAction -> {} );
	}

	@Override
	public DeleteStatement visitDeleteStatement(SqmDeleteStatement sqmStatement) {
		getProcessingStateStack().push(
				new SqlAstProcessingStateImpl(
						null,
						this,
						getCurrentClauseStack()::getCurrent,
						() -> (expression) -> {}
				)
		);

		try {
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
		finally {
			getProcessingStateStack().pop();
		}
	}
}
