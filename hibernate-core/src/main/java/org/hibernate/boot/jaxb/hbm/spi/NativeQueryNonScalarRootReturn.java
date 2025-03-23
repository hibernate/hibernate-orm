/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
public interface NativeQueryNonScalarRootReturn extends NativeQueryReturn {
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
