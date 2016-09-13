/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.ScrollMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.common.spi.SqmTypeImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.sql.sqm.ast.SelectQuery;
import org.hibernate.sql.sqm.convert.spi.Callback;
import org.hibernate.sql.sqm.convert.spi.SelectStatementInterpreter;
import org.hibernate.sql.sqm.exec.internal.PreparedStatementCreatorScrollableForwardOnlyImpl;
import org.hibernate.sql.sqm.exec.internal.PreparedStatementCreatorScrollableInsensitiveImpl;
import org.hibernate.sql.sqm.exec.internal.PreparedStatementCreatorScrollableSensitiveImpl;
import org.hibernate.sql.sqm.exec.internal.PreparedStatementCreatorStandardImpl;
import org.hibernate.sql.sqm.exec.internal.PreparedStatementExecutorNormalImpl;
import org.hibernate.sql.sqm.exec.internal.PreparedStatementExecutorScrollableImpl;
import org.hibernate.sql.sqm.exec.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.sqm.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.sqm.exec.internal.RowTransformerTupleImpl;
import org.hibernate.sql.sqm.exec.internal.SqlTreeExecutorImpl;
import org.hibernate.sql.sqm.exec.spi.PreparedStatementCreator;
import org.hibernate.sql.sqm.exec.spi.QueryOptions;
import org.hibernate.sql.sqm.exec.spi.RowTransformer;
import org.hibernate.sqm.query.SqmSelectStatement;
import org.hibernate.sqm.query.select.SqmSelection;

/**
 * @author Steve Ebersole
 */
public class ConcreteSqmSelectQueryPlan<R> implements SelectQueryPlan<R> {
	private final SqmSelectStatement sqm;
	private final EntityGraphQueryHint entityGraphHint;
	private final RowTransformer<R> rowTransformer;

	public ConcreteSqmSelectQueryPlan(
			SqmSelectStatement sqm,
			EntityGraphQueryHint entityGraphHint,
			Class<R> resultType,
			QueryOptions queryOptions) {
		this.sqm = sqm;
		this.entityGraphHint = entityGraphHint;

		this.rowTransformer = determineRowTransformer( sqm, resultType, queryOptions );
	}

	@SuppressWarnings("unchecked")
	private RowTransformer<R> determineRowTransformer(
			SqmSelectStatement sqm,
			Class<R> resultType,
			QueryOptions queryOptions) {
		if ( resultType != null ) {
			// an explicit return Type was requested
			if ( resultType.isArray() ) {
				return (RowTransformer<R>) RowTransformerPassThruImpl.INSTANCE;
			}

			if ( Tuple.class.isAssignableFrom( resultType ) ) {
				List<TupleElement<?>> tupleElements = new ArrayList<>();
				int i = 0;
				for ( SqmSelection selection : sqm.getQuerySpec().getSelectClause().getSelections() ) {
					tupleElements.add(
							new RowTransformerTupleImpl.HqlTupleElementImpl(
									( (SqmTypeImplementor) selection.getExpression().getExpressionType() ).getOrmType().getReturnedClass(),
									selection.getAlias()
							)
					);
				}

				return (RowTransformer<R>) new RowTransformerTupleImpl( tupleElements );
			}

			if ( sqm.getQuerySpec().getSelectClause().getSelections().size() > 1 ) {
				throw new IllegalQueryOperationException( "Query defined multiple selections, return cannot be typed (other that Object[] or Tuple)" );
			}
			else {
				return (RowTransformer<R>) RowTransformerSingularReturnImpl.INSTANCE;
			}
		}

		// otherwise just pass through the result row (as if Object[] were specified as return type)
		return (RowTransformer<R>) RowTransformerPassThruImpl.INSTANCE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<R> performList(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		verifyQueryIsSelect();

		final Callback callback = new Callback() {};
		final SelectQuery sqlTree = SelectStatementInterpreter.interpret( sqm, queryOptions, callback );
		return (List<R>) new SqlTreeExecutorImpl().executeSelect(
				sqlTree,
				PreparedStatementCreatorStandardImpl.INSTANCE,
				PreparedStatementExecutorNormalImpl.INSTANCE,
				queryOptions,
				inputParameterBindings,
				rowTransformer,
				callback,
				persistenceContext
		);
	}

	private void verifyQueryIsSelect() {
		if ( !SqmSelectStatement.class.isInstance( sqm ) ) {
			throw new IllegalQueryOperationException(
					"Query is not a SELECT statement [" + sqm.getClass().getSimpleName() + "]"
			);
		}
	}

	@Override
	public Iterator<R> performIterate(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		verifyQueryIsSelect();

		// todo : implement
		throw new NotYetImplementedException( "Query#iterate not yet implemented" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ScrollableResultsImplementor performScroll(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings,
			ScrollMode scrollMode) {
		verifyQueryIsSelect();

		final Callback callback = new Callback() {};
		final SelectQuery sqlTree = SelectStatementInterpreter.interpret( sqm, queryOptions, callback );

		final PreparedStatementCreator creator;
		if ( scrollMode == ScrollMode.FORWARD_ONLY ) {
			creator = PreparedStatementCreatorScrollableForwardOnlyImpl.INSTANCE;
		}
		else if ( scrollMode == ScrollMode.SCROLL_SENSITIVE ) {
			creator = PreparedStatementCreatorScrollableSensitiveImpl.INSTANCE;
		}
		else {
			creator = PreparedStatementCreatorScrollableInsensitiveImpl.INSTANCE;
		}

		return (ScrollableResultsImplementor) new SqlTreeExecutorImpl().executeSelect(
				sqlTree,
				creator,
				PreparedStatementExecutorScrollableImpl.INSTANCE,
				queryOptions,
				inputParameterBindings,
				rowTransformer,
				callback,
				persistenceContext
		);
	}
}
