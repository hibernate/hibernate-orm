package org.hibernate.tool.internal.reveng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;

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
	
	public Map<String, List<ForeignKey>> process(ReverseEngineeringStrategy revengStrategy) {
		Map<String, List<ForeignKey>> oneToManyCandidates = new HashMap<String, List<ForeignKey>>();
        Iterator<Entry<String, Table>> iterator = dependentTables.entrySet().iterator();
		while (iterator.hasNext() ) {
			Entry<String, Table> entry = iterator.next();
			String fkName = entry.getKey();
			Table fkTable = entry.getValue();			
			List<Column> columns = dependentColumns.get(fkName);
			List<Column> refColumns = referencedColumns.get(fkName);
			
			String className = revengStrategy.tableToClassName(TableIdentifier.create(referencedTable) );

			ForeignKey key = fkTable.createForeignKey(fkName, columns, className, null, refColumns);			
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
