/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

/**
* @author Steve Ebersole
*/
public class JaccPermissionDefinition {
	public final String contextId;
	public final String role;
	public final String clazz;
	public final String actions;

	public JaccPermissionDefinition(String contextId, String role, String clazz, String actions) {
		this.contextId = contextId;
		this.role = role;
		this.clazz = clazz;
		this.actions = actions;
	}
}
