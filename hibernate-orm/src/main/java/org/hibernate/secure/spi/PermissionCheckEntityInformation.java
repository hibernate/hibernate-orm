/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.secure.spi;

import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public interface PermissionCheckEntityInformation {
	public Object getEntity();
	public String getEntityName();
	public Serializable getIdentifier();
}
