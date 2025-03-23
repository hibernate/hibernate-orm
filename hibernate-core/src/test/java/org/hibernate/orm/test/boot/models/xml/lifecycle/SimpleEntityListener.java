/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.lifecycle;

/**
 * @author Marco Belladelli
 */
public class SimpleEntityListener {
	public void postPersist(Object entity) {
		// used by hibernate lifecycle callbacks
	}

	public void postPersist() {
		// should not be used by hibernate lifecycle callbacks
	}

	public void postPersist(Object arg1, Object arg2) {
		// should not be used by hibernate lifecycle callbacks
	}

	public void postRemove(Object entity) {
		// used by hibernate lifecycle callbacks
	}

	public void postUpdate(Object entity) {
		// used by hibernate lifecycle callbacks
	}

	public void postLoad(Object entity) {
		// used by hibernate lifecycle callbacks
	}
}
