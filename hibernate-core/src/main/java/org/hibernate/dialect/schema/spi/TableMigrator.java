/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.TableInformation;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Produces the alter-table commands needed to update a table definition.
///
/// Implementations must preserve command order and must not mutate the mapping
/// or extracted table information.
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getTableMigrator()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface TableMigrator {
	String[] getSqlAlterStrings(
			Table table,
			Metadata metadata,
			TableInformation tableInfo,
			SqlStringGenerationContext context);
}
