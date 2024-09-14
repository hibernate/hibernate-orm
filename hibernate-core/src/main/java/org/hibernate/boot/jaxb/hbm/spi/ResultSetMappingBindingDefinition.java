/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * The elements here will all be of type {@link NativeQueryReturn}.  However
	 * the generate JAXB bindings do not understand that as a commonality between all of the
	 * sub-element types.
	 *
	 * @return The value return JAXB mappings.
	 */
	List getValueMappingSources();
}
