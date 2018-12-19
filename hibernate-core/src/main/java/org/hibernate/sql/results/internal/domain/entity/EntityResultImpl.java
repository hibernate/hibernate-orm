/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
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
	private final EntityValuedNavigableReference entityReference;


	public EntityResultImpl(
			EntityValuedNavigableReference entityReference,
			String resultVariable,
			DomainResultCreationContext creationContext,
			DomainResultCreationState creationState) {
		super(
				entityReference.getNavigable(),
				entityReference.getLockMode(),
				entityReference.getNavigablePath(),
				creationContext,
				creationState
		);

		this.resultVariable = resultVariable;
		this.entityReference = entityReference;

		afterInitialize( creationState );
	}

	public EntityValuedNavigable getNavigable() {
		return getEntityValuedNavigable();
	}

	public EntityValuedNavigableReference getEntityReference() {
		return entityReference;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer<Initializer> collector,
			AssemblerCreationState creationOptions,
			AssemblerCreationContext creationContext) {

		// todo (6.0) : seems like here is where we ought to determine the SQL selection mappings

		final EntityRootInitializer initializer = new EntityRootInitializer(
				this,
				getNavigablePath(),
				getLockMode(),
				getIdentifierResult(),
				getDiscriminatorResult(),
				getVersionResult(),
				collector,
				creationContext,
				creationOptions
		);

		return new EntityAssembler(
				getNavigable().getJavaTypeDescriptor(),
				initializer
		);
	}
}
