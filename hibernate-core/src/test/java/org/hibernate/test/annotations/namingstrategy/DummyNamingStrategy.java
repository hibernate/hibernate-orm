// $Id$
package org.hibernate.test.annotations.namingstrategy;
import java.util.List;

import org.hibernate.cfg.DefaultNamingStrategy;

@SuppressWarnings("serial")
public class DummyNamingStrategy extends DefaultNamingStrategy {
	
	private int counter = 0;
	
	@Override
	public String tableName(String tableName) {
		return "T" + tableName;
	}
	
	@Override
	public String foreignKeyName(String sourceTableName, List<String> sourceColumnNames,
			String targetTableName, List<String> targetColumnNames) {
		return "F" + counter++;
	}
	
	@Override
	public String uniqueKeyName(String tableName, List<String> columnNames) {
		StringBuilder sb = new StringBuilder();
		for ( String columnName : columnNames ) {
			sb.append( columnName );
		}
		return "U" + sb.toString();
	}
	
	@Override
	public String indexName(String tableName, List<String> columnNames) {
		StringBuilder sb = new StringBuilder();
		for ( String columnName : columnNames ) {
			sb.append( columnName );
		}
		return "I" + sb.toString();
	}

}
