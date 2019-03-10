/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.EmptyStack;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.model.domain.internal.entity.EntityTableGroup;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.internal.StandardSqlExpressionResolver;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * Specialized SqmSelectToSqlAstConverter extension to help in generating the
 * SELECT for selection of an entity's identifier columns, here specifically
 * intended to be used as the SELECT portion of an INSERT-SELECT.
 *
 * @author Steve Ebersole
 */
public class IdSelectGenerator extends SqmSelectToSqlAstConverter {

	public static QuerySpec generateEntityIdSelect(
			EntityTypeDescriptor entityDescriptor,
			SqmDeleteOrUpdateStatement sourceSqmStatement,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			SessionFactoryImplementor sessionFactory) {
		final IdSelectGenerator generator = new IdSelectGenerator(
				sourceSqmStatement,
				entityDescriptor,
				queryOptions,
				loadQueryInfluencers,
				afterLoadAction -> {},
				sessionFactory
		);
		return generator.process();
	}

	private final SqmDeleteOrUpdateStatement sourceSqmStatement;
	private final EntityTypeDescriptor entityDescriptor;

	private final QuerySpec idSelectQuerySpec;
	private final TableSpace idSelectTableSpace;

	private final StandardSqlExpressionResolver expressionResolver;

	public IdSelectGenerator(
			SqmDeleteOrUpdateStatement sourceSqmStatement,
			EntityTypeDescriptor entityDescriptor,
			QueryOptions queryOptions,
			LoadQueryInfluencers influencers,
			Callback callback,
			SqlAstCreationContext creationContext) {
		super( queryOptions, influencers, callback, creationContext );
		this.sourceSqmStatement = sourceSqmStatement;
		this.entityDescriptor = entityDescriptor;

		this.idSelectQuerySpec = new QuerySpec( true );

		final EntityTableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
				sourceSqmStatement.getTarget().getUniqueIdentifier(),
				sourceSqmStatement.getTarget().getNavigablePath(),
				sourceSqmStatement.getTarget().getExplicitAlias(),
				JoinType.INNER,
				LockMode.NONE,
				this.getSqlAstCreationState()
		);

		// this allows where-clause restrictions from the source query to
		// 	resolve to this TableGroup for the entity for the id select
		getFromClauseIndex().crossReference( sourceSqmStatement.getTarget(), rootTableGroup );

//		getFromClauseIndex().registerTableGroup( rootTableGroup.getNavigablePath(), rootTableGroup );

		this.idSelectTableSpace = idSelectQuerySpec.getFromClause().makeTableSpace();
		idSelectTableSpace.setRootTableGroup( rootTableGroup );

		this.expressionResolver = new StandardSqlExpressionResolver(
				() -> idSelectQuerySpec,
				expression -> expression,
				(expression, sqlSelection) -> {}
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultCreationState

	@Override
	public Stack<ColumnReferenceQualifier> getColumnReferenceQualifierStack() {
		return EmptyStack.instance();
	}

	@Override
	public Stack<NavigableReference> getNavigableReferenceStack() {
		return EmptyStack.instance();
	}

	@Override
	public boolean fetchAllAttributes() {
		return false;
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return expressionResolver;
	}

	@Override
	protected QuerySpec currentQuerySpec() {
		return idSelectQuerySpec;
	}

	@Override
	public TableSpace getCurrentTableSpace() {
		return idSelectTableSpace;
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		final List<Fetch> fetches = new ArrayList<>();
		final Consumer<Fetchable> fetchableConsumer = fetchable -> {
			if ( fetchParent.findFetch( fetchable.getNavigableName() ) != null ) {
				return;
			}

			fetches.add(
					fetchable.generateFetch(
							fetchParent,
							FetchTiming.IMMEDIATE,
							true,
							LockMode.NONE,
							null,
							this
					)
			);
		};

		try {
			NavigableContainer navigableContainer = fetchParent.getNavigableContainer();
			navigableContainer.visitKeyFetchables( fetchableConsumer );
			navigableContainer.visitFetchables( fetchableConsumer );
		}
		finally {

		}

		return fetches;
	}

	private QuerySpec process() {
		// NOTE : we ignore the created DomainResult - we only care about the generated/resolved SqlSelections
		NavigablePath idNavigablePath = sourceSqmStatement.getTarget()
				.getNavigablePath()
				.append( EntityIdentifier.NAVIGABLE_ID );
		entityDescriptor.getIdentifierDescriptor().createDomainResult(
				idNavigablePath,
				// no domain alias
				null,
				this
		);

		applyQueryRestrictions();

		return idSelectQuerySpec;
	}

	private void applyQueryRestrictions() {
		final SqmWhereClause whereClause = sourceSqmStatement.getWhereClause();
		if ( whereClause == null ) {
			return;
		}

		final SqmPredicate predicate = whereClause.getPredicate();
		if ( predicate == null ) {
			return;
		}

		if ( predicate instanceof Junction ) {
			final Junction junction = (Junction) predicate;
			if ( junction.isEmpty() ) {
				// todo (6.0) : ? - should it matter a conjunction versus a disjunction in terms of all versus none like we do in other places?
				return;
			}
		}

		predicate.accept( this );
	}

}
