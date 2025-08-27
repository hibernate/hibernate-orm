/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.callback.listener;

import jakarta.persistence.PostLoad;

public class PersonListener {

	@PostLoad
	protected void postLoad(MultiListenersAppliedTest.PersonCallback callback) {
		callback.addPostLoadCall("PersonListener"  );
		callback.setPostPersistCalled();
	}
}
