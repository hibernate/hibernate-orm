/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.internal;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.AbstractPreDatabaseOperationEvent;
import org.hibernate.secure.spi.JaccService;
import org.hibernate.secure.spi.PermissibleAction;
import org.hibernate.secure.spi.PermissionCheckEntityInformation;

/**
 * Base class for JACC-securable event listeners
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJaccSecurableEventListener implements JaccSecurityListener {
	private JaccService jaccService;

	protected void performSecurityCheck(AbstractPreDatabaseOperationEvent event, PermissibleAction action) {
		performSecurityCheck( event.getSession(), event, action );
	}

	protected void performSecurityCheck(
			SessionImplementor session,
			PermissionCheckEntityInformation entityInformation,
			PermissibleAction action) {
		if ( jaccService == null ) {
			jaccService = session.getFactory().getServiceRegistry().getService( JaccService.class );
		}
		jaccService.checkPermission( entityInformation, action );
	}
}
