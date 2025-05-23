/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.Metadata;

import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * A mapping model object representing a {@linkplain jakarta.persistence.ForeignKey foreign key} constraint.
 *
 * @author Gavin King
 */
public class ForeignKey extends Constraint {

	private Table referencedTable;
	private String referencedEntityName;
	private String keyDefinition;
	private OnDeleteAction onDeleteAction;
	private final List<Column> referencedColumns = new ArrayList<>();
	private boolean creationEnabled = true;

	public ForeignKey(Table table) {
		super( table );
	}

	@Deprecated(since = "7")
	public ForeignKey() {
	}

	@Override
	public String getExportIdentifier() {
		// Not sure name is always set.  Might need some implicit naming
		return qualify( getTable().getExportIdentifier(), "FK-" + getName() );
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
		// the FK name "none" was a magic value in the hbm.xml
		// mapping language that indicated to not create a FK
		if ( "none".equals( name ) ) {
			disableCreation();
		}
	}

	public Table getReferencedTable() {
		return referencedTable;
	}

	private void appendColumns(StringBuilder buf, Iterator<Column> columns) {
		while ( columns.hasNext() ) {
			Column column = columns.next();
			buf.append( column.getName() );
			if ( columns.hasNext() ) {
				buf.append( "," );
			}
		}
	}

	public void setReferencedTable(Table referencedTable) throws MappingException {
		this.referencedTable = referencedTable;
	}

	/**
	 * Validates that column span of the foreign key and the primary key is the same.
	 * <p>
	 * Furthermore it aligns the length of the underlying tables columns.
	 */
	public void alignColumns() {
		if ( isReferenceToPrimaryKey() ) {
			final int columnSpan = getColumnSpan();
			final PrimaryKey primaryKey = referencedTable.getPrimaryKey();
			if ( primaryKey.getColumnSpan() != columnSpan ) {
				StringBuilder sb = new StringBuilder();
				sb.append( "Foreign key (" ).append( getName() ).append( ":" )
						.append( getTable().getName() )
						.append( " [" );
				appendColumns( sb, getColumns().iterator() );
				sb.append( "])" )
						.append( ") must have same number of columns as the referenced primary key (" )
						.append( referencedTable.getName() )
						.append( " [" );
				appendColumns( sb, primaryKey.getColumns().iterator() );
				sb.append( "])" );
				throw new MappingException( sb.toString() );
			}

			//TODO: shouldn't this happen even for non-PK references?
			for ( int i = 0; i<columnSpan; i++ ) {
				Column referencedColumn = primaryKey.getColumn(i);
				Column referencingColumn = getColumn(i);
				referencingColumn.setLength( referencedColumn.getLength() );
				referencingColumn.setScale( referencedColumn.getScale() );
				referencingColumn.setPrecision( referencedColumn.getPrecision() );
				referencingColumn.setArrayLength( referencedColumn.getArrayLength() );
			}
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

	public void setOnDeleteAction(OnDeleteAction onDeleteAction) {
		this.onDeleteAction = onDeleteAction;
	}

	public OnDeleteAction getOnDeleteAction() {
		return onDeleteAction;
	}

	public boolean isPhysicalConstraint() {
		return referencedTable.isPhysicalTable()
			&& getTable().isPhysicalTable()
			&& !referencedTable.hasDenormalizedTables();
	}

	/**
	 * Returns the referenced columns if the foreignkey does not refer to the primary key
	 */
	public List<Column> getReferencedColumns() {
		return referencedColumns;
	}

	/**
	 * Does this foreignkey reference the primary key of the reference table
	 */
	public boolean isReferenceToPrimaryKey() {
		return referencedColumns.isEmpty();
	}

	public void addReferencedColumns(List<Column> referencedColumns) {
		for (Column referencedColumn : referencedColumns) {
//			if ( !referencedColumn.isFormula() ) {
				addReferencedColumn( referencedColumn );
//			}
		}
	}

	private void addReferencedColumn(Column column) {
		if ( !referencedColumns.contains( column ) ) {
			referencedColumns.add( column );
		}
	}

	public String toString() {
		if ( !isReferenceToPrimaryKey() ) {
			return getClass().getSimpleName()
					+ '(' + getTable().getName() + getColumns()
					+ " ref-columns:" + '(' + getReferencedColumns() + ") as " + getName() + ")";
		}
		else {
			return super.toString();
		}

	}

	@Internal
	public PersistentClass resolveReferencedClass(Metadata metadata) {
		final String referencedEntityName = getReferencedEntityName();
		if ( referencedEntityName == null ) {
			throw new MappingException( "An association from the table '" + getTable().getName() +
					"' does not specify the referenced entity" );
		}

		final PersistentClass referencedClass = metadata.getEntityBinding( referencedEntityName );
		if ( referencedClass == null ) {
			throw new MappingException( "An association from the table '" + getTable().getName() +
					"' refers to an unmapped class '" + referencedEntityName + "'" );
		}

		return referencedClass;
	}
}
