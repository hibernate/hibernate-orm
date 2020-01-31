/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.Joinable;

/**
 * @author Steve Ebersole
 */
public class IdTable implements Exportable {
	private final EntityMappingType entityDescriptor;
	private final String qualifiedTableName;

	private IdTableSessionUidColumn sessionUidColumn;
	private final List<IdTableColumn> columns = new ArrayList<>();

	public IdTable(
			EntityMappingType entityDescriptor,
			Function<String,String> idTableNameAdjuster) {
		this.entityDescriptor = entityDescriptor;

		this.qualifiedTableName = idTableNameAdjuster.apply(
				( (Joinable) entityDescriptor.getEntityPersister() ).getTableName()
		);

		entityDescriptor.getIdentifierMapping().visitColumns(
				(columnExpression, containingTableExpression, jdbcMapping) -> columns.add(
						new IdTableColumn(
								this,
								columnExpression,
								jdbcMapping
						)
				)
		);
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	public String getQualifiedTableName() {
		return qualifiedTableName;
	}

	public List<IdTableColumn> getIdTableColumns() {
		return columns;
	}

	public IdTableSessionUidColumn getSessionUidColumn() {
		return sessionUidColumn;
	}

	public String getTableExpression() {
		return qualifiedTableName;
	}

	public void addColumn(IdTableColumn column) {
		columns.add( column );
		if ( column instanceof IdTableSessionUidColumn ) {
			this.sessionUidColumn = (IdTableSessionUidColumn) column;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Exportable

	@Override
	public String getExportIdentifier() {
		return getQualifiedTableName();
	}
}
