/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.sql.internal.SqlAstProcessingStateImpl;
import org.hibernate.query.sqm.sql.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * Specialized BaseSqmToSqlAstConverter implementation used during conversion
 * of an SQM mutation query tree representing into the various SQL AST trees
 * needed to perform that operation.
 *
 * @see #visitSetClause(SqmSetClause, Consumer, BiConsumer)
 * @see #visitWhereClause(SqmWhereClause, Consumer, BiConsumer)
 * @see #visitSelectClause(SqmSelectClause, QuerySpec, Consumer, BiConsumer)
 *
 * @author Steve Ebersole
 */
public class MultiTableSqmMutationConverter extends BaseSqmToSqlAstConverter implements DomainResultCreationState {
	private final EntityMappingType mutatingEntityDescriptor;
	private final TableGroup mutatingTableGroup;

	private BiConsumer<SqmParameter, List<JdbcParameter>> parameterResolutionConsumer;

	public MultiTableSqmMutationConverter(
			EntityMappingType mutatingEntityDescriptor,
			DomainParameterXref domainParameterXref,
			QueryOptions queryOptions,
			QueryParameterBindings domainParameterBindings,
			SqlAstCreationContext creationContext) {
		super( creationContext, queryOptions, domainParameterXref, domainParameterBindings );
		this.mutatingEntityDescriptor = mutatingEntityDescriptor;

		final SqlAstProcessingStateImpl rootProcessingState = new SqlAstProcessingStateImpl(
				null,
				this,
				getCurrentClauseStack()::getCurrent
		);

		getProcessingStateStack().push( rootProcessingState );

		final NavigablePath navigablePath = new NavigablePath( mutatingEntityDescriptor.getEntityName() );
		this.mutatingTableGroup = mutatingEntityDescriptor.createRootTableGroup(
				navigablePath,
				null,
				true,
				LockMode.PESSIMISTIC_WRITE,
				getSqlAliasBaseGenerator(),
				getSqlExpressionResolver(),
				() -> predicate -> {
				},
				creationContext.getSessionFactory()
		);

		getFromClauseAccess().registerTableGroup( navigablePath, mutatingTableGroup );
	}

	@SuppressWarnings("unused")
	public EntityMappingType getMutatingEntityDescriptor() {
		return mutatingEntityDescriptor;
	}

	public TableGroup getMutatingTableGroup() {
		return mutatingTableGroup;
	}

	@Override
	public Stack<SqlAstProcessingState> getProcessingStateStack() {
		return super.getProcessingStateStack();
	}

	/**
	 * Specialized hook to visit the assignments defined by the update SQM allow
	 * "listening" for each SQL assignment.
	 */
	public void visitSetClause(
			SqmSetClause setClause,
			Consumer<Assignment> assignmentConsumer,
			BiConsumer<SqmParameter, List<JdbcParameter>> parameterResolutionConsumer) {
		this.parameterResolutionConsumer = parameterResolutionConsumer;

		for ( SqmAssignment assignment : setClause.getAssignments() ) {
			visitAssignment( assignment, assignmentConsumer );
		}
	}

	public List<Assignment> visitSetClause(SqmSetClause setClause) {
		throw new UnsupportedOperationException();
	}

	private void visitAssignment(
			SqmAssignment sqmAssignment,
			Consumer<Assignment> assignmentConsumer) {
		final Assignable assignable = (Assignable) sqmAssignment.getTargetPath().accept( this );

		final Expression value = (Expression) sqmAssignment.getValue().accept( this );

		assignmentConsumer.accept( new Assignment( assignable, value ) );
	}

	@Override
	public Assignment visitAssignment(SqmAssignment sqmAssignment) {
		return new Assignment(
				(Assignable) sqmAssignment.getTargetPath().accept( this ),
				(Expression) sqmAssignment.getValue().accept( this )
		);
	}

	public Predicate visitWhereClause(
			SqmWhereClause sqmWhereClause,
			Consumer<ColumnReference> restrictionColumnReferenceConsumer,
			BiConsumer<SqmParameter, List<JdbcParameter>> parameterResolutionConsumer) {
		this.parameterResolutionConsumer = parameterResolutionConsumer;

		if ( sqmWhereClause == null || sqmWhereClause.getPredicate() == null ) {
			return null;
		}

		final SqlAstProcessingState rootProcessingState = getProcessingStateStack().getCurrent();
		final SqlAstProcessingStateImpl restrictionProcessingState = new SqlAstProcessingStateImpl(
				rootProcessingState,
				this,
				getCurrentClauseStack()::getCurrent
		) {
			@Override
			public SqlExpressionResolver getSqlExpressionResolver() {
				return this;
			}

			@Override
			public Expression resolveSqlExpression(
					String key, Function<SqlAstProcessingState, Expression> creator) {
				final Expression expression = rootProcessingState.getSqlExpressionResolver().resolveSqlExpression(
						key,
						creator
				);
				if ( expression instanceof ColumnReference ) {
					restrictionColumnReferenceConsumer.accept( (ColumnReference) expression );
				}
				return expression;
			}
		};

		getProcessingStateStack().push( restrictionProcessingState );
		try {
			return (Predicate) sqmWhereClause.getPredicate().accept( this );
		}
		finally {
			getProcessingStateStack().pop();
			this.parameterResolutionConsumer = null;
		}
	}

	@Override
	public Predicate visitWhereClause(SqmWhereClause whereClause) {
		return (Predicate) super.visitWhereClause( whereClause );
	}

	@Override
	protected Expression consumeSqmParameter(SqmParameter sqmParameter) {
		assert parameterResolutionConsumer != null;

		final Expression expression = super.consumeSqmParameter( sqmParameter );

		final List<JdbcParameter> jdbcParameters = getJdbcParamsBySqmParam().get( sqmParameter );
		parameterResolutionConsumer.accept( sqmParameter, jdbcParameters );

		return expression;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		return Collections.emptyList();
	}

	public void visitSelectClause(
			SqmSelectClause sqmSelectClause,
			QuerySpec sqlQuerySpec,
			Consumer<ColumnReference> columnReferenceConsumer,
			BiConsumer<SqmParameter, List<JdbcParameter>> parameterResolutionConsumer) {
		assert sqmSelectClause != null;

		this.parameterResolutionConsumer = parameterResolutionConsumer;

		final SqlAstProcessingState rootProcessingState = getProcessingStateStack().getCurrent();
		final SqlAstProcessingStateImpl processingState = new SqlAstQuerySpecProcessingStateImpl(
				sqlQuerySpec,
				rootProcessingState,
				this,
				getCurrentClauseStack()::getCurrent
		) {
			@Override
			public SqlExpressionResolver getSqlExpressionResolver() {
				return this;
			}

			@Override
			public Expression resolveSqlExpression(
					String key, Function<SqlAstProcessingState, Expression> creator) {
				final Expression expression = rootProcessingState.getSqlExpressionResolver().resolveSqlExpression(
						key,
						creator
				);
				if ( expression instanceof ColumnReference ) {
					columnReferenceConsumer.accept( (ColumnReference) expression );
				}
				return expression;
			}
		};

		getProcessingStateStack().push( processingState );
		try {
			for ( int i = 0; i < sqmSelectClause.getSelectionItems().size(); i++ ) {
				final DomainResultProducer domainResultProducer = (DomainResultProducer) sqmSelectClause.getSelectionItems()
						.get( i )
						.accept( this );
				domainResultProducer.applySqlSelections( this );
			}
		}
		finally {
			getProcessingStateStack().pop();
			this.parameterResolutionConsumer = null;
		}
	}
}
