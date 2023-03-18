/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.internal.EntityAssembler;
import org.hibernate.sql.results.graph.entity.internal.EntityResultInitializer;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;

/**
 * @author Steve Ebersole
 */
public class EntityResultImpl implements EntityResult {
	private final NavigablePath navigablePath;
	private final EntityValuedModelPart entityValuedModelPart;

	private final Fetch identifierFetch;
	private final BasicFetch<?> discriminatorFetch;
	private final ImmutableFetchList fetches;
	private final boolean hasJoinFetches;
	private final boolean containsCollectionFetches;

	private final String resultAlias;
	private final LockMode lockMode;

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
		this.lockMode = lockMode;


		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
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
	public DomainResultAssembler<?> createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final Initializer initializer = creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> new EntityResultInitializer(
						this,
						getNavigablePath(),
						lockMode,
						identifierFetch,
						discriminatorFetch,
						null,
						creationState
				)
		);

		return new EntityAssembler( getResultJavaType(), initializer.asEntityInitializer() );
	}
}
