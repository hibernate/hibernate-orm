/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.BitSet;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityFetchJoinedImpl extends AbstractFetchParent implements EntityFetch,
		InitializerProducer<EntityFetchJoinedImpl> {
	private final FetchParent fetchParent;
	private final EntityValuedFetchable fetchContainer;
	private final EntityResultImpl entityResult;
	private final DomainResult<?> keyResult;
	private final NotFoundAction notFoundAction;

	private final String sourceAlias;

	public EntityFetchJoinedImpl(
			FetchParent fetchParent,
			ToOneAttributeMapping toOneMapping,
			TableGroup tableGroup,
			DomainResult<?> keyResult,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		super( navigablePath );
		this.fetchContainer = toOneMapping;
		this.fetchParent = fetchParent;
		this.keyResult = keyResult;
		this.notFoundAction = toOneMapping.getNotFoundAction();
		this.sourceAlias = tableGroup.getSourceAlias();

		this.entityResult = new EntityResultImpl(
				navigablePath,
				toOneMapping,
				tableGroup,
				null
		);

		this.entityResult.afterInitialize( this, creationState );
	}

	public EntityFetchJoinedImpl(
			FetchParent fetchParent,
			EntityCollectionPart collectionPart,
			TableGroup tableGroup,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		super( navigablePath );
		this.fetchContainer = collectionPart;
		this.fetchParent = fetchParent;
		this.notFoundAction = collectionPart.getNotFoundAction();
		this.keyResult = null;
		this.sourceAlias = tableGroup.getSourceAlias();

		this.entityResult = new EntityResultImpl(
				navigablePath,
				collectionPart,
				tableGroup,
				null
		);

		this.entityResult.afterInitialize( this, creationState );
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EntityFetchJoinedImpl(EntityFetchJoinedImpl original) {
		super( original.getNavigablePath() );
		this.fetchContainer = original.fetchContainer;
		this.fetchParent = original.fetchParent;
		this.entityResult = original.entityResult;
		this.keyResult = original.keyResult;
		this.notFoundAction = original.notFoundAction;
		this.sourceAlias = original.sourceAlias;
	}

	@Override
	public EntityValuedFetchable getEntityValuedModelPart() {
		return fetchContainer;
	}

	@Override
	public FetchableContainer getFetchContainer() {
		return fetchContainer;
	}

	@Override
	public EntityValuedFetchable getReferencedModePart() {
		return getEntityValuedModelPart();
	}

	@Override
	public EntityValuedFetchable getReferencedMappingType() {
		return getEntityValuedModelPart();
	}

	@Override
	public EntityMappingType getReferencedMappingContainer() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public EntityValuedFetchable getFetchedMapping() {
		return getEntityValuedModelPart();
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public DomainResultAssembler<?> createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return buildEntityAssembler( creationState.resolveInitializer( this, parentAccess, this ).asEntityInitializer() );
	}

	protected EntityAssembler buildEntityAssembler(EntityInitializer entityInitializer) {
		return new EntityAssembler( getFetchedMapping().getJavaType(), entityInitializer );
	}

	@Override
	public Initializer createInitializer(
			EntityFetchJoinedImpl resultGraphNode,
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parentAccess, creationState );
	}

	@Override
	public EntityInitializer createInitializer(FetchParentAccess parentAccess, AssemblerCreationState creationState) {
		return new EntityJoinedFetchInitializer(
				entityResult,
				getReferencedModePart(),
				getNavigablePath(),
				creationState.determineEffectiveLockMode( sourceAlias ),
				notFoundAction,
				keyResult,
				entityResult.getRowIdResult(),
				entityResult.getIdentifierFetch(),
				entityResult.getDiscriminatorFetch(),
				parentAccess,
				creationState
		);
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public boolean hasTableGroup() {
		return true;
	}

	@Override
	public boolean containsCollectionFetches() {
		return entityResult.containsCollectionFetches();
	}

	public EntityResultImpl getEntityResult() {
		return entityResult;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		entityResult.collectValueIndexesToCache( valueIndexes );
	}

	/*
	 * BEGIN: For Hibernate Reactive
	 */
	protected DomainResult<?> getKeyResult() {
		return keyResult;
	}

	protected NotFoundAction getNotFoundAction() {
		return notFoundAction;
	}

	protected String getSourceAlias() {
		return sourceAlias;
	}
	/*
	 * END: Hibernate Reactive: make sure values are accessible from subclass
	 */
}
