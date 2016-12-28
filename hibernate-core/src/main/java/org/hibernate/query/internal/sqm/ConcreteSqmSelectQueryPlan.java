/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal.sqm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.ScrollMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.streams.StingArrayCollector;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.persister.common.spi.OrmTypeExporter;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.JpaTupleBuilder;
import org.hibernate.query.spi.EntityGraphQueryHint;
import org.hibernate.query.spi.ExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.convert.spi.Callback;
import org.hibernate.sql.convert.spi.SqmSelectInterpretation;
import org.hibernate.sql.convert.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.internal.RowTransformerTupleImpl;
import org.hibernate.sql.exec.internal.RowTransformerTupleTransformerAdapter;
import org.hibernate.sql.exec.internal.TupleElementImpl;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.RowTransformer;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.sqm.domain.DomainMetamodel;
import org.hibernate.sqm.query.SqmSelectStatement;
import org.hibernate.sqm.query.select.SqmSelection;

/**
 * @author Steve Ebersole
 */
public class ConcreteSqmSelectQueryPlan<R> implements SelectQueryPlan<R> {
	private final SqmSelectStatement sqm;
	private final DomainMetamodel domainMetamodel;
	private final EntityGraphQueryHint entityGraphHint;
	private final RowTransformer<R> rowTransformer;

	public ConcreteSqmSelectQueryPlan(
			SqmSelectStatement sqm,
			DomainMetamodel domainMetamodel,
			EntityGraphQueryHint entityGraphHint,
			Class<R> resultType,
			QueryOptions queryOptions) {
		this.sqm = sqm;
		this.domainMetamodel = domainMetamodel;
		this.entityGraphHint = entityGraphHint;

		this.rowTransformer = determineRowTransformer( sqm, resultType, queryOptions );
	}

	@SuppressWarnings("unchecked")
	private RowTransformer<R> determineRowTransformer(
			SqmSelectStatement sqm,
			Class<R> resultType,
			QueryOptions queryOptions) {
		if ( resultType == null || resultType.isArray() ) {
			if ( queryOptions.getTupleTransformer() != null ) {
				return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
			}
			else {
				return RowTransformerPassThruImpl.instance();
			}
		}

		// NOTE : if we get here, a result-type of some kind (other than Object[].class) was specified

		if ( Tuple.class.isAssignableFrom( resultType ) ) {
			// resultType is Tuple..
			if ( queryOptions.getTupleTransformer() == null ) {
				final List<TupleElement<?>> tupleElementList = new ArrayList<>();
				for ( SqmSelection selection : sqm.getQuerySpec().getSelectClause().getSelections() ) {
					tupleElementList.add(
							new TupleElementImpl(
									( (OrmTypeExporter) selection.getExpression().getExpressionType() ).getOrmType().getReturnedClass(),
									selection.getAlias()
							)
					);
				}
				return (RowTransformer<R>) new RowTransformerTupleImpl( tupleElementList );
//				return (RowTransformer<R>) new RowTransformerTupleImpl(
//						sqm.getQuerySpec().getSelectClause().getSelections()
//								.stream()
//								.map( selection -> (TupleElement<?>) new TupleElementImpl(
//										( (SqmTypeImplementor) selection.getExpression().getExpressionType() ).getOrmType().getReturnedClass(),
//										selection.getAlias()
//								) )
//								.collect( Collectors.toList() )
//				);
			}

			// there can be a TupleTransformer IF it is a JpaTupleBuilder,
			// otherwise this is considered an error
			if ( queryOptions.getTupleTransformer() instanceof JpaTupleBuilder ) {
				return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
			}

			throw new IllegalArgumentException(
					"Illegal combination of Tuple resultType and (non-JpaTupleBuilder) TupleTransformer : " +
							queryOptions.getTupleTransformer()
			);
		}

		// NOTE : if we get here we have a resultType of some kind

		if ( queryOptions.getTupleTransformer() != null ) {
			// aside from checking the type parameters for the given TupleTransformer
			// there is not a decent way to verify that the TupleTransformer returns
			// the same type.  We rely on the API here and assume the best
			return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
		}
		else if ( sqm.getQuerySpec().getSelectClause().getSelections().size() > 1 ) {
			throw new IllegalQueryOperationException( "Query defined multiple selections, return cannot be typed (other that Object[] or Tuple)" );
		}
		else {
			return RowTransformerSingularReturnImpl.instance();
		}
	}

	@SuppressWarnings("unchecked")
	private RowTransformer makeRowTransformerTupleTransformerAdapter(
			SqmSelectStatement sqm,
			QueryOptions queryOptions) {
		return new RowTransformerTupleTransformerAdapter<>(
				sqm.getQuerySpec().getSelectClause().getSelections()
						.stream()
						.map( SqmSelection::getAlias )
						.collect( StingArrayCollector.INSTANCE ),
				queryOptions.getTupleTransformer()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<R> performList(
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		verifyQueryIsSelect();

		boolean shallow = false;

		final Callback callback = new Callback() {
			@Override
			public void registerAfterLoadAction(AfterLoadAction afterLoadAction) {
				// do nothing here
			}
		};

		// todo : SelectStatementInterpreter needs to account for the EntityGraph hint
		final SqmSelectInterpretation interpretation = SqmSelectToSqlAstConverter.interpret(
				sqm,
				persistenceContext.getFactory(),
				domainMetamodel,
				queryOptions,
				shallow,
				callback
		);

		final JdbcSelect jdbcSelect = SqlAstSelectInterpreter.interpret(
				interpretation,
				shallow,
				persistenceContext.getFactory(),
				inputParameterBindings,
				executionContext
		);

		return new JdbcSelectExecutorStandardImpl().list(
				jdbcSelect,
				queryOptions,
				inputParameterBindings,
				rowTransformer,
				callback,
				persistenceContext,
				executionContext
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
			ExecutionContext executionContext,
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
			ExecutionContext executionContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings,
			ScrollMode scrollMode) {
		verifyQueryIsSelect();

		boolean shallow = false;

		final Callback callback = new Callback() {
			@Override
			public void registerAfterLoadAction(AfterLoadAction afterLoadAction) {
				// do nothing here
			}
		};

		// todo : SelectStatementInterpreter needs to account for the EntityGraph hint
		final SqmSelectInterpretation interpretation = SqmSelectToSqlAstConverter.interpret(
				sqm,
				persistenceContext.getFactory(),
				domainMetamodel,
				queryOptions,
				shallow,
				callback
		);

		final JdbcSelect jdbcSelect = SqlAstSelectInterpreter.interpret(
				interpretation,
				shallow,
				persistenceContext.getFactory(),
				inputParameterBindings,
				executionContext
		);

		return new JdbcSelectExecutorStandardImpl().scroll(
				jdbcSelect,
				scrollMode,
				queryOptions,
				inputParameterBindings,
				rowTransformer,
				callback,
				persistenceContext,
				executionContext
		);
	}
}
