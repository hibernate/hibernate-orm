/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.internal;

import org.hibernate.secure.spi.GrantedPermission;
import org.hibernate.secure.spi.JaccService;
import org.hibernate.secure.spi.PermissibleAction;
import org.hibernate.secure.spi.PermissionCheckEntityInformation;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class DisabledJaccServiceImpl implements JaccService {
	private static final Logger log = Logger.getLogger( DisabledJaccServiceImpl.class );

	@Override
	public String getContextId() {
		return null;
	}

	@Override
	public void addPermission(GrantedPermission permissionDeclaration) {
		log.debug( "Ignoring call to addPermission on disabled JACC service" );
	}

	@Override
	public void checkPermission(PermissionCheckEntityInformation entityInformation, PermissibleAction action) {
		log.debug( "Ignoring call to checkPermission on disabled JACC service" );
	}
}
