/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.AbstractEntityResultGraphNode;
import org.hibernate.sql.results.graph.entity.EntityResult;

/**
 * Standard ReturnEntity impl
 *
 * @author Steve Ebersole
 */
public class EntityResultImpl extends AbstractEntityResultGraphNode implements EntityResult {

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
		if ( fetchable instanceof TableGroupProducer && getNavigablePath().isAliased() ) {
			for ( TableGroupJoin tableGroupJoin : tableGroup.getTableGroupJoins() ) {
				final NavigablePath navigablePath = tableGroupJoin.getNavigablePath();
				if ( tableGroupJoin.getJoinedGroup().isFetched()
						&& fetchable.getNavigableRole().getLocalName().equals( navigablePath.getLocalName() )
						&& tableGroupJoin.getJoinedGroup().getModelPart() == fetchable ) {
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

	protected LockMode getLockMode(AssemblerCreationState creationState) {
		return creationState.determineEffectiveLockMode( tableGroup.getSourceAlias() );
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final Initializer initializer = creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> new EntityResultInitializer(
						this,
						getNavigablePath(),
						getLockMode( creationState ),
						getIdentifierFetch(),
						getDiscriminatorFetch(),
						getRowIdResult(),
						creationState
				)
		);

		return new EntityAssembler( this.getResultJavaType(), initializer.asEntityInitializer() );
	}

	@Override
	public String toString() {
		return "EntityResultImpl {" + getNavigablePath() + "}";
	}
}
