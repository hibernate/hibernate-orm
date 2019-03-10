/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
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
			EntityValuedNavigable referencedEntity,
			String resultVariable,
			DomainResultCreationState creationState) {
		super(
				referencedEntity,
				creationState.determineLockMode( resultVariable ),
				navigablePath,
				creationState
		);

		this.resultVariable = resultVariable;

		afterInitialize( creationState );
	}

	public EntityValuedNavigable getNavigable() {
		return getEntityValuedNavigable();
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {

		// todo (6.0) : seems like here is where we ought to determine the SQL selection mappings

		final EntityRootInitializer initializer = new EntityRootInitializer(
				this,
				getNavigablePath(),
				getLockMode(),
				getIdentifierResult(),
				getDiscriminatorResult(),
				getVersionResult(),
				collector,
				creationState
		);

		return new EntityAssembler(
				getNavigable().getJavaTypeDescriptor(),
				initializer
		);
	}
}
