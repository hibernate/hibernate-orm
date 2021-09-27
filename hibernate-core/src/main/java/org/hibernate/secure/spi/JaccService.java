/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.spi;

import org.hibernate.service.Service;

/**
 * Service describing Hibernate integration with JACC for security service.
 *
 * @author Steve Ebersole
 *
 * @deprecated Support for JACC will be removed in 6.0
 */
@Deprecated
public interface JaccService extends Service {
	/**
	 * Obtain the JACC context-id in effect for this service.  {@code null} indicates no
	 * context is in effect (service is disabled).
	 *
	 * @return The effective JACC context-id
	 */
	public String getContextId();

	public void addPermission(GrantedPermission permissionDeclaration);
	public void checkPermission(PermissionCheckEntityInformation entityInformation, PermissibleAction action);
}
