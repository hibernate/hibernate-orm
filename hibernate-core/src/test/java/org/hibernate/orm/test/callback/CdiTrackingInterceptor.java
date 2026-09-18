/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.callback;

import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

public class CdiTrackingInterceptor implements Interceptor {
	static final CopyOnWriteArrayList<CdiTrackingInterceptor> INSTANCES = new CopyOnWriteArrayList<>();

	private final InterceptorMonitor monitor;
	private boolean postConstructCalled;
	private int persistCount;

	@Inject
	public CdiTrackingInterceptor(InterceptorMonitor monitor) {
		this.monitor = monitor;
		INSTANCES.add( this );
	}

	@PostConstruct
	void onPostConstruct() {
		postConstructCalled = true;
	}

	@Override
	public boolean onPersist(Object entity, Object id, Object[] state,
							String[] propertyNames, Type[] types) {
		persistCount++;
		monitor.entityPersisted();
		return false;
	}

	public boolean isPostConstructCalled() {
		return postConstructCalled;
	}

	public boolean isPersistCalled() {
		return persistCount > 0;
	}

	static void clearInstances() {
		INSTANCES.clear();
	}
}
