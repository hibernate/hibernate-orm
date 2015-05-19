/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.ConstraintSource;
import org.hibernate.boot.model.source.spi.DerivedValueSource;
import org.hibernate.boot.model.source.spi.IndexConstraintSource;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.UniqueKeyConstraintSource;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/**
 * Centralized binding of columns and formulas.
 *
 * @author Steve Ebersole
 */
public class RelationalObjectBinder {
	private final Database database;
	private final PhysicalNamingStrategy physicalNamingStrategy;

	public static interface ColumnNamingDelegate {
		// todo : use ImplicitNamingStrategy / PhysicalNamingStrategy
		// todo : leverage Identifier class
		public Identifier determineImplicitName(LocalMetadataBuildingContext context);
	}

	public RelationalObjectBinder(MetadataBuildingContext buildingContext) {
		this.database = buildingContext.getMetadataCollector().getDatabase();
		this.physicalNamingStrategy = buildingContext.getBuildingOptions().getPhysicalNamingStrategy();
	}

	public void bindColumnOrFormula(
			MappingDocument sourceDocument,
			RelationalValueSource relationalValueSource,
			SimpleValue simpleValue,
			boolean areColumnsNullableByDefault,
			ColumnNamingDelegate columnNamingDelegate) {
		bindColumnsAndFormulas(
				sourceDocument,
				Collections.singletonList( relationalValueSource ),
				simpleValue,
				areColumnsNullableByDefault,
				columnNamingDelegate
		);
	}

	public void bindColumns(
			MappingDocument sourceDocument,
			List<ColumnSource> columnSources,
			SimpleValue simpleValue,
			boolean areColumnsNullableByDefault,
			ColumnNamingDelegate columnNamingDelegate) {
		for ( ColumnSource columnSource : columnSources ) {
			bindColumn(
					sourceDocument,
					columnSource,
					simpleValue,
					areColumnsNullableByDefault,
					columnNamingDelegate
			);
		}
	}

	public void bindColumnsAndFormulas(
			MappingDocument sourceDocument,
			List<RelationalValueSource> relationalValueSources,
			SimpleValue simpleValue,
			boolean areColumnsNullableByDefault,
			ColumnNamingDelegate columnNamingDelegate) {
		for ( RelationalValueSource relationalValueSource : relationalValueSources ) {
			if ( ColumnSource.class.isInstance( relationalValueSource ) ) {
				final ColumnSource columnSource = (ColumnSource) relationalValueSource;
				bindColumn(
						sourceDocument,
						columnSource,
						simpleValue,
						areColumnsNullableByDefault,
						columnNamingDelegate
				);
			}
			else {
				final DerivedValueSource formulaSource = (DerivedValueSource) relationalValueSource;
				simpleValue.addFormula( new Formula( formulaSource.getExpression() ) );
			}
		}
	}

	public void bindColumn(
			MappingDocument sourceDocument,
			ColumnSource columnSource,
			SimpleValue simpleValue,
			boolean areColumnsNullableByDefault,
			ColumnNamingDelegate columnNamingDelegate) {
		Table table = simpleValue.getTable();

		final Column column = new Column();
		column.setValue( simpleValue );

		// resolve column name
		final Identifier logicalName;
		if ( StringHelper.isNotEmpty( columnSource.getName() ) ) {
			logicalName = database.toIdentifier( columnSource.getName() );
		}
		else {
			logicalName = columnNamingDelegate.determineImplicitName( sourceDocument );
		}

		final Identifier physicalName = physicalNamingStrategy.toPhysicalColumnName(
				logicalName,
				database.getJdbcEnvironment()
		);
		column.setName( physicalName.render( database.getDialect() ) );

		if ( table != null ) {
			table.addColumn( column );
			sourceDocument.getMetadataCollector().addColumnNameBinding(
					table,
					logicalName,
					column
			);
		}

		if ( columnSource.getSizeSource() != null ) {
			// UGH!
			// This is the purpose of the default sizes and nullness here on metamodel dev branch.
			//
			// But here, for now, this needs to continue; although another option is "magic numbers" (e.g., -1)
			// to indicate null
			if ( columnSource.getSizeSource().getLength() != null ) {
				column.setLength( columnSource.getSizeSource().getLength() );
			}
			else {
				column.setLength( Column.DEFAULT_LENGTH );
			}

			if ( columnSource.getSizeSource().getScale() != null ) {
				column.setScale( columnSource.getSizeSource().getScale() );
			}
			else {
				column.setScale( Column.DEFAULT_SCALE );
			}

			if ( columnSource.getSizeSource().getPrecision() != null ) {
				column.setPrecision( columnSource.getSizeSource().getPrecision() );
			}
			else {
				column.setPrecision( Column.DEFAULT_PRECISION );
			}
		}

		column.setNullable( interpretNullability( columnSource.isNullable(), areColumnsNullableByDefault ) );

		column.setUnique( columnSource.isUnique() );

		column.setCheckConstraint( columnSource.getCheckCondition() );
		column.setDefaultValue( columnSource.getDefaultValue() );
		column.setSqlType( columnSource.getSqlType() );

		column.setComment( columnSource.getComment() );

		column.setCustomRead( columnSource.getReadFragment() );
		column.setCustomWrite( columnSource.getWriteFragment() );

		simpleValue.addColumn( column );
	}

