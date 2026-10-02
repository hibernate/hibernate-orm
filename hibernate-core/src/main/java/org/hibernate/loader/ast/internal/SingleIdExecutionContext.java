package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.exec.internal.BaseExecutionContext;

/**
 * @author Steve Ebersole
 */
class SingleIdExecutionContext extends BaseExecutionContext {
	@Nullable
	private final Object entityInstance;
	private final Object entityId;
	private final EntityMappingType rootEntityDescriptor;
	@Nullable
	private final Boolean readOnly;
	private final LockOptions lockOptions;
	private final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;

	public SingleIdExecutionContext(
			@Nonnull Object entityId,
			@Nullable Object entityInstance,
			@Nonnull EntityMappingType rootEntityDescriptor,
			@Nullable Boolean readOnly,
			@Nonnull LockOptions lockOptions,
			@Nonnull SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler,
			@Nonnull SharedSessionContractImplementor session) {
		super( session );
		this.entityInstance = entityInstance;
		this.entityId = entityId;
		this.rootEntityDescriptor = rootEntityDescriptor;
		this.readOnly = readOnly;
		this.lockOptions = lockOptions;
		this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
	}

	@Nullable
	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@Nonnull
	@Override
	public Object getEntityId() {
		return entityId;
	}

	@Nonnull
	@Override
	public EntityMappingType getRootEntityDescriptor() {
		return rootEntityDescriptor;
	}

	@Nonnull
	@Override
	public QueryOptions getQueryOptions() {
		return new QueryOptionsAdapter() {
			@Nullable
			@Override
			public Boolean isReadOnly() {
				return readOnly;
			}

			@Override
			@Nonnull
			public LockOptions getLockOptions() {
				return lockOptions;
			}
		};
	}

	@Override
	public void registerLoadingEntityHolder(@Nonnull EntityHolder holder) {
		subSelectFetchableKeysHandler.addKey( holder );
	}

	@Override
	public boolean upgradeLocks() {
		return true;
	}
}
