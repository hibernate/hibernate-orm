/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.type.Type;

/**
 * An interceptor that does nothing.  May be used as a base class for application-defined custom interceptors.
 * 
 * @author Gavin King
 */
public class EmptyInterceptor implements Interceptor, Serializable {
	/**
	 * The singleton reference.
	 */
	public static final Interceptor INSTANCE = new EmptyInterceptor();

	protected EmptyInterceptor() {
	}

	@Override
	public void onDelete(
			Object entity, 
			Serializable id, 
			Object[] state, 
			String[] propertyNames, 
			Type[] types) {}

	@Override
	public boolean onFlushDirty(
			Object entity, 
			Serializable id, 
			Object[] currentState, 
			Object[] previousState, 
			String[] propertyNames, 
			Type[] types) {
		return false;
	}

	@Override
	public boolean onLoad(
			Object entity, 
			Serializable id, 
			Object[] state, 
			String[] propertyNames, 
			Type[] types) {
		return false;
	}

	@Override
	public boolean onSave(
			Object entity, 
			Serializable id, 
			Object[] state, 
			String[] propertyNames, 
			Type[] types) {
		return false;
	}

	@Override
	public void postFlush(Iterator entities) {
	}

	@Override
	public void preFlush(Iterator entities) {
	}

	@Override
	public Boolean isTransient(Object entity) {
		return null;
	}

	@Override
	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) {
		return null;
	}

	@Override
	public int[] findDirty(
			Object entity,
			Serializable id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) {
		return null;
	}

	@Override
	public String getEntityName(Object object) {
		return null;
	}

	@Override
	public Object getEntity(String entityName, Serializable id) {
		return null;
	}

	@Override
	public void afterTransactionBegin(Transaction tx) {
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
	}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
	}

	@Override
	public String onPrepareStatement(String sql) {
		return sql;
	}

	@Override
	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
	}

	@Override
	public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
	}

	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
	}
	
}
