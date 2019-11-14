/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityFetch extends AbstractEntityFecth {

	private final EntityResultImpl entityResult;

	public EntityFetch(
			FetchParent fetchParent,
			SingularAssociationAttributeMapping fetchedAttribute,
			LockMode lockMode,
			boolean nullable,
			NavigablePath navigablePath,
			DomainResultCreationState creationState) {
		super( fetchParent, fetchedAttribute, navigablePath, nullable, lockMode );

		entityResult = new EntityResultImpl(
				navigablePath,
				(EntityValuedModelPart) fetchedAttribute.getMappedTypeDescriptor(),
				null,
				creationState
		);
	}

	@Override
	protected EntityInitializer getEntityInitializer(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		return new EntityFetchInitializer(
				entityResult,
				getNavigablePath(),
				getLockMode(),
				entityResult.getIdentifierResult(),
				entityResult.getDiscriminatorResult(),
				entityResult.getVersionResult(),
				collector,
				creationState
		);
	}
}
