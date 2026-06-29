/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import jakarta.persistence.PessimisticLockScope;
import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 *  Defines an event class for the loading of an entity.
 *
 * @author Steve Ebersole
 */
public class LoadEvent extends AbstractSessionEvent {

	private Object entityId;
	private String entityClassName;
	private Object instanceToLoad;
	private LockOptions lockOptions;
	private boolean isAssociationFetch;
	private Object result;
	private Boolean readOnly;

	public LoadEvent(@Nonnull Object entityId, @Nullable Object instanceToLoad, @Nonnull EventSource source, @Nullable Boolean readOnly) {
		this( entityId, null, instanceToLoad, LockMode.NONE.toLockOptions(), false, source, readOnly );
	}

	public LoadEvent(@Nonnull Object entityId, @Nullable String entityClassName, @Nonnull LockMode lockMode, @Nonnull EventSource source, @Nullable Boolean readOnly) {
		this( entityId, entityClassName, null, lockMode, false, source, readOnly );
	}

	public LoadEvent(@Nonnull Object entityId, @Nullable String entityClassName, @Nonnull LockOptions lockOptions, @Nonnull EventSource source, @Nullable Boolean readOnly) {
		this( entityId, entityClassName, null, lockOptions, false, source, readOnly );
	}

	public LoadEvent(@Nonnull Object entityId, @Nullable String entityClassName, boolean isAssociationFetch, @Nonnull EventSource source, @Nullable Boolean readOnly) {
		this( entityId, entityClassName, null, LockOptions.NONE, isAssociationFetch, source, readOnly );
	}

	private LoadEvent(
			@Nonnull Object entityId,
			@Nullable String entityClassName,
			@Nullable Object instanceToLoad,
			@Nonnull LockMode lockMode,
			boolean isAssociationFetch,
			@Nonnull EventSource source,
			@Nullable Boolean readOnly) {
		this(
				entityId,
				entityClassName,
				instanceToLoad,
				lockMode.toLockOptions(),
				isAssociationFetch,
				source,
				readOnly
		);
	}

	private LoadEvent(
			@Nonnull Object entityId,
			@Nullable String entityClassName,
			@Nullable Object instanceToLoad,
			@Nonnull LockOptions lockOptions,
			boolean isAssociationFetch,
			@Nonnull EventSource source,
			@Nullable Boolean readOnly) {
		super( source );
		this.entityId = entityId;
		this.entityClassName = entityClassName;
		this.instanceToLoad = instanceToLoad;
		this.lockOptions = lockOptions;
		this.isAssociationFetch = isAssociationFetch;
		this.readOnly = readOnly;
		validate();
	}

	@Internal
	public void validate() {
		if ( entityId == null ) {
			throw new IllegalArgumentException( "Identifier may not be null" );
		}

		final var lockMode = lockOptions.getLockMode();
		if ( lockMode == LockMode.WRITE ) {
			throw new IllegalArgumentException( "Invalid lock mode: " + LockMode.WRITE );
		}
		else if ( lockMode == null ) {
			lockOptions.setLockMode( LockMode.NONE );
		}
	}

	@Nonnull
	public Object getEntityId() {
		return entityId;
	}

	@Internal
	public void setEntityId(@Nonnull Object entityId) {
		this.entityId = entityId;
	}

	@Nullable
	public String getEntityClassName() {
		return entityClassName;
	}

	@Internal
	public void setEntityClassName(@Nullable String entityClassName) {
		this.entityClassName = entityClassName;
	}

	public boolean isAssociationFetch() {
		return isAssociationFetch;
	}

	@Internal
	public void setAssociationFetch(boolean associationFetch) {
		isAssociationFetch = associationFetch;
	}

	@Nullable
	public Object getInstanceToLoad() {
		return instanceToLoad;
	}

	@Internal
	public void setInstanceToLoad(@Nullable Object instanceToLoad) {
		this.instanceToLoad = instanceToLoad;
	}

	@Nonnull
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Internal
	public void setLockOptions(@Nonnull LockOptions lockOptions) {
		this.lockOptions = lockOptions;
	}

	@Nullable
	public Object getResult() {
		return result;
	}

	public void setResult(@Nullable Object result) {
		this.result = result;
	}

	public @Nullable Boolean getReadOnly() {
		return readOnly;
	}

	@Internal
	public void setReadOnly(@Nullable Boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * @deprecated Use {@linkplain #getLockOptions()} instead.
	 */
	@Deprecated(since = "7.1")
	public @Nonnull LockMode getLockMode() {
		return lockOptions.getLockMode();
	}

	/**
	 * @deprecated Use {@linkplain #getLockOptions()} instead.
	 */
	@Deprecated(since = "7.1")
	public int getLockTimeout() {
		return lockOptions.getTimeout().milliseconds();
	}

	/**
	 * @deprecated Use {@linkplain #getLockOptions()} instead.
	 */
	@Deprecated(since = "7.1")
	public boolean getLockScope() {
		return lockOptions.getLockScope() != PessimisticLockScope.NORMAL;
	}
}
