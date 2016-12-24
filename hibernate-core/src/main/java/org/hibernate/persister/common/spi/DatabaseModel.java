/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.spi;

import org.hibernate.boot.model.naming.Identifier;

/**
 * @author Steve Ebersole
 */
public interface DatabaseModel {

	// todo : we need DatabaseModel to incorporate catalogs/schemas in some fashion
	//		either like org.hibernate.boot.model.relational.Database does
	//		or via names including catalogs/schemas names (e.g., `catalogName.schemaName.objectName`)

	// todo : how to model column readers/writers? on Column? or seperately on the things that use Column
	//		The latter would mean we'd have to allow for multiple Column defs based on name+reader/writer
	// 		in terms of rendering SQL
	//		I lean towards tracking these on Column, and either:
	//			1) verifying that all uses apply the same reader/writer
	//			2) allow multiple column references to apply a single reader/writer definition (the rest would be null/empty)

	PhysicalTable findPhysicalTableByLogicalName(Identifier logicalName);
	PhysicalTable findPhysicalTable(String name);

	DerivedTable findDerivedTable(String expression);
}
