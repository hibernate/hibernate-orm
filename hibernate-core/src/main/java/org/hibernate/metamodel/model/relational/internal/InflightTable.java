/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.internal;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.PrimaryKey;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.metamodel.model.relational.spi.UniqueKey;

/**
 * @author Steve Ebersole
 */
public interface InflightTable extends Table {
	void addColumn(Column column);

	ForeignKey createForeignKey(
			String name,
			boolean export,
			String keyDefinition,
			boolean cascadeDeleteEnabled,
			boolean isReferenceToPrimaryKey,
			Table targetTable,
			ForeignKey.ColumnMappings columnMappings);

	UniqueKey createUniqueKey(String name);

	void addPrimaryKey(PrimaryKey primaryKey);
}
