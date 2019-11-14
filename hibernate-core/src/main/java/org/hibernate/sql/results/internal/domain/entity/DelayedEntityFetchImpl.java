/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class DelayedEntityFetchImpl extends AbstractEntityFecth {

	private DomainResult fkResult;

	public DelayedEntityFetchImpl(
			FetchParent fetchParent,
			SingularAssociationAttributeMapping fetchedAttribute,
			LockMode lockMode,
			boolean nullable,
			NavigablePath navigablePath,
			DomainResult fkResult,
			DomainResultCreationState creationState) {
		super( fetchParent, fetchedAttribute, navigablePath, nullable, lockMode );
		this.fkResult = fkResult;
	}

	@Override
	protected EntityInitializer getEntityInitializer(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		final SingularAssociationAttributeMapping fetchedAttribute = (SingularAssociationAttributeMapping) getFetchedMapping();
		return new DelayedEntityFetchInitializer(
				parentAccess,
				getNavigablePath(),
				fetchedAttribute.getMappedFetchStrategy(),
				getLockMode(),
				(EntityPersister) fetchedAttribute.getMappedTypeDescriptor(),
				fkResult.createResultAssembler( collector, creationState )
		);
	}
}
