/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * An object that produces an {@code alter table} statements
 * needed to update the definition of a table.
 *
 * @author Gavin King
 *
 * @since 6.2
 */
public interface TableMigrator {
	String[] getSqlAlterStrings(
			Table table,
			Metadata metadata,
			TableInformation tableInfo,
			SqlStringGenerationContext context);
}
