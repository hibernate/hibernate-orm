/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.id.uuid.LocalObjectUuidHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.streams.GenericArrayCollector;
import org.hibernate.metamodel.model.relational.internal.ColumnMappingImpl;
import org.hibernate.metamodel.model.relational.internal.ColumnMappingsImpl;
import org.hibernate.metamodel.model.relational.internal.InflightTable;

import static org.hibernate.mapping.Constraint.hashedName;

/**
 * @author Steve Ebersole
 */
public class ForeignKey implements Exportable {
	private final String name;
	private final Table referringTable;
	private final Table targetTable;
	private final ColumnMappings columnMappings;
	private final boolean cascadeDeleteEnabled;
	private final boolean isReferenceToPrimaryKey;
	private final boolean export;
	private final String keyDefinition;

	public ForeignKey(
			String name,
			boolean export,
			String keyDefinition,
			boolean cascadeDeleteEnabled,
			boolean isReferenceToPrimaryKey,
			Table referringTable,
			Table targetTable,
			ColumnMappings columnMappings) {
		this.name = name;
		this.export = export;
		this.keyDefinition = keyDefinition;
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
		this.isReferenceToPrimaryKey = isReferenceToPrimaryKey;
		this.referringTable = referringTable;
		this.targetTable = targetTable;
		this.columnMappings = columnMappings;
	}

	public String getName() {
		return name;
	}

	public Table getReferringTable() {
		return referringTable;
	}

	public Table getTargetTable() {
		return targetTable;
	}

	public ColumnMappings getColumnMappings() {
		return columnMappings;
	}

	public boolean isExportationEnabled() {
		return export;
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( ( (ExportableTable) referringTable ).getTableName().getText(), "FK-" + getName() );
	}

	/**
	 * Does this foreignkey reference the primary key of the reference table
	 */
	public boolean isReferenceToPrimaryKey() {
		return isReferenceToPrimaryKey;
	}

	public String getKeyDefinition() {
		return keyDefinition;
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	/**
	 * Models the metadata for a foreign key (physical or logical).
	 * <p/>
	 * The terms "target" and "referring" used here are relative to the
	 * direction of the FK.  E.g. given a table {@code ADDRESS} that defines a foreign key
	 * referring to a {@code PERSON} table... {@code ADDRESS} would be the referencing
	 * table and {@code PERSON} would be the target table.  The columns in {@code ADDRESS}
	 * making up the FK would be the referring columns, and the columns on the {@code PERSON}
	 * table (referred to by the FK) would be the target columns:
	 * <pre>{@code
	 *     referring                target
	 *     -----------------        ----------------------------
	 *     ADDRESS             ->   PERSON
	 *         PERSON_ID       ->       ID
	 *     }
	 * </pre>
	 * <p/>
	 * Note that this is made an interface to allow for variants of FK declaration to
	 * potentially provide dedicated impls.  These variants include:<ul>
	 *     <li>FKs that reference to the target table's PK and that name the columns the same</li>
	 *     <li>FKs that reference to the target table's PK and define them in the same order</li>
	 *     <li>FKs that reference to column(s) in the target table that are not its defined PK</li>
	 * </ul>
	 * <p/>
	 * Note also that it is expected that the ColumnMappings have been validated prior to passing the in:<ul>
	 *     <li>both sides define the same number of Columns</li>
	 *     <li>
	 *         all referring Columns come from the referring Table and that all target Columns come from the
	 *         target Table as defined by {@link Column#getSourceTable()}
	 *     </li>
	 * </ul>
	 */
	public interface ColumnMappings {
		Table getReferringTable();
		Table getTargetTable();

		List<ColumnMapping> getColumnMappings();

		default List<Column> getReferringColumns() {
			return getColumnMappings()
					.stream()
					.map( ColumnMapping::getReferringColumn )
					.collect( Collectors.toList() );
		}

		default List<Column> getTargetColumns() {
			return getColumnMappings()
					.stream()
					.map( ColumnMapping::getTargetColumn )
					.collect( Collectors.toList() );
		}

		Column findReferringColumn(Column targetColumn);

		interface ColumnMapping {
			Column getReferringColumn();
			Column getTargetColumn();
		}
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ForeignKey that = (ForeignKey) o;
		return Objects.equals( name, that.name ) &&
				Objects.equals( referringTable, that.referringTable );
	}

	@Override
	public int hashCode() {

		return Objects.hash( name, referringTable );
	}

	public static class Builder {
		private final String name;
		private final boolean export;
		private final boolean referenceToPrimaryKey;
		private final boolean cascadeDeletes;
		private  final String explicitDefinition;
		private final Table referringTable;
		private final Table targetTable;

		private final List<ColumnMappings.ColumnMapping> columnMappings = new ArrayList<>();

		public Builder(
				String name,
				boolean export,
				boolean referenceToPrimaryKey,
				boolean cascadeDeletes, String explicitDefinition,
				Table referringTable,
				Table targetTable) {
			this.name = name;
			this.export = export;
			this.referenceToPrimaryKey = referenceToPrimaryKey;
			this.cascadeDeletes = cascadeDeletes;
			this.explicitDefinition = explicitDefinition;
			this.referringTable = referringTable;
			this.targetTable = targetTable;
		}

		public ForeignKey build() {
			final String name = StringHelper.isEmpty( this.name )
					? generateSyntheticName( referringTable, columnMappings, export )
					: this.name;

			return ( (InflightTable) referringTable ).createForeignKey(
					name,
					export,
					explicitDefinition,
					cascadeDeletes,
					referenceToPrimaryKey,
					targetTable,
					new ColumnMappingsImpl(
							referringTable,
							targetTable,
							columnMappings
					)
			);
		}

		private static String generateSyntheticName(Table referringTable, List<ColumnMappings.ColumnMapping> columnMappings, boolean export) {
			if ( ! export || ! ( referringTable instanceof PhysicalTable ) ) {
				return LocalObjectUuidHelper.generateLocalObjectUuid();
			}

			final StringBuilder sb = new StringBuilder( "table`" + ( (PhysicalTable) referringTable ).getTableName().getText() + "`" );

			// Ensure a consistent ordering of columns, regardless of the order
			// they were bound.
			// Clone the list, as sometimes a set of order-dependent Column
			// bindings are given.
			final Column[] alphabeticalColumns = columnMappings.stream()
					.map( ColumnMappings.ColumnMapping::getReferringColumn )
					.collect( GenericArrayCollector.forType( Column.class ) );

			Arrays.sort( alphabeticalColumns, Column.COLUMN_COMPARATOR );

			for ( Column column : alphabeticalColumns ) {
				String columnName = column == null ? "" : column.getExpression();
				sb.append( "column`" ).append( columnName ).append( "`" );
			}
			return "FK_" + hashedName( sb.toString() );
		}

		public void addColumnMapping(Column referringColumn, Column targetColumn) {
			assert referringColumn.getSourceTable() == referringTable;
			assert targetColumn.getSourceTable() == targetTable;

			columnMappings.add( new ColumnMappingImpl( referringColumn,targetColumn ) );
		}
	}
}
