/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks.xml.complete;

import org.hibernate.orm.test.jpa.callbacks.xml.common.CallbackTarget;

/**
 * @author Steve Ebersole
 */
public class ListenerC {
	public static final String NAME = "ListenerC";

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
