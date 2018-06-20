package org.hibernate.tool.internal.export.lint;

import java.util.Iterator;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

public abstract class RelationalModelDetector extends Detector {

	public void visit(IssueCollector collector) {
		for (Iterator<Table> iter = getMetadata().collectTableMappings().iterator(); iter.hasNext();) {
			Table table = (Table) iter.next();
			this.visit(table, collector);
		}					
	}
	
	abstract protected void visit(Table table, Column col, IssueCollector collector);

	protected void visitColumns(Table table, IssueCollector collector) {
		Iterator<?> columnIter = table.getColumnIterator();
		while ( columnIter.hasNext() ) {
			Column col = ( Column ) columnIter.next();
			this.visit(table, col, collector );
		}		
	}

	/**
	 * @return true if visit should continue down through the columns 
	 */
	protected void visit(Table table, IssueCollector collector) {
		visitColumns(table, collector);
	}
	
}

