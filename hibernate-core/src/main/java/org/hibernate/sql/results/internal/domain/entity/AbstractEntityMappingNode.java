/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.internal.domain.AbstractFetchParent;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityMappingNode;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityMappingNode extends AbstractFetchParent implements EntityMappingNode {
	private final EntityValuedNavigable entityValuedNavigable;
	private final DomainResult identifierResult;
	private final DomainResult versionResult;
	private final LockMode lockMode;

	public AbstractEntityMappingNode(
			EntityValuedNavigable entityValuedNavigable,
			LockMode lockMode,
			NavigablePath navigablePath,
			DomainResultCreationContext creationContext,
			DomainResultCreationState creationState) {
		super( entityValuedNavigable, navigablePath );
		this.entityValuedNavigable = entityValuedNavigable;
		this.lockMode = lockMode;

		final EntityTypeDescriptor entityDescriptor = entityValuedNavigable.getEntityDescriptor();

		identifierResult = entityDescriptor.getIdentifierDescriptor().createDomainResult(
				null,
				creationState,
				creationContext
		);

		final VersionDescriptor<Object, Object> versionDescriptor = entityDescriptor.getHierarchy().getVersionDescriptor();
		if ( versionDescriptor == null ) {
			versionResult = null;
		}
		else {
			versionResult = versionDescriptor.createDomainResult(
					null,
					creationState, creationContext
			);
		}

		// todo (6.0) : handle other special navigables such as discriminator, row-id, tenant-id, etc
	}

	@Override
	public EntityValuedNavigable getEntityValuedNavigable() {
		return entityValuedNavigable;
	}

	@Override
	public EntityJavaDescriptor getJavaTypeDescriptor() {
		return getEntityValuedNavigable().getJavaTypeDescriptor();
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	protected DomainResult getIdentifierResult() {
		return identifierResult;
	}

	protected DomainResult getDiscriminatorResult() {
		return null;
	}

	protected DomainResult getVersionResult() {
		return versionResult;
	}
}
