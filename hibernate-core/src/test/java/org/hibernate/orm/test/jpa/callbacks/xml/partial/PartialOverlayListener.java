/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.partial;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.PrePersist;

public class PartialOverlayListener {

	private static final AtomicInteger PRE_PERSIST_COUNT = new AtomicInteger();

	public static void reset() {
		PRE_PERSIST_COUNT.set( 0 );
	}

	public static int getPrePersistCount() {
		return PRE_PERSIST_COUNT.get();
	}

	@PrePersist
	void onPrePersist(PartialOverlayParent entity) {
		PRE_PERSIST_COUNT.incrementAndGet();
	}
}
