/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityFetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;

/**
 * EntityFetch implementation used for handling cases where the entity values
 * are expected to be available in the JdbcValues either as a root selection
 * or as a join fetch.
 *
 * @author Steve Ebersole
 */
public class EntityFetchImpl extends AbstractEntityMappingNode implements EntityFetch {
	private final FetchParent fetchParent;

	public EntityFetchImpl(
			FetchParent fetchParent,
			EntityValuedNavigable fetchedNavigable,
			LockMode lockMode,
			NavigablePath navigablePath,
			DomainResultCreationContext creationContext,
			DomainResultCreationState creationState) {
		super( fetchedNavigable, lockMode, navigablePath, creationContext, creationState );

		this.fetchParent = fetchParent;

		afterInitialize( creationState );
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public EntityValuedNavigable getFetchedNavigable() {
		return getEntityValuedNavigable();
	}

	@Override
	public boolean isNullable() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationContext creationContext,
			AssemblerCreationState creationState) {
		final EntityFetchInitializer initializer = new EntityFetchInitializer(
				parentAccess,
				this,
				getNavigablePath(),
				getLockMode(),
				getIdentifierResult(),
				getDiscriminatorResult(),
				getVersionResult(),
				collector,
				creationContext,
				creationState
		);

		return new EntityAssembler( getFetchedNavigable().getJavaTypeDescriptor(), initializer );
	}
}
