/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.SqlAstProcessingStateImpl;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * Specialized BaseSqmToSqlAstConverter implementation used during conversion
 * of an SQM mutation query tree representing into the various SQL AST trees
 * needed to perform that operation.
 *
 * @author Steve Ebersole
 */
public class MultiTableSqmMutationConverter extends BaseSqmToSqlAstConverter<Statement> {

	private final EntityMappingType mutatingEntityDescriptor;
	private final TableGroup mutatingTableGroup;
	private Predicate discriminatorPredicate;

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
					assert this.discriminatorPredicate == null;
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

	@Override
	public Predicate visitWhereClause(SqmWhereClause whereClause) {
		return SqlAstTreeHelper.combinePredicates( super.visitWhereClause( whereClause ), discriminatorPredicate );
	}

}
