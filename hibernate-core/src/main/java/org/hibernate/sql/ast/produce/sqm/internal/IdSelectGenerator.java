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
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

import org.jboss.logging.Logger;

/**
 * Specialized SqmSelectToSqlAstConverter extension to help in generating the
 * SELECT for selection of an entity's identifier columns, here specifically
 * intended to be used as the SELECT portion of an INSERT-SELECT.
 *
 * @author Steve Ebersole
 */
public class IdSelectGenerator extends SqmSelectToSqlAstConverter {
	private static final Logger log = Logger.getLogger( IdSelectGenerator.class );

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
	private final TableGroup rootTableGroup;

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

		SqlAstQuerySpecProcessingStateImpl processingState = new SqlAstQuerySpecProcessingStateImpl(
				idSelectQuerySpec,
				null,
				this,
				getCurrentClauseStack()::getCurrent,
				() -> expr -> {
				},
				() -> sqlSelection -> log.debugf( "Notified of SqlSelection [%s] via SqlAstProcessingState" )
		);

		getProcessingStateStack().push( processingState );

		this.rootTableGroup = entityDescriptor.createRootTableGroup(
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

		idSelectQuerySpec.getFromClause().addRoot( rootTableGroup );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultCreationState

	@Override
	protected QuerySpec currentQuerySpec() {
		return idSelectQuerySpec;
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
