/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.complete;

import org.hibernate.orm.test.jpa.callbacks.xml.common.CallbackTarget;

/**
 * @author Steve Ebersole
 */
public class ListenerA {
	public static final String NAME = "ListenerA";

	protected void prePersist(CallbackTarget target) {
		target.prePersistCalled( NAME );
	}

	protected void postPersist(CallbackTarget target) {
		target.postPersistCalled( NAME );
	}

	protected void preRemove(CallbackTarget target) {
		target.preRemoveCalled( NAME );
	}

	protected void postRemove(CallbackTarget target) {
		target.postRemoveCalled( NAME );
	}

	protected void preUpdate(CallbackTarget target) {
		target.preUpdateCalled( NAME );
	}

	protected void postUpdate(CallbackTarget target) {
		target.postUpdateCalled( NAME );
	}

	protected void postLoad(CallbackTarget target) {
		target.postLoadCalled( NAME );
	}

}
