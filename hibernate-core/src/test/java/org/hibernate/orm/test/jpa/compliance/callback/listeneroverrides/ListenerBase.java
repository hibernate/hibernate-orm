/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.jpa.compliance.callback.listeneroverrides;

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
public abstract class ListenerBase {
	protected abstract String getListenerName();

	@PrePersist
	protected void prePersist(CallbackTarget target) {
		target.prePersistCalled( getListenerName() );
	}

	@PostPersist
	protected void postPersist(CallbackTarget target) {
		target.postPersistCalled( getListenerName() );
	}

	@PreRemove
	protected void preRemove(CallbackTarget target) {
		target.preRemoveCalled( getListenerName() );
	}

	@PostRemove
	protected void postRemove(CallbackTarget target) {
		target.postRemoveCalled( getListenerName() );
	}

	@PreUpdate
	protected void preUpdate(CallbackTarget target) {
		target.preUpdateCalled( getListenerName() );
	}

	@PostUpdate
	protected void postUpdate(CallbackTarget target) {
		target.postUpdateCalled( getListenerName() );
	}

	@PostLoad
	protected void postLoad(CallbackTarget target) {
		target.postLoadCalled( getListenerName() );
	}
}
