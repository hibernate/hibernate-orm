/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module.subject;

import jakarta.persistence.PrePersist;

public class PackageListener {
	@PrePersist
	public void prePersist(Object entity) {
		EventTracker.events.add( "package:" + entity.getClass().getSimpleName() );
	}
}
