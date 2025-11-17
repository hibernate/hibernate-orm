/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.replace;

import org.hibernate.orm.test.jpa.callbacks.xml.common.CallbackTarget;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * @author Steve Ebersole
 */
public class ListenerA {
	@PrePersist
	protected void prePersist(CallbackTarget target) {
		target.prePersistCalled( "ListenerA" );
	}

	@PostPersist
	protected void postPersist(CallbackTarget target) {
		target.postPersistCalled( "ListenerA" );
	}

	@PreRemove
	protected void preRemove(CallbackTarget target) {
		target.preRemoveCalled( "ListenerA" );
	}

	@PostRemove
	protected void postRemove(CallbackTarget target) {
		target.postRemoveCalled( "ListenerA" );
	}

	@PreUpdate
	protected void preUpdate(CallbackTarget target) {
		target.preUpdateCalled( "ListenerA" );
	}

	@PostUpdate
	protected void postUpdate(CallbackTarget target) {
		target.postUpdateCalled( "ListenerA" );
	}

	@PostLoad
	protected void postLoad(CallbackTarget target) {
		target.postLoadCalled( "ListenerA" );
	}

}
