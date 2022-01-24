/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.List;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.query.sqm.spi.EntityIdentifierNavigablePath;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.internal.EntityAssembler;
import org.hibernate.sql.results.graph.entity.internal.EntityResultInitializer;

/**
 * @author Steve Ebersole
 */
public class EntityResultImpl implements EntityResult {
	private final NavigablePath navigablePath;
	private final EntityValuedModelPart entityValuedModelPart;

	private final Fetch identifierFetch;
	private final BasicFetch<?> discriminatorFetch;
	private final List<Fetch> fetches;

	private final String resultAlias;
	private final LockMode lockMode;

	@SuppressWarnings( { "PointlessNullCheck" } )
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
									() -> p -> {},
									sqlAstCreationState,
									sqlAstCreationState.getCreationContext().getSessionFactory()
							);
				}
		);

		this.discriminatorFetch = discriminatorFetchBuilder.apply( this );

		this.fetches = creationState.visitFetches( this );

		final EntityIdentifierMapping identifierMapping = entityValuedModelPart
				.getEntityMappingType()
				.getIdentifierMapping();
		final String idAttributeName = identifierMapping instanceof SingleAttributeIdentifierMapping
				? ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName()
				: null;

		final MutableObject<Fetch> idFetchRef = new MutableObject<>();

		for ( int i = 0; i < this.fetches.size(); i++ ) {
			final Fetch fetch = this.fetches.get( i );
			final String fetchLocalName = fetch.getNavigablePath().getLocalName();

			if ( fetchLocalName.equals( EntityIdentifierMapping.ROLE_LOCAL_NAME )
					|| ( idAttributeName != null && fetchLocalName.equals( idAttributeName ) ) ) {
				// we found the id fetch
				idFetchRef.set( fetch );
				this.fetches.remove( i );
				break;
			}
		}

		if ( idFetchRef.isNotSet() ) {
			identifierFetch = ( (Fetchable) identifierMapping ).generateFetch(
					this,
					new EntityIdentifierNavigablePath(
							navigablePath,
							ResultsHelper.attributeName( identifierMapping )
					),
					FetchTiming.IMMEDIATE,
					true,
					null,
					creationState
			);
		}
		else {
			this.identifierFetch = idFetchRef.get();
		}
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
	public List<Fetch> getFetches() {
		return fetches;
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		for ( int i = 0; i < fetches.size(); i++ ) {
			if ( fetches.get( i ).getFetchedMapping().getFetchableName().equals( fetchable.getFetchableName() ) ) {
				return fetches.get( i );
			}
		}

		return null;
	}

	@Override
	public DomainResultAssembler<?> createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final EntityInitializer initializer = (EntityInitializer) creationState.resolveInitializer(
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

		return new EntityAssembler( getResultJavaType(), initializer );
	}
}
