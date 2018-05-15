/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.BiFunction;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.SingleEntityLoader;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * Subsequent select initializer.  See {@link ImmediateUkEntityFetch}
 *
 * @author Steve Ebersole
 */
public class ImmediateUkEntityFetchInitializer extends AbstractImmediateEntityFetchInitializer {
	private final BiFunction<Object, SharedSessionContractImplementor, EntityUniqueKey> uniqueKeyGenerator;

	private EntityUniqueKey entityKey;

	private boolean isLoadingEntity;

	public ImmediateUkEntityFetchInitializer(
			EntityValuedNavigable fetchedNavigable,
			NavigablePath navigablePath,
			SingleEntityLoader loader,
			FetchParentAccess parentAccess,
			DomainResultAssembler keyValueAssembler,
			NotFoundAction notFoundAction,
			BiFunction<Object, SharedSessionContractImplementor,EntityUniqueKey> uniqueKeyGenerator) {
		super( fetchedNavigable, navigablePath, loader, parentAccess, keyValueAssembler, notFoundAction );
		this.uniqueKeyGenerator = uniqueKeyGenerator;
	}

	@Override
	protected KeyType keyType() {
		return KeyType.UK;
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

		entityKey = uniqueKeyGenerator.apply(
				keyValue,
				rowProcessingState.getSession()
		);

		final SharedSessionContractImplementor session = rowProcessingState.getSession();

		// todo (6.0) : add support for LoadingEntityEntry by UK?
//		final LoadingEntityEntry existingEntry = session.getPersistenceContext()
//				.getLoadContexts()
//				.findLoadingEntityEntry( entityKey );
//
//		if ( existingEntry != null ) {
//			entityInstance = existingEntry.getEntityInstance();
//			isLoadingEntity = false;
//			return;
//		}

		final Object managed = session.getPersistenceContext().getEntity( entityKey );
		if ( managed != null ) {
			setEntityInstance( managed );
			isLoadingEntity = false;
			return;
		}

		// todo (6.0) : second-level cache

//		setEntityInstance( getEntityDescriptor().instantiate( keyValue, session ) );

//		final LoadingEntityEntry entityEntry = new LoadingEntityEntry(
//				entityKey,
//				getEntityDescriptor(),
//				entityInstance,
//				// todo (6.0) : rowId
//				null
//		);
//
//		rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingEntity( entityKey, entityEntry );
		isLoadingEntity = true;

	}

	@Override
	protected boolean isLoadingEntityInstance() {
		return isLoadingEntity;
	}

	@Override
	protected void afterLoad(Object entityInstance, RowProcessingState rowProcessingState) {
		if ( entityInstance != null ) {
			rowProcessingState.getSession().getPersistenceContext().addEntity( entityKey, entityInstance );
		}
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		super.finishUpRow( rowProcessingState );

		entityKey = null;
		isLoadingEntity = false;
	}

	@Override
	public String toString() {
		return "ImmediateUkEntityFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
