/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.spi.db;

/**
 * Base definition for XSD column mappings
 *
 * @author Steve Ebersole
 */
public interface JaxbColumn extends JaxbDatabaseObject {
	String getName();

	default String getTable() {
		return null;
	}
}
