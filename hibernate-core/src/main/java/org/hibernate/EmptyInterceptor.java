/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.util.Iterator;

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
	public String getEntityName(Object object) {
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
	public void onCollectionUpdate(Object collection, Object key) throws CallbackException {
	}
}
