/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
