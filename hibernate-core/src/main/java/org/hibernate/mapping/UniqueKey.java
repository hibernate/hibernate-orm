/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.Map;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * A mapping model object representing a {@linkplain jakarta.persistence.UniqueConstraint unique key}
 * constraint on a relational database table.
 *
 * @author Brett Meyer
 */
public class UniqueKey extends Constraint {

	private final SelectableOrderings<Column> columnOrderMap = new SelectableOrderings<>();
	private boolean nameExplicit; // true when the constraint name was explicitly specified by @UniqueConstraint annotation
	private boolean explicit; // true when the constraint was explicitly specified by @UniqueConstraint annotation
	private boolean nullsNotDistinct;

	public UniqueKey(Table table) {
		super( table );
	}

	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	public Map<Column, String> getColumnOrderMap() {
		return columnOrderMap.asMap();
	}

	void visitOrderingSelectables(java.util.function.Consumer<Selectable> consumer) {
		columnOrderMap.visit( consumer );
	}

	@Override
	public String getExportIdentifier() {
		return qualify( getTableExportIdentifier(), "UK-" + getName() );
	}

	public boolean isNameExplicit() {
		return nameExplicit;
	}

	public void setNameExplicit(boolean nameExplicit) {
		this.nameExplicit = nameExplicit;
	}

	public boolean isExplicit() {
		return explicit;
	}

	public void setExplicit(boolean explicit) {
		this.explicit = explicit;
	}

	public boolean isNullsNotDistinct() {
		return nullsNotDistinct;
	}

	public void setNullsNotDistinct(boolean nullsNotDistinct) {
		this.nullsNotDistinct = nullsNotDistinct;
	}

	public boolean hasNullableColumn() {
		for ( var column : getColumns() ) {
			final var tableColumn = getTable().getColumn( column );
			if ( tableColumn != null && tableColumn.isNullable() ) {
				return true;
			}
		}
		return false;
	}
}
