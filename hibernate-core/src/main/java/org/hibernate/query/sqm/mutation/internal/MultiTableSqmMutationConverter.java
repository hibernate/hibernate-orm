/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.SqlAstProcessingStateImpl;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstHelper;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.ast.tree.update.Assignment;

/**
 * Specialized BaseSqmToSqlAstConverter implementation used during conversion
 * of an SQM mutation query tree representing into the various SQL AST trees
 * needed to perform that operation.
 *
 * @see #visitSetClause(SqmSetClause, Consumer, SqmParameterResolutionConsumer)
 * @see #visitWhereClause(SqmWhereClause, Consumer, SqmParameterResolutionConsumer)
 *
 * @author Steve Ebersole
 */
public class MultiTableSqmMutationConverter extends BaseSqmToSqlAstConverter<Statement> {
	public interface SqmParameterResolutionConsumer {
		void accept(SqmParameter<?> sqmParam, MappingModelExpressible<?> mappingType, List<JdbcParameter> jdbcParameters);
	}

	private final EntityMappingType mutatingEntityDescriptor;
	private final TableGroup mutatingTableGroup;
	private Predicate discriminatorPredicate;

	private SqmParameterResolutionConsumer parameterResolutionConsumer;

	public MultiTableSqmMutationConverter(
			EntityMappingType mutatingEntityDescriptor,
			SqmStatement<?> statement,
			SqmRoot<?> sqmRoot,
			DomainParameterXref domainParameterXref,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			QueryParameterBindings domainParameterBindings,
			SqlAstCreationContext creationContext) {
		this(
				mutatingEntityDescriptor,
				statement,
				sqmRoot,
				sqmRoot.getExplicitAlias(),
				domainParameterXref,
				queryOptions,
				loadQueryInfluencers,
				domainParameterBindings,
				creationContext
		);
	}

	public MultiTableSqmMutationConverter(
			EntityMappingType mutatingEntityDescriptor,
			SqmStatement<?> statement,
			SqmRoot<?> sqmRoot,
			String sourceAlias,
			DomainParameterXref domainParameterXref,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			QueryParameterBindings domainParameterBindings,
			SqlAstCreationContext creationContext) {
		super(
				creationContext,
				statement,
				queryOptions,
				loadQueryInfluencers,
				domainParameterXref,
				domainParameterBindings,
				false
		);
		this.mutatingEntityDescriptor = mutatingEntityDescriptor;

		final SqlAstProcessingStateImpl rootProcessingState = new SqlAstProcessingStateImpl(
				null,
				this,
				getCurrentClauseStack()::getCurrent
		);

		pushProcessingState( rootProcessingState );

		this.mutatingTableGroup = mutatingEntityDescriptor.createRootTableGroup(
				true,
				sqmRoot.getNavigablePath(),
				sourceAlias,
				null,
				() -> (predicate) -> {
					this.discriminatorPredicate = predicate;
				},
				this
		);

		getFromClauseAccess().registerTableGroup( sqmRoot.getNavigablePath(), mutatingTableGroup );
	}

	@Override
	public void pruneTableGroupJoins() {
		super.pruneTableGroupJoins();
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
	 * Specialized hook to visit the assignments defined by the update SQM.
	 */
	public void visitSetClause(
			SqmSetClause setClause,
			Consumer<Assignment> assignmentConsumer,
			SqmParameterResolutionConsumer parameterResolutionConsumer) {
		this.parameterResolutionConsumer = parameterResolutionConsumer;

		final List<Assignment> assignments = super.visitSetClause( setClause );
		for ( Assignment assignment : assignments ) {
			assignmentConsumer.accept( assignment );
		}
	}

	public List<Assignment> visitSetClause(SqmSetClause setClause) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Specialized hook to visit the target paths defined by the insert SQM.
	 */
	public AdditionalInsertValues visitInsertionTargetPaths(
			BiConsumer<Assignable, List<ColumnReference>> targetColumnReferenceConsumer,
			SqmInsertStatement<?> sqmStatement,
			EntityPersister entityDescriptor,
			TableGroup tableGroup,
			SqmParameterResolutionConsumer parameterResolutionConsumer) {
		this.parameterResolutionConsumer = parameterResolutionConsumer;
		return visitInsertionTargetPaths( targetColumnReferenceConsumer, sqmStatement, entityDescriptor, tableGroup );
	}

	public Predicate visitWhereClause(
			SqmWhereClause sqmWhereClause,
			Consumer<ColumnReference> restrictionColumnReferenceConsumer,
			SqmParameterResolutionConsumer parameterResolutionConsumer) {
		this.parameterResolutionConsumer = parameterResolutionConsumer;

		if ( sqmWhereClause == null || sqmWhereClause.getPredicate() == null ) {
			return null;
		}

		final SqlAstProcessingState rootProcessingState = getCurrentProcessingState();
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
					ColumnReferenceKey key, Function<SqlAstProcessingState, Expression> creator) {
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

		pushProcessingState( restrictionProcessingState, getFromClauseIndex() );
		try {
			return SqlAstHelper.combinePredicates(
					(Predicate) sqmWhereClause.getPredicate().accept( this ),
					discriminatorPredicate
			);
		}
		finally {
			popProcessingStateStack();
			this.parameterResolutionConsumer = null;
		}
	}

	@Override
	public Predicate visitWhereClause(SqmWhereClause whereClause) {
		return (Predicate) super.visitWhereClause( whereClause );
	}

	@Override
	protected Expression consumeSqmParameter(
			SqmParameter<?> sqmParameter,
			MappingModelExpressible<?> valueMapping,
			BiConsumer<Integer, JdbcParameter> jdbcParameterConsumer) {
		assert parameterResolutionConsumer != null;

		final Expression expression = super.consumeSqmParameter( sqmParameter, valueMapping, jdbcParameterConsumer );

		final List<List<JdbcParameter>> jdbcParameters = getJdbcParamsBySqmParam().get( sqmParameter );
		final MappingModelExpressible<?> mappingType = getSqmParameterMappingModelExpressibleResolutions().get( sqmParameter );
		parameterResolutionConsumer.accept(
				sqmParameter,
				mappingType,
				jdbcParameters.get( jdbcParameters.size() - 1 )
		);

		return expression;
	}

}
