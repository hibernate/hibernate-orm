/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.naming.QualifiedTableName;

/**
 * @author Steve Ebersole
 */
public class IdTable extends PhysicalTable {
	private final EntityDescriptor entityDescriptor;

	public IdTable(
			EntityDescriptor entityDescriptor,
			QualifiedTableName physicalQualifiedName) {
		super(
				physicalQualifiedName,
				true,
				"Table used to temporarily hold id values for the entity " + entityDescriptor.getEntityName()
		);
		this.entityDescriptor = entityDescriptor;
	}

	public EntityDescriptor getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public String getTableExpression() {
		return getQualifiedTableName().getTableName().getText();
	}

	@Override
	public boolean isExportable() {
		return false;
	}

	@Override
	public void addColumn(Column column) {
		if ( ! IdTableColumn.class.isInstance( column ) ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Columns added to an IdTable must be typed as %s, but found %s",
							IdTableColumn.class.getName(),
							column
					)
			);
		}

		super.addColumn( column );
	}

	@Override
	public ForeignKey createForeignKey(
			String name,
			boolean export,
			String keyDefinition,
			boolean cascadeDeleteEnabled,
			Table targetTable,
			ForeignKey.ColumnMappings columnMappings) {
		throw new UnsupportedOperationException( "Cannot generate FK on entity id table" );
	}
}
