/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.spi;

/**
 * Common interface for all mappings that contain relational table information
 *
 * @author Steve Ebersole
 */
public interface TableInformationContainer {
	public String getSchema();

	public String getCatalog();

	public String getTable();

	public String getSubselect();

	public String getSubselectAttribute();
}
