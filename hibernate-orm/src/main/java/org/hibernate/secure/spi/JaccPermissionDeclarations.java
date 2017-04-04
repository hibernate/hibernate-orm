/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class JaccPermissionDeclarations {
	private final String contextId;
	private List<GrantedPermission> permissionDeclarations;

	public JaccPermissionDeclarations(String contextId) {
		this.contextId = contextId;
	}

	public String getContextId() {
		return contextId;
	}

	public void addPermissionDeclaration(GrantedPermission permissionDeclaration) {
		if ( permissionDeclarations == null ) {
			permissionDeclarations = new ArrayList<GrantedPermission>();
		}
		permissionDeclarations.add( permissionDeclaration );
	}

	public void addPermissionDeclarations(Collection<GrantedPermission> permissionDeclarations) {
		if ( this.permissionDeclarations == null ) {
			this.permissionDeclarations = new ArrayList<GrantedPermission>();
		}
		this.permissionDeclarations.addAll( permissionDeclarations );
	}

	public Collection<GrantedPermission> getPermissionDeclarations() {
		return permissionDeclarations;
	}
}
