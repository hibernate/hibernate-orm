/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import java.util.Locale;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableReference;

/**
 * @author Steve Ebersole
 */
public class IdTableReference extends TableReference {
	public IdTableReference(
			IdTable table,
			String identificationVariable) {
		super( table, identificationVariable, false );
	}

	@Override
	public IdTable getTable() {
		return (IdTable) super.getTable();
	}

	@Override
	public IdTableReference locateTableReference(Table table) {
		if ( ! IdTable.class.isInstance( table ) ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Expecting Table passed to %s#locateTableReference to be of type %s, but found %s ",
							getClass().getName(),
							IdTable.class.getName(),
							table
					)
			);
		}

		return (IdTableReference) super.locateTableReference( table );
	}

	@Override
	public ColumnReference resolveColumnReference(Column column) {
		if ( ! IdTableColumn.class.isInstance( column ) ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Expecting Column passed to %s#resolveColumnReference to be of type %s, but found %s ",
							getClass().getName(),
							IdTableColumn.class.getName(),
							column
					)
			);
		}

		return super.resolveColumnReference( column );
	}
}
