/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

/**
 * @author Steve Ebersole
 */
public interface PhysicalMappedTable extends MappedTable {
	/**
	 * Get the qualified name for this MappedTable, including namespace
	 * name (catalog, schema).  The actual "physical name" (see
	 * {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy#toPhysicalTableName}) is not
	 * determined until later
	 */
	QualifiedName getLogicalName();
}
