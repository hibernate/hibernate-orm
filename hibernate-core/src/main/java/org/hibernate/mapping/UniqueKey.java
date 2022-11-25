/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.internal.util.StringHelper;

/**
 * A mapping model object representing a {@linkplain jakarta.persistence.UniqueConstraint unique key}
 * constraint on a relational database table.
 *
 * @author Brett Meyer
 */
public class UniqueKey extends Constraint {
	private final Map<Column, String> columnOrderMap = new HashMap<>();
	private boolean nameExplicit; // true when the constraint name was explicitly specified by @UniqueConstraint annotation

	@Override @Deprecated(since="6.2")
	public String sqlConstraintString(
			SqlStringGenerationContext context,
			String constraintName,
			String defaultCatalog,
			String defaultSchema) {
//		return dialect.getUniqueDelegate().uniqueConstraintSql( this );
		// Not used.
		return "";
	}

	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( StringHelper.isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	public Map<Column, String> getColumnOrderMap() {
		return columnOrderMap;
	}

	public String generatedConstraintNamePrefix() {
		return "UK_";
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getExportIdentifier(), "UK-" + getName() );
	}

	public boolean isNameExplicit() {
		return nameExplicit;
	}

	public void setNameExplicit(boolean nameExplicit) {
		this.nameExplicit = nameExplicit;
	}
}
