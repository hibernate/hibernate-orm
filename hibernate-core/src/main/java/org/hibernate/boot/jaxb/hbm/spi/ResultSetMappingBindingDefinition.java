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

/**
 * @author Steve Ebersole
 */
public interface ResultSetMappingBindingDefinition {
	/**
	 * The ResultSet mapping name
	 *
	 * @return The name.
	 */
	String getName();

	/**
	 * Get the JAXB mappings for each defined value return in the ResultSet mapping.
	 *
	 * Needs to be a "grab bag", as opposed to individual Lists per return type, in
	 * order to maintain the defined order.  And atm there is (unfortunately) not
	 * a common type for all the JAXB return mappings.  But the list elements
	 * will be one of:<ul>
	 *     <li>{@link JaxbHbmNativeQueryScalarReturnType}</li>
	 *     <li>{@link JaxbHbmNativeQueryReturnType}</li>
	 *     <li>{@link JaxbHbmNativeQueryJoinReturnType}</li>
	 *     <li>{@link JaxbHbmNativeQueryCollectionLoadReturnType}</li>
	 * </ul>
	 *
	 * @return The value return JAXB mappings.
	 */
	List getValueMappingSources();
}
