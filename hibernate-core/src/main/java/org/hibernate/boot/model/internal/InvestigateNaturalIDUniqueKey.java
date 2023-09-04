/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.hibernate.boot.model.internal.PropertyBinder.getNaturalIDUniqueKeyName;

public class InvestigateNaturalIDUniqueKey implements SecondPass {

	private final Table table;

	public InvestigateNaturalIDUniqueKey(Table table) {
		this.table = table;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		String uniqueKeyName = getNaturalIDUniqueKeyName(table);
		UniqueKey uniqueKey = table.getUniqueKey(uniqueKeyName);
		if (uniqueKey == null) {
			return;
		}
		List<Column> naturalIDColumns = uniqueKey.getColumns();
		for (Map.Entry<String, UniqueKey> key : table.getRawUniqueKeys().entrySet()) {
			if (key.getKey().equals(uniqueKeyName)) {
				continue;
			}

			if (new HashSet<>(key.getValue().getColumns()).containsAll(naturalIDColumns)) {
				table.removeUniqueKey(uniqueKeyName);
				return;
			}
		}
	}
}
