/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.mapping;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;

/**
 * A foreign key constraint
 * @author Gavin King
 */
public class ForeignKey extends Constraint {

	private Table referencedTable;
	private String referencedEntityName;
	private boolean cascadeDeleteEnabled;
	private List referencedColumns = new ArrayList();    
    
	public String sqlConstraintString(Dialect dialect, String constraintName, String defaultCatalog, String defaultSchema) {
		String[] cols = new String[ getColumnSpan() ];
		String[] refcols = new String[ getColumnSpan() ];
		int i=0;
		Iterator refiter = null;
		if(isReferenceToPrimaryKey() ) {
			refiter = referencedTable.getPrimaryKey().getColumnIterator();
		} 
		else {
			refiter = referencedColumns.iterator();
		}
		
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			cols[i] = ( (Column) iter.next() ).getQuotedName(dialect);
			refcols[i] = ( (Column) refiter.next() ).getQuotedName(dialect);
			i++;
		}
		String result = dialect.getAddForeignKeyConstraintString(
			constraintName, cols, referencedTable.getQualifiedName(dialect, defaultCatalog, defaultSchema), refcols, isReferenceToPrimaryKey()
		);
		return cascadeDeleteEnabled && dialect.supportsCascadeDelete() ? 
			result + " on delete cascade" : 
			result;
	}

	public Table getReferencedTable() {
		return referencedTable;
	}

	private void appendColumns(StringBuilder buf, Iterator columns) {
		while( columns.hasNext() ) {
			Column column = (Column) columns.next();
			buf.append( column.getName() );
			if ( columns.hasNext() ) buf.append(",");
		}
	}

	public void setReferencedTable(Table referencedTable) throws MappingException {
		//if( isReferenceToPrimaryKey() ) alignColumns(referencedTable); // TODO: possibly remove to allow more piecemal building of a foreignkey.  
		
		this.referencedTable = referencedTable;
	}

	/**
	 * Validates that columnspan of the foreignkey and the primarykey is the same.
	 * 
	 * Furthermore it aligns the length of the underlying tables columns.
	 */
	public void alignColumns() {
		if ( isReferenceToPrimaryKey() ) alignColumns(referencedTable);
	}
	
	private void alignColumns(Table referencedTable) {
		if ( referencedTable.getPrimaryKey().getColumnSpan()!=getColumnSpan() ) {
			StringBuilder sb = new StringBuilder();
			sb.append("Foreign key (")
                .append( getName() + ":")
				.append( getTable().getName() )
				.append(" [");
			appendColumns( sb, getColumnIterator() );
			sb.append("])")
				.append(") must have same number of columns as the referenced primary key (")
				.append( referencedTable.getName() )
				.append(" [");
			appendColumns( sb, referencedTable.getPrimaryKey().getColumnIterator() );
			sb.append("])");
			throw new MappingException( sb.toString() );
		}
		
		Iterator fkCols = getColumnIterator();
		Iterator pkCols = referencedTable.getPrimaryKey().getColumnIterator();
		while ( pkCols.hasNext() ) {
			( (Column) fkCols.next() ).setLength( ( (Column) pkCols.next() ).getLength() );
		}

	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		return "alter table " + 
			getTable().getQualifiedName(dialect, defaultCatalog, defaultSchema) + 
			dialect.getDropForeignKeyString() + 
			getName();
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}
	
	public boolean isPhysicalConstraint() {
		return referencedTable.isPhysicalTable() && 
				getTable().isPhysicalTable() && 
				!referencedTable.hasDenormalizedTables();
	}

	/** Returns the referenced columns if the foreignkey does not refer to the primary key */
	public List getReferencedColumns() {
		return referencedColumns;		
	}

	/** Does this foreignkey reference the primary key of the reference table */ 
	public boolean isReferenceToPrimaryKey() {
		return referencedColumns.isEmpty();
	}

	public void addReferencedColumns(Iterator referencedColumnsIterator) {
		while ( referencedColumnsIterator.hasNext() ) {
			Selectable col = (Selectable) referencedColumnsIterator.next();
			if ( !col.isFormula() ) addReferencedColumn( (Column) col );
		}
	}

	private void addReferencedColumn(Column column) {
		if ( !referencedColumns.contains(column) ) referencedColumns.add(column);		
	}
	
	public String toString() {
		if(!isReferenceToPrimaryKey() ) {
			StringBuilder result = new StringBuilder(getClass().getName() + '(' + getTable().getName() + getColumns() );
			result.append( " ref-columns:" + '(' + getReferencedColumns() );
			result.append( ") as " + getName() );
			return result.toString();
		} 
		else {
			return super.toString();
		}
		
	}
}
