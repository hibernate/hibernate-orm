/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * Defines environment's support for id-tables.  Generally this comes from
 * Dialect.
 *
 * @author Steve Ebersole
 */
public interface IdTableSupport {
	/**
	 * Determine the name to use for the id-table.
	 */
	String determineIdTableName(EntityDescriptor entityDescriptor);

	/**
	 * Get the SQL command needed to create the id-table
	 */
	String getCreateIdTableCommand();

	/**
	 * Get the options for id-table creation.  This is added to the
	 * end of the {@link #getCreateIdTableCommand()}, after the
	 * name is added.
	 */
	String getCreateIdTableStatementOptions();

	/**
	 * Get the SQL command for dropping the id-table
	 */
	String getDropIdTableCommand();
}
