/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2015-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.reveng.internal.core.reader;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.TableIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ForeignKeysInfo {

	final Map<String, Table> dependentTables;
	final Map<String, List<Column>> dependentColumns;
	final Map<String, List<Column>> referencedColumns;
	private final Table referencedTable;
	
	public ForeignKeysInfo(
			Table referencedTable, 
			Map<String, Table> tables, 
			Map<String, List<Column>> columns, 
			Map<String, List<Column>> refColumns) {
		this.referencedTable = referencedTable;
		this.dependentTables = tables;
		this.dependentColumns = columns;
		this.referencedColumns = refColumns;
	}
	
	public Map<String, List<ForeignKey>> process(RevengStrategy revengStrategy) {
		Map<String, List<ForeignKey>> oneToManyCandidates = new HashMap<String, List<ForeignKey>>();
        Iterator<Entry<String, Table>> iterator = dependentTables.entrySet().iterator();
		while (iterator.hasNext() ) {
			Entry<String, Table> entry = iterator.next();
			String fkName = entry.getKey();
			Table fkTable = entry.getValue();			
			List<Column> columns = dependentColumns.get(fkName);
			List<Column> refColumns = referencedColumns.get(fkName);
			
			String className = revengStrategy.tableToClassName(TableIdentifier.create(referencedTable) );

			ForeignKey key = fkTable.createForeignKey(fkName, columns, className, null, null, refColumns);			
			key.setReferencedTable(referencedTable);

			addToMultiMap(oneToManyCandidates, className, key);				
		}
		return oneToManyCandidates;
	}

	private void addToMultiMap(Map<String, List<ForeignKey>> multimap, String key, ForeignKey item) {
		List<ForeignKey> existing = multimap.get(key);
		if(existing == null) {
			existing = new ArrayList<ForeignKey>();
			multimap.put(key, existing);
		}
		existing.add(item);
	}

}
