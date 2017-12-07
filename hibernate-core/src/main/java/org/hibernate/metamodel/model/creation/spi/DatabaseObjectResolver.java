/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import java.util.List;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.mapping.Selectable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * @author Steve Ebersole
 */
public interface DatabaseObjectResolver {
	Table resolveTable(MappedTable mappedTable);

	Column resolveColumn(MappedColumn mappedColumn);

	ForeignKey.ColumnMappings resolveColumnMappings(List<Selectable> columns, List<Selectable> otherColumns);

	ForeignKey resolveForeignKey(MappedForeignKey bootForeignKey);
}
