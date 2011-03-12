/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.event;

import org.hibernate.engine.EntityEntry;

/**
 * @author Gavin King
 */
public class FlushEntityEvent extends AbstractEvent {
	
	private Object entity;
	private Object[] propertyValues;
	private Object[] databaseSnapshot;
	private int[] dirtyProperties;
	private boolean hasDirtyCollection;
	private boolean dirtyCheckPossible;
	private boolean dirtyCheckHandledByInterceptor;
	private EntityEntry entityEntry;
	
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
}
