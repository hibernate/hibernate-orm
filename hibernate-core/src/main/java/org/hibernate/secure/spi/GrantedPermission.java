/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.spi;

/**
 * Describes a Hibernate (persistence) permission.
 *
 * @author Steve Ebersole
 */
public class GrantedPermission {
	private final String role;
	private final String entityName;
	private final PermissibleAction action;

	public GrantedPermission(String role, String entityName, String action) {
		this.role = role;
		this.entityName = entityName;
		this.action = PermissibleAction.interpret( action );
	}

	public String getRole() {
		return role;
	}

	public String getEntityName() {
		return entityName;
	}

	public PermissibleAction getPermissibleAction() {
		return action;
	}
}
