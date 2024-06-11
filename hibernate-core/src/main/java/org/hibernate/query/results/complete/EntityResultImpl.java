/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.BitSet;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.internal.EntityAssembler;
import org.hibernate.sql.results.graph.entity.internal.EntityInitializerImpl;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;

/**
 * @author Steve Ebersole
 */
public class EntityResultImpl implements EntityResult, InitializerProducer<EntityResultImpl> {
	private final NavigablePath navigablePath;
	private final EntityValuedModelPart entityValuedModelPart;

	private final Fetch identifierFetch;
	private final BasicFetch<?> discriminatorFetch;
	private final ImmutableFetchList fetches;
	private final boolean hasJoinFetches;
	private final boolean containsCollectionFetches;

	private final String resultAlias;

	public EntityResultImpl(
			NavigablePath navigablePath,
			EntityValuedModelPart entityValuedModelPart,
			String resultAlias,
			LockMode lockMode,
			Function<EntityResultImpl, BasicFetch<?>> discriminatorFetchBuilder,
			DomainResultCreationState creationState) {
		this.navigablePath = navigablePath;
		this.entityValuedModelPart = entityValuedModelPart;
		this.resultAlias = resultAlias;

		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		if ( resultAlias != null ) {
			sqlAstCreationState.registerLockMode( resultAlias, lockMode );
		}
		sqlAstCreationState.getFromClauseAccess().resolveTableGroup(
				navigablePath,
				np -> {
					return entityValuedModelPart.getEntityMappingType()
							.getEntityPersister()
							.createRootTableGroup(
									true,
									navigablePath,
									null,
									null,
									() -> p -> {},
									sqlAstCreationState
							);
				}
		);

		this.identifierFetch = creationState.visitIdentifierFetch( this );
		this.discriminatorFetch = discriminatorFetchBuilder.apply( this );

		this.fetches = creationState.visitFetches( this );
		this.hasJoinFetches = fetches.hasJoinFetches();
		this.containsCollectionFetches = fetches.containsCollectionFetches();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EntityValuedModelPart getReferencedMappingType() {
		return entityValuedModelPart;
	}

	@Override
	public EntityValuedModelPart getEntityValuedModelPart() {
		return entityValuedModelPart;
	}

	@Override
	public String getResultVariable() {
		return resultAlias;
	}

	@Override
	public ImmutableFetchList getFetches() {
		return fetches;
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		return fetches.get( fetchable );
	}

	@Override
	public boolean hasJoinFetches() {
		return hasJoinFetches;
	}

	@Override
	public boolean containsCollectionFetches() {
		return containsCollectionFetches;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		final EntityPersister entityPersister = entityValuedModelPart.getEntityMappingType().getEntityPersister();
		identifierFetch.collectValueIndexesToCache( valueIndexes );
		if ( !entityPersister.useShallowQueryCacheLayout() ) {
			if ( discriminatorFetch != null ) {
				discriminatorFetch.collectValueIndexesToCache( valueIndexes );
			}
			EntityResult.super.collectValueIndexesToCache( valueIndexes );
		}
		else if ( entityPersister.storeDiscriminatorInShallowQueryCacheLayout() && discriminatorFetch != null ) {
			discriminatorFetch.collectValueIndexesToCache( valueIndexes );
		}
	}

	@Override
	public DomainResultAssembler<?> createResultAssembler(
			InitializerParent parent,
			AssemblerCreationState creationState) {
		return new EntityAssembler( getResultJavaType(), creationState.resolveInitializer( this, parent, this ).asEntityInitializer() );
	}

	@Override
	public Initializer<?> createInitializer(
			EntityResultImpl resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parent, creationState );
	}

	@Override
	public Initializer<?> createInitializer(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return new EntityInitializerImpl(
				this,
				resultAlias,
				identifierFetch,
				discriminatorFetch,
				null,
				null,
				NotFoundAction.EXCEPTION,
				false,
				null,
				true,
				creationState
		);
	}
}
