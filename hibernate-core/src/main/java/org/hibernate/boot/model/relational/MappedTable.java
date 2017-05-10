/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import java.util.List;
import java.util.Set;

import org.hibernate.mapping.ForeignKey;

/**
 * Models any mapped "table reference" (e.g. a physical table, an in-lined
 * view, etc).
 *
 * @author Steve Ebersole
 */
public interface MappedTable extends Exportable, Loggable {
	/**
	 * Get an identifier for this MappedTable that is unique across all
	 * MappedTable references in a given {@link Database}.
	 * <p/>
	 * Think "surrogate primary key" relative to Database.
	 */
	String getUid();

	/**
	 * Will this MappedTable physically be exported as per
	 * {@link Exportable}?  Or is it "virtual"?
	 */
	boolean isExportable();

	/**
	 * Retrieve all columns defined for this table.  The returned Set has
	 * an iteration order defined as the order that columns are encountered
	 * while processing the application's mappings.
	 */
	Set<MappedColumn> getMappedColumns();

	// todo (6.0) : others as deemed appropriate - see o.h.mapping.Table

	/**
	 * Get the name of the mapped table.
	 */
	String getName();

	/**
	 * Get the schema associated to the mapped table.
	 */
	String getSchema();

	/**
	 * Get the catalog associated to the mapped table.
	 */
	String getCatalog();

	/**
	 * Create a foreign key associated to the mapped table.
	 *
	 * @param keyName the foreign key name.
	 * @param keyColumns the columns to be associated with the foreign key.
	 * @param referencedEntityName the referenced entity name.
	 * @param keyDefinition foreign key definition
	 * @return the constructed foreign key.
	 */
	ForeignKey createForeignKey(String keyName, List keyColumns, String referencedEntityName, String keyDefinition);

}
