/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.EntityEntry;

/**
 * @author Gavin King
 */
public class FlushEntityEvent extends AbstractSessionEvent {

	private Object entity;
	private Object[] propertyValues;
	private Object[] databaseSnapshot;
	private int[] dirtyProperties;
	private boolean hasDirtyCollection;
	private boolean dirtyCheckPossible;
	private boolean dirtyCheckHandledByInterceptor;
	private EntityEntry entityEntry;
	private boolean allowedToReuse;//allows this event instance to be reused for multiple events: special case to GC
	private int instanceGenerationId;//in support of event instance reuse: to double check no recursive/nested use is happening

	public FlushEntityEvent(EventSource source, Object entity, EntityEntry entry) {
		super(source);
		this.entity = entity;
		this.entityEntry = entry;
	}

	public EntityEntry getEntityEntry() {
		return entityEntry;
	}
	public Object[] getDatabaseSnapshot() {
		return databaseSnapshot;
	}
	public void setDatabaseSnapshot(Object[] databaseSnapshot) {
		this.databaseSnapshot = databaseSnapshot;
	}
	public boolean hasDatabaseSnapshot() {
		return databaseSnapshot!=null;
	}
	public boolean isDirtyCheckHandledByInterceptor() {
		return dirtyCheckHandledByInterceptor;
	}
	public void setDirtyCheckHandledByInterceptor(boolean dirtyCheckHandledByInterceptor) {
		this.dirtyCheckHandledByInterceptor = dirtyCheckHandledByInterceptor;
	}
	public boolean isDirtyCheckPossible() {
		return dirtyCheckPossible;
	}
	public void setDirtyCheckPossible(boolean dirtyCheckPossible) {
		this.dirtyCheckPossible = dirtyCheckPossible;
	}
	public int[] getDirtyProperties() {
		return dirtyProperties;
	}
	public void setDirtyProperties(int[] dirtyProperties) {
		this.dirtyProperties = dirtyProperties;
	}
	public boolean hasDirtyProperties() {
		return dirtyProperties != null && dirtyProperties.length != 0;
	}
	public boolean hasDirtyCollection() {
		return hasDirtyCollection;
	}
	public void setHasDirtyCollection(boolean hasDirtyCollection) {
		this.hasDirtyCollection = hasDirtyCollection;
	}
	public Object[] getPropertyValues() {
		return propertyValues;
	}
	public void setPropertyValues(Object[] propertyValues) {
		this.propertyValues = propertyValues;
	}
	public Object getEntity() {
		return entity;
	}

	/**
	 * This is a terrible anti-pattern, but particular circumstances call for being
	 * able to reuse the same event instance: this is otherwise allocated in hot loops
	 * and since each event is escaping the scope it's actually causing allocation issues.
	 * The flush event does not appear to be used recursively so this is currently safe to
	 * do, nevertheless we add an allowedToReuse flag to ensure only instances whose
	 * purpose has completed are being reused.
	 * N.B. two out of three parameters from the constructor are reset: the same EventSource is implied
	 * on reuse.
	 * @param entity same as constructor parameter
	 * @param entry same as constructor parameter
	 */
	public void resetAndReuseEventInstance(Object entity, EntityEntry entry) {
		this.entity = entity;
		this.entityEntry = entry;
		this.allowedToReuse = false;
		//and reset other fields to the default:
		this.propertyValues = null;
		this.databaseSnapshot = null;
		this.dirtyProperties = null;
		this.hasDirtyCollection = false;
		this.dirtyCheckPossible = false;
		this.dirtyCheckHandledByInterceptor = false;
	}

	public boolean isAllowedToReuse() {
		return this.allowedToReuse;
	}

	public void setAllowedToReuse(final boolean allowedToReuse) {
		this.allowedToReuse = allowedToReuse;
	}

	public int getInstanceGenerationId() {
		return this.instanceGenerationId;
	}

	public void setInstanceGenerationId(final int instanceGenerationId) {
		this.instanceGenerationId = instanceGenerationId;
	}
}
