/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.events;

import jakarta.persistence.PrePersist;

/**
 * @author Steve Ebersole
 */
public class TheListener {
	private final Monitor monitor;

	@jakarta.inject.Inject
	public TheListener(Monitor monitor) {
		this.monitor = monitor;
	}

	@PrePersist
	public void onCreate(Object entity) {
		monitor.entitySaved();
	}
}
