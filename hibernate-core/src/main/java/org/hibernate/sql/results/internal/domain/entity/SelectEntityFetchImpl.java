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
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Andrea Boriero
 */
public class SelectEntityFetchImpl extends AbstractEntityFecth {

	private final DomainResult result;

	public SelectEntityFetchImpl(
			FetchParent fetchParent,
			SingularAssociationAttributeMapping fetchedAttribute,
			LockMode lockMode,
			NavigablePath navigablePath,
			DomainResult result) {
		super( fetchParent, fetchedAttribute, navigablePath, fetchedAttribute.isNullable(), lockMode );
		this.result = result;
	}


	@Override
	protected EntityInitializer getEntityInitializer(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		final SingularAssociationAttributeMapping fetchedAttribute = (SingularAssociationAttributeMapping) getFetchedMapping();

		return new SelectEntityInitializer(
				getNavigablePath(),
				(EntityPersister) fetchedAttribute.getMappedTypeDescriptor(),
				result.createResultAssembler( collector, creationState ),
				fetchedAttribute.isUnwrapProxy(),
				fetchedAttribute.isNullable()
		);
	}


}
