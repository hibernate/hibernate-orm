/**
 * 
 */
package org.hibernate.tool.internal.reveng.strategy;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.tool.api.reveng.RevengStrategy;

public class TableSelectorStrategy extends DelegatingStrategy {
	
	List<SchemaSelection> selections = new ArrayList<SchemaSelection>();
	
	public TableSelectorStrategy(RevengStrategy res) {
		super(res);
	}
	
	public List<SchemaSelection> getSchemaSelections() {
		return selections;
	}
	

	public void clearSchemaSelections() {
		selections.clear();
	}
	
	public void addSchemaSelection(SchemaSelection selection) {
		selections.add(selection);
	}	
}