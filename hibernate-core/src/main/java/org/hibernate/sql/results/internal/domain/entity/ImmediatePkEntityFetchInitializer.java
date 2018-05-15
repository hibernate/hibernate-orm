/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.SingleEntityLoader;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.LoadingEntityEntry;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * Subsequent select initializer.  See {@link ImmediatePkEntityFetch}
 *
 * @author Steve Ebersole
 */
public class ImmediatePkEntityFetchInitializer extends AbstractImmediateEntityFetchInitializer {
	private EntityKey entityKey;

	private boolean isLoadingEntity;

	public ImmediatePkEntityFetchInitializer(
			EntityValuedNavigable fetchedNavigable,
			NavigablePath navigablePath,
			SingleEntityLoader loader,
			FetchParentAccess parentAccess,
			DomainResultAssembler keyValueAssembler,
			NotFoundAction notFoundAction) {
		super( fetchedNavigable, navigablePath, loader, parentAccess, keyValueAssembler, notFoundAction );
	}

	@Override
	protected KeyType keyType() {
		return KeyType.PK;
	}


	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( entityKey != null ) {
			return;
		}

		final Object keyValue = getKeyValue();

		if ( keyValue == null ) {
			return;
		}

		entityKey = new EntityKey( keyValue, getEntityDescriptor() );

		final SharedSessionContractImplementor session = rowProcessingState.getSession();

		final LoadingEntityEntry existingEntry = session.getPersistenceContext()
				.getLoadContexts()
				.findLoadingEntityEntry( entityKey );

		if ( existingEntry != null ) {
			setEntityInstance( existingEntry.getEntityInstance() );
			isLoadingEntity = false;
			return;
		}

		final Object managed = session.getPersistenceContext().getEntity( entityKey );
		if ( managed != null ) {
			setEntityInstance( managed );
			isLoadingEntity = false;
			return;
		}

		// todo (6.0) : second-level cache

		final Object entityInstance = getEntityDescriptor().instantiate( keyValue, session );
		final LoadingEntityEntry entityEntry = new LoadingEntityEntry(
				this,
				entityKey,
				getEntityDescriptor(),
				entityInstance
		);

		rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingEntity( entityKey, entityEntry );
		isLoadingEntity = true;
	}


	@Override
	protected boolean isLoadingEntityInstance() {
		return isLoadingEntity;
	}

	@Override
	protected void afterLoad(Object entityInstance, RowProcessingState rowProcessingState) {
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		super.finishUpRow( rowProcessingState );

		entityKey = null;
		isLoadingEntity = false;
	}
}
