/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4.packagelevel;

import jakarta.persistence.PrePersist;

public class PackageLevelPackageListener {
	public PackageLevelPackageListener() {
	}

	@PrePersist
	void prePersist(Object entity) {
		PackageLevelEntityListenerTests.Events.names.add( "package:" + entity.getClass().getSimpleName() );
	}
}
