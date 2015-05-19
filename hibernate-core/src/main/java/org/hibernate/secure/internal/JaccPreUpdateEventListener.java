/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.internal;

import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.secure.spi.PermissibleAction;

/**
 * Check security before any update
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Steve Ebersole
 */
public class JaccPreUpdateEventListener extends AbstractJaccSecurableEventListener implements PreUpdateEventListener {
	public JaccPreUpdateEventListener() {
	}

	public boolean onPreUpdate(PreUpdateEvent event) {
		performSecurityCheck( event, PermissibleAction.UPDATE );
		return false;
	}
}
