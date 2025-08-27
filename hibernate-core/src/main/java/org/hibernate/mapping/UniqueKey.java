/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.HashMap;
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

	private final Map<Column, String> columnOrderMap = new HashMap<>();
	private boolean nameExplicit; // true when the constraint name was explicitly specified by @UniqueConstraint annotation
	private boolean explicit; // true when the constraint was explicitly specified by @UniqueConstraint annotation

	public UniqueKey(Table table) {
		super( table );
	}

	@Deprecated(since = "7")
	public UniqueKey() {
	}

	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	public Map<Column, String> getColumnOrderMap() {
		return columnOrderMap;
	}

	@Override
	public String getExportIdentifier() {
		return qualify( getTable().getExportIdentifier(), "UK-" + getName() );
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

	public boolean hasNullableColumn() {
		for ( Column column : getColumns() ) {
			final Column tableColumn = getTable().getColumn( column );
			if ( tableColumn != null && tableColumn.isNullable() ) {
				return true;
			}
		}
		return false;
	}
}
