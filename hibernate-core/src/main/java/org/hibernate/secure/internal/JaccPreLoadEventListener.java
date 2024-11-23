/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.internal;

import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.secure.spi.PermissibleAction;

/**
 * Check security before any load
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Steve Ebersole
 *
 * @deprecated Support for JACC will be removed in 6.0
 */
@Deprecated
public class JaccPreLoadEventListener extends AbstractJaccSecurableEventListener implements PreLoadEventListener {
	public JaccPreLoadEventListener() {
	}

	public void onPreLoad(PreLoadEvent event) {
		performSecurityCheck( event.getSession(), event, PermissibleAction.READ );
	}
}
