/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.core;

import org.hibernate.event.internal.PostInsertEventListenerStandardImpl;
import org.hibernate.jpa.event.spi.jpa.CallbackRegistry;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Steve Ebersole
 *
 * @deprecated No longer used.  Handling variance has been incorporated directly into PostInsertEventListenerStandardImpl
 */
@Deprecated
public class JpaPostInsertEventListener extends PostInsertEventListenerStandardImpl {
	public JpaPostInsertEventListener() {
	}

	public JpaPostInsertEventListener(CallbackRegistry callbackRegistry) {
		injectCallbackRegistry( callbackRegistry );
	}
}
