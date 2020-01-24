/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.entity.AbstractEntityResultGraphNode;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * Standard ReturnEntity impl
 *
 * @author Steve Ebersole
 */
public class EntityResultImpl extends AbstractEntityResultGraphNode implements EntityResult {
	private final String resultVariable;

	public EntityResultImpl(
			NavigablePath navigablePath,
			EntityValuedModelPart entityValuedModelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		this( navigablePath, entityValuedModelPart, resultVariable, null, creationState );
	}

	public EntityResultImpl(
			NavigablePath navigablePath,
			EntityValuedModelPart entityValuedModelPart,
			String resultVariable,
			EntityMappingType targetType,
			DomainResultCreationState creationState) {
		super(
				entityValuedModelPart,
				creationState.getSqlAstCreationState().determineLockMode( resultVariable ),
				navigablePath,
				creationState
		);

		this.resultVariable = resultVariable;

		afterInitialize( creationState );
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

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer initializerCollector,
			AssemblerCreationState creationState) {
		// todo (6.0) : seems like here is where we ought to determine the SQL selection mappings

		final EntityResultInitializer initializer = new EntityResultInitializer(
				this,
				getNavigablePath(),
				getLockMode(),
				getIdentifierResult(),
				getDiscriminatorResult(),
				getVersionResult(),
				initializerCollector,
				creationState
		);

		return new EntityAssembler( getResultJavaTypeDescriptor(), initializer );
	}

	@Override
	public String toString() {
		return "EntityResultImpl {" + getNavigablePath() + "}";
	}
}
