/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.relational.MappedUniqueKey;
import org.hibernate.internal.util.StringHelper;

/**
 * A relational unique key constraint
 *
 * @author Brett Meyer
 */
public class UniqueKey extends Constraint implements MappedUniqueKey {
	private java.util.Map<Column, String> columnOrderMap = new HashMap<>();

	@Override
	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( StringHelper.isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	@Override
	public Map<Column, String> getColumnOrderMap() {
		return columnOrderMap;
	}

	@Override
	public String generatedConstraintNamePrefix() {
		return "UK_";
	}

}
