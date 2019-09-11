/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;

import org.hibernate.sql.results.spi.EntityResult;
import org.hibernate.sql.results.spi.Initializer;

/**
 * Standard ReturnEntity impl
 *
 * @author Steve Ebersole
 */
public class EntityResultImpl extends AbstractEntityMappingNode implements EntityResult {
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
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer initializerCollector,
			AssemblerCreationState creationState) {
		// todo (6.0) : seems like here is where we ought to determine the SQL selection mappings

		final EntityRootInitializer initializer = new EntityRootInitializer(
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

}
