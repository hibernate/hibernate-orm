/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
public class ListenerC {

	@PrePersist
	protected void prePersist(CallbackTarget target) {
		target.prePersistCalled( "ListenerC" );
	}

	@PostPersist
	protected void postPersist(CallbackTarget target) {
		target.postPersistCalled( "ListenerC" );
	}

	@PreRemove
	protected void preRemove(CallbackTarget target) {
		target.preRemoveCalled( "ListenerC" );
	}

	@PostRemove
	protected void postRemove(CallbackTarget target) {
		target.postRemoveCalled( "ListenerC" );
	}

	@PreUpdate
	protected void preUpdate(CallbackTarget target) {
		target.preUpdateCalled( "ListenerC" );
	}

	@PostUpdate
	protected void postUpdate(CallbackTarget target) {
		target.postUpdateCalled( "ListenerC" );
	}

	@PostLoad
	protected void postLoad(CallbackTarget target) {
		target.postLoadCalled( "ListenerC" );
	}
}
