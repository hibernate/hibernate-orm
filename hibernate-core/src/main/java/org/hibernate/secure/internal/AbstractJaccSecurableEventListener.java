/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
