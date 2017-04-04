/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

/**
 * A foreign key constraint
 *
 * @author Gavin King
 */
public class ForeignKey extends Constraint {
	private Table referencedTable;
	private String referencedEntityName;
	private String keyDefinition;
	private boolean cascadeDeleteEnabled;
	private List<Column> referencedColumns = new ArrayList<Column>();
	private boolean creationEnabled = true;

	public ForeignKey() {
	}

	@Override
	public String getExportIdentifier() {
		// NOt sure name is always set.  Might need some implicit naming
		return StringHelper.qualify( getTable().getName(), "FK-" + getName() );
	}

	public void disableCreation() {
		creationEnabled = false;
	}

	public boolean isCreationEnabled() {
		return creationEnabled;
	}

	@Override
	public void setName(String name) {
		super.setName( name );
		// the FK name "none" is a magic value in the hbm.xml binding that indicated to
		// not create a FK.
		if ( "none".equals( name ) ) {
			disableCreation();
		}
	}

	public String sqlConstraintString(
			Dialect dialect,
			String constraintName,
			String defaultCatalog,
			String defaultSchema) {
		String[] columnNames = new String[getColumnSpan()];
		String[] referencedColumnNames = new String[getColumnSpan()];

		final Iterator<Column> referencedColumnItr;
		if ( isReferenceToPrimaryKey() ) {
			referencedColumnItr = referencedTable.getPrimaryKey().getColumnIterator();
		}
		else {
			referencedColumnItr = referencedColumns.iterator();
		}

		Iterator columnItr = getColumnIterator();
		int i = 0;
		while ( columnItr.hasNext() ) {
			columnNames[i] = ( (Column) columnItr.next() ).getQuotedName( dialect );
			referencedColumnNames[i] = referencedColumnItr.next().getQuotedName( dialect );
			i++;
		}

		final String result = keyDefinition != null ?
				dialect.getAddForeignKeyConstraintString(
						constraintName,
						keyDefinition
				) :
				dialect.getAddForeignKeyConstraintString(
						constraintName,
						columnNames,
						referencedTable.getQualifiedName(
								dialect,
								defaultCatalog,
								defaultSchema
						),
						referencedColumnNames,
						isReferenceToPrimaryKey()
				);
		
		return cascadeDeleteEnabled && dialect.supportsCascadeDelete()
				? result + " on delete cascade"
				: result;
	}

	public Table getReferencedTable() {
		return referencedTable;
	}

	private void appendColumns(StringBuilder buf, Iterator columns) {
		while ( columns.hasNext() ) {
			Column column = (Column) columns.next();
			buf.append( column.getName() );
			if ( columns.hasNext() ) {
				buf.append( "," );
			}
		}
	}

	public void setReferencedTable(Table referencedTable) throws MappingException {
		//if( isReferenceToPrimaryKey() ) alignColumns(referencedTable); // TODO: possibly remove to allow more piecemal building of a foreignkey.  

		this.referencedTable = referencedTable;
	}

	/**
	 * Validates that columnspan of the foreignkey and the primarykey is the same.
	 * <p/>
	 * Furthermore it aligns the length of the underlying tables columns.
	 */
	public void alignColumns() {
		if ( isReferenceToPrimaryKey() ) {
			alignColumns( referencedTable );
		}
	}

	private void alignColumns(Table referencedTable) {
		final int referencedPkColumnSpan = referencedTable.getPrimaryKey().getColumnSpan();
		if ( referencedPkColumnSpan != getColumnSpan() ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "Foreign key (" ).append( getName() ).append( ":" )
					.append( getTable().getName() )
					.append( " [" );
			appendColumns( sb, getColumnIterator() );
			sb.append( "])" )
					.append( ") must have same number of columns as the referenced primary key (" )
					.append( referencedTable.getName() )
					.append( " [" );
			appendColumns( sb, referencedTable.getPrimaryKey().getColumnIterator() );
			sb.append( "])" );
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

	public String getKeyDefinition() {
		return keyDefinition;
	}

	public void setKeyDefinition(String keyDefinition) {
		this.keyDefinition = keyDefinition;
	}
	
	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		final StringBuilder buf = new StringBuilder( "alter table " );
		buf.append( getTable().getQualifiedName( dialect, defaultCatalog, defaultSchema ) );
		buf.append( dialect.getDropForeignKeyString() );
		if ( dialect.supportsIfExistsBeforeConstraintName() ) {
			buf.append( "if exists " );
		}
		buf.append( dialect.quote( getName() ) );
		if ( dialect.supportsIfExistsAfterConstraintName() ) {
			buf.append( " if exists" );
		}
		return buf.toString();
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}

	public boolean isPhysicalConstraint() {
		return referencedTable.isPhysicalTable()
				&& getTable().isPhysicalTable()
				&& !referencedTable.hasDenormalizedTables();
	}

	/**
	 * Returns the referenced columns if the foreignkey does not refer to the primary key
	 */
	public List getReferencedColumns() {
		return referencedColumns;
	}

	/**
	 * Does this foreignkey reference the primary key of the reference table
	 */
	public boolean isReferenceToPrimaryKey() {
		return referencedColumns.isEmpty();
	}

	public void addReferencedColumns(Iterator referencedColumnsIterator) {
		while ( referencedColumnsIterator.hasNext() ) {
			Selectable col = (Selectable) referencedColumnsIterator.next();
			if ( !col.isFormula() ) {
				addReferencedColumn( (Column) col );
			}
		}
	}

	private void addReferencedColumn(Column column) {
		if ( !referencedColumns.contains( column ) ) {
			referencedColumns.add( column );
		}
	}

	public String toString() {
		if ( !isReferenceToPrimaryKey() ) {
			return getClass().getName()
					+ '(' + getTable().getName() + getColumns()
					+ " ref-columns:" + '(' + getReferencedColumns() + ") as " + getName() + ")";
		}
		else {
			return super.toString();
		}

	}

	public String generatedConstraintNamePrefix() {
		return "FK_";
	}
}
