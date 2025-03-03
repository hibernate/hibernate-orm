/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.bytecode.enhance.internal.tracker.DirtyTracker;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;

/**
 * A self dirtiness tracker that declares additional methods that are intended for
 * internal communication. This interface can be implemented optionally instead of
 * the plain {@link SelfDirtinessTracker}.
 */
public interface ExtendedSelfDirtinessTracker extends SelfDirtinessTracker {

	String REMOVE_DIRTY_FIELDS_NAME = "$$_hibernate_removeDirtyFields";

	void $$_hibernate_getCollectionFieldDirtyNames(DirtyTracker dirtyTracker);

	boolean $$_hibernate_areCollectionFieldsDirty();

	void $$_hibernate_clearDirtyCollectionNames();

	void $$_hibernate_removeDirtyFields(LazyAttributeLoadingInterceptor lazyInterceptor);
}
