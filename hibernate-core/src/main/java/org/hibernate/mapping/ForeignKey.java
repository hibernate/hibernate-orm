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
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.relational.ForeignKeyExporter;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.JavaTypeHelper;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;

/**
 * A foreign key constraint
 *
 * @author Gavin King
 */
public class ForeignKey extends Constraint implements MappedForeignKey {
	private MappedTable referencedTable;
	private String referencedEntityName;
	private String keyDefinition;
	private boolean cascadeDeleteEnabled;
	private List<MappedColumn> referencedColumns = new ArrayList<>();

	public ForeignKey() {
	}

	public org.hibernate.metamodel.model.relational.spi.ForeignKey generateRuntimeModel(
			RuntimeModelCreationContext creationContext,
			ForeignKeyExporter exporter) {
		// todo (6.0) : needed?  Depends how "ForeignKey resolver" works
		//		see `org.hibernate.boot.spi.InFlightMetadataCollector#registerForeignKeyCreator`
		throw new NotYetImplementedFor6Exception();
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

	@Override
	public MappedTable getReferencedTable() {
		return referencedTable;
	}

	@Override
	public MappedTable getTargetTable() {
		return getMappedTable();
	}


	private void appendColumns(StringBuilder buf, List<Selectable> columns) {
		boolean firstPass = true;

		for ( Selectable column : columns ) {
			if ( firstPass ) {
				firstPass = false;
			}
			else {
				buf.append( ',' );
			}
			buf.append( column.getText() );
		}
	}

	@Override
	public void setReferencedTable(MappedTable referencedTable) throws MappingException {
		this.referencedTable = referencedTable;
	}

	/**
	 * Validates that columnspan of the foreignkey and the primarykey is the same.
	 * <p/>
	 * Furthermore it aligns the length of the underlying tables columns.
	 */
	@Override
	public void alignColumns() {
		if ( isReferenceToPrimaryKey() ) {
			alignColumns( referencedTable );
		}
	}

	private void alignColumns(MappedTable referencedTable) {
		final List<Selectable> columns = JavaTypeHelper.cast( getColumns() );
		final List<Selectable> targetColumns = JavaTypeHelper.cast( referencedTable.getPrimaryKey().getColumns() );

		final int referencedPkColumnSpan = targetColumns.size();

		if ( referencedPkColumnSpan != columns.size() ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "Foreign key (" ).append( getName() ).append( ":" )
					.append( getMappedTable().getName() )
					.append( " [" );
			appendColumns( sb, columns );
			sb.append( "])" )
					.append( ") must have same number of columns as the referenced primary key (" )
					.append( referencedTable.getName() )
					.append( " [" );
			appendColumns( sb, targetColumns );
			sb.append( "])" );
			throw new MappingException( sb.toString() );
		}

		for ( int i = 0; i < columns.size(); i++ ) {
			if ( columns.get( i ) instanceof Column && targetColumns.get( i ) instanceof Column ) {
				( (Column) columns.get( i ) ).setLength(
						( (Column) targetColumns.get( i ) ).getLength()
				);
			}
		}
	}

	@Override
	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	@Override
	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	@Override
	public String getKeyDefinition() {
		return keyDefinition;
	}

	@Override
	public void setKeyDefinition(String keyDefinition) {
		this.keyDefinition = keyDefinition;
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}

	@Override
	public boolean isPhysicalConstraint() {
		return isCreationEnabled()
				&& referencedTable.isPhysicalTable()
				&& getMappedTable().isPhysicalTable()
				&& !referencedTable.hasDenormalizedTables();
	}

	/**
	 * Returns the referenced columns if the foreignkey does not refer to the primary key
	 */
	@Override
	public List<MappedColumn> getReferencedColumns() {
		return referencedColumns;
	}

	@Override
	public List<MappedColumn> getTargetColumns() {
		if ( referencedColumns != null && !referencedColumns.isEmpty() ) {
			return referencedColumns;
		}
		else {
			return getReferencedTable().getPrimaryKey().getColumns();
		}
	}

	/**
	 * Does this foreignkey reference the primary key of the reference table
	 */
	@Override
	public boolean isReferenceToPrimaryKey() {
		return referencedColumns.isEmpty();
	}

	/**
	 * @deprecated since 6.0, use {@link #addReferencedColumns(List<? extends MappedColumn>()}.
	 */
	@Deprecated
	public void addReferencedColumns(Iterator referencedColumnsIterator) {
		while ( referencedColumnsIterator.hasNext() ) {
			Selectable col = (Selectable) referencedColumnsIterator.next();
			if ( !col.isFormula() ) {
				addReferencedColumn( (Column) col );
			}
		}
	}

	@Override
	public void addReferencedColumns(List<? extends MappedColumn> referencedColumns) {
		addReferencedColumns( referencedColumns.iterator() );
	}

	private void addReferencedColumn(Column column) {
		if ( !referencedColumns.contains( column ) ) {
			referencedColumns.add( column );
		}
	}

	@Override
	public String generatedConstraintNamePrefix() {
		return "FK_";
	}

	public String toString() {
		return String.format(
				Locale.ROOT,
				"Boot-model ForeignKey[ (%s) => (%s) ]",
				getColumns(),
				getReferencedColumns()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * All schema management is managed though exporters
	 *
	 * @deprecated no mas
	 */
	@Deprecated
	public String sqlConstraintString(
			Dialect dialect,
			String constraintName,
			String defaultCatalog,
			String defaultSchema) {
		if ( isCreationEnabled() ) {
			String[] columnNames = new String[getColumnSpan()];
			String[] referencedColumnNames = new String[getColumnSpan()];

			final List<MappedColumn> targetColumns = getTargetColumns();

			final List<MappedColumn> referencingColumns = getColumns();
			for ( int i = 0; i < referencingColumns.size(); i++ ) {
				columnNames[i] = ( (Column) referencingColumns.get( i ) ).getName().render( dialect );
				referencedColumnNames[i] = ( (Column) targetColumns.get( i ) ).getName().render( dialect );
			}

			final String result = keyDefinition != null
					? dialect.getAddForeignKeyConstraintString(
					constraintName,
					keyDefinition
			)
					: dialect.getAddForeignKeyConstraintString(
					constraintName,
					columnNames,
					// Don't pull this `#getQualifiedName` up to MappedTable - it is only
					// 		used from legacy schema-management tooling using the boot-model.
					//
					//		1) it
					( (Table) referencedTable ).getQualifiedName(
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
		else {
			throw new MappingException( "A Sql constrain string cannot be created, the fk creation is disabled" );
		}
	}


	/**
	 * All schema management is managed though exporters
	 *
	 * @deprecated no mas
	 */
	@Deprecated
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
}
