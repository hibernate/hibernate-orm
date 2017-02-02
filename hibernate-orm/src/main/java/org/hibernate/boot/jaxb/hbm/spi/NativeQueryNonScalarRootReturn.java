/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.spi;

import java.util.List;

import org.hibernate.LockMode;

/**
 * Commonality for non-scalar root returns for a native query result mapping
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface NativeQueryNonScalarRootReturn {
	/**
	 * Access the alias associated with this return
	 *
	 * @return The alias
	 */
	String getAlias();

	/**
	 * Access the LockMode associated with this return
	 *
	 * @return The LockMode
	 */
	LockMode getLockMode();

	/**
	 * Access the nested property mappings associated with this return
	 *
	 * @return The nested property mappings
	 */
	List<JaxbHbmNativeQueryPropertyReturnType> getReturnProperty();
}