	private static boolean interpretNullability(TruthValue nullable, boolean areColumnsNullableByDefault) {
		if ( nullable == null || nullable == TruthValue.UNKNOWN ) {
			return areColumnsNullableByDefault;
		}
		else {
			return nullable == TruthValue.TRUE;
		}
	}

	public void bindFormulas(
			MappingDocument sourceDocument,
			List<DerivedValueSource> formulaSources,
			OneToOne oneToOneBinding) {
		for ( DerivedValueSource formulaSource : formulaSources ) {
			oneToOneBinding.addFormula( new Formula( formulaSource.getExpression() ) );
		}
	}

	public static interface RelationalObjectResolutionContext {
		/**
		 * Resolve a Table, given its logical name.
		 * <p/>
		 * NOTE : not sure yet how to best implement this.  One option is to simply
		 * construct the proper key needed to resolve the reference from the
		 * "metadataCollector".  Another option is to have this context provide a
		 * "scoped table resolution", local to the point where the table reference
		 * occurred; for example, an entity might act as such a scope and resolution of
		 * table references would occur locally scoped to just tables defined for the
		 * entity.
		 * <p/>
		 * todo : does "logical name" include catalog/schema?
		 *
		 * @param metadataCollector
		 * @param logicalName
		 *
		 * @return
		 */
		Table resolveTable(InFlightMetadataCollector metadataCollector, String logicalName);

		Column resolveColumn(InFlightMetadataCollector metadataCollector, Table table, String logicalName);
	}

	public static void bindConstraints(
			MappingDocument mappingDocument,
			RelationalObjectResolutionContext resolutionContext,
			Collection<ConstraintSource> constraintSources) {
		for ( ConstraintSource constraintSource : constraintSources ) {
			if ( IndexConstraintSource.class.isInstance( constraintSource ) ) {
				bindIndexConstraint(
						mappingDocument,
						resolutionContext,
						(IndexConstraintSource) constraintSource
				);
			}
			else if ( UniqueKeyConstraintSource.class.isInstance( constraintSource )) {
				bindUniqueKeyConstraint(
						mappingDocument,
						resolutionContext,
						(UniqueKeyConstraintSource) constraintSource
				);
			}
		}

	}

	private static void bindIndexConstraint(
			MappingDocument mappingDocument,
			RelationalObjectResolutionContext resolutionContext,
			IndexConstraintSource constraintSource) {
		// 1) resolve table
		final Table table = resolutionContext.resolveTable(
				mappingDocument.getMetadataCollector(),
				constraintSource.getTableName()
		);

		// 2) resolve Index
		final Index index = table.getOrCreateIndex( constraintSource.name() );

		// 3) add columns
		for ( String columnName : constraintSource.columnNames() ) {
			// 3.a) resolve Column reference
			final Column column = resolutionContext.resolveColumn( mappingDocument.getMetadataCollector(), table, columnName );
			// 3.b) add it to the Index
			index.addColumn( column );
		}
	}

	private static void bindUniqueKeyConstraint(
			MappingDocument mappingDocument,
			RelationalObjectResolutionContext resolutionContext,
			UniqueKeyConstraintSource constraintSource) {
		// 1) resolve table
		final Table table = resolutionContext.resolveTable(
				mappingDocument.getMetadataCollector(),
				constraintSource.getTableName()
		);

		// 2) resolve UniqueKey
		final UniqueKey uniqueKey = table.getOrCreateUniqueKey( constraintSource.name() );

		// 3) add columns
		for ( String columnName : constraintSource.columnNames() ) {
			// 3.a) resolve Column reference
			final Column column = resolutionContext.resolveColumn( mappingDocument.getMetadataCollector(), table, columnName );
			// 3.b) add it to the UniqueKey
			uniqueKey.addColumn( column );
		}
	}
}
