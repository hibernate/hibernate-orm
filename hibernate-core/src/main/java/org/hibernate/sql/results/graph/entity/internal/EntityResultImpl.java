/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.entity.AbstractEntityResultGraphNode;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Standard ReturnEntity impl
 *
 * @author Steve Ebersole
 */
public class EntityResultImpl<E> extends AbstractEntityResultGraphNode
		implements EntityResult<E>, InitializerProducer<EntityResultImpl<E>> {

	private final TableGroup tableGroup;
	private final String resultVariable;

	public EntityResultImpl(
			NavigablePath navigablePath,
			EntityValuedModelPart entityValuedModelPart,
			TableGroup tableGroup,
			String resultVariable) {
		super( entityValuedModelPart, navigablePath );
		this.tableGroup = tableGroup;
		this.resultVariable = resultVariable;
	}

	@Override
	public NavigablePath resolveNavigablePath(Fetchable fetchable) {
		if ( fetchable instanceof TableGroupProducer ) {
			for ( var tableGroupJoin : tableGroup.getTableGroupJoins() ) {
				final var navigablePath = tableGroupJoin.getNavigablePath();
				if ( tableGroupJoin.getJoinedGroup().isFetched()
						&& fetchable.getFetchableName().equals( navigablePath.getLocalName() )
						&& tableGroupJoin.getJoinedGroup().getModelPart() == fetchable
						&& castNonNull( navigablePath.getParent() ).equals( getNavigablePath() ) ) {
					return navigablePath;
				}
			}
		}
		return super.resolveNavigablePath( fetchable );
	}

	@Override
	public FetchableContainer getReferencedMappingType() {
		return getReferencedMappingContainer();
	}

	@Override
	public EntityValuedModelPart getReferencedModePart() {
		return getEntityValuedModelPart();
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	protected String getSourceAlias() {
		return tableGroup.getSourceAlias();
	}

	@Override
	public JavaType<E> getResultJavaType() {
		return (JavaType<E>) super.getResultJavaType();
	}

	@Override
	public DomainResultAssembler<E> createResultAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return new EntityAssembler<>(
				this.getResultJavaType(),
				creationState.resolveInitializer( this, parent, this ).asEntityInitializer()
		);
	}

	@Override
	public Initializer<?> createInitializer(
			EntityResultImpl<E> resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parent, creationState );
	}

	@Override
	public Initializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new EntityInitializerImpl(
				this,
				getSourceAlias(),
				getIdentifierFetch(),
				getDiscriminatorFetch(),
				null,
				getRowIdResult(),
				NotFoundAction.EXCEPTION,
				false,
				null,
				true,
				creationState
		);
	}

	@Override
	public String toString() {
		return "EntityResultImpl {" + getNavigablePath() + "}";
	}
}
