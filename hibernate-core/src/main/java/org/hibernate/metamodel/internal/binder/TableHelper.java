/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.binder;

import java.util.Collections;
import java.util.List;

import org.hibernate.TruthValue;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.internal.binder.ConstraintNamingStrategyHelper.IndexNamingStrategyHelper;
import org.hibernate.metamodel.internal.binder.ConstraintNamingStrategyHelper.UniqueKeyNamingStrategyHelper;
import org.hibernate.metamodel.source.spi.ColumnSource;
import org.hibernate.metamodel.source.spi.InLineViewSource;
import org.hibernate.metamodel.source.spi.MappingDefaults;
import org.hibernate.metamodel.source.spi.SizeSource;
import org.hibernate.metamodel.source.spi.TableSource;
import org.hibernate.metamodel.source.spi.TableSpecificationSource;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Index;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.UniqueKey;

import org.jboss.logging.Logger;

/**
 * @author Gail Badner
 */
public class TableHelper {
	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TableHelper.class.getName()
	);

	private final BinderRootContext helperContext;

	public TableHelper(BinderRootContext helperContext) {
		this.helperContext = helperContext;
	}

	public TableSpecification createTable(
			final TableSpecificationSource tableSpecSource,
			final ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper) {
		return createTable( tableSpecSource, namingStrategyHelper, null );
	}

	public TableSpecification createTable(
			final TableSpecificationSource tableSpecSource,
			final ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper,
			final Table includedTable) {
		if ( tableSpecSource == null && namingStrategyHelper == null ) {
			throw bindingContext().makeMappingException(
					"An explicit name must be specified for the table"
			);
		}
		final boolean isTableSourceNull = tableSpecSource == null;
		final Schema schema = resolveSchema( tableSpecSource );

		TableSpecification tableSpec;
		if ( isTableSourceNull || tableSpecSource instanceof TableSource ) {
			String explicitName = isTableSourceNull ? null : TableSource.class.cast( tableSpecSource ).getExplicitTableName();
			String tableName = normalizeDatabaseIdentifier( explicitName, namingStrategyHelper );
			String logicTableName = TableNamingStrategyHelper.class.cast( namingStrategyHelper ).getLogicalName(
					bindingContext().getBuildingOptions().getNamingStrategy()
			);
			String rowId = isTableSourceNull ? null : TableSource.class.cast( tableSpecSource ).getRowId();
			tableSpec = createTableSpecification(
					schema,
					tableName,
					logicTableName,
					includedTable,
					rowId
			);
		}
		else {
			final InLineViewSource inLineViewSource = (InLineViewSource) tableSpecSource;
			tableSpec = schema.createInLineView(
					createIdentifier( inLineViewSource.getLogicalName() ),
					inLineViewSource.getSelectStatement()
			);
		}
		return tableSpec;
	}

	public Schema resolveSchema(final TableSpecificationSource tableSpecSource) {
		final boolean tableSourceNull = tableSpecSource == null;
		final MappingDefaults mappingDefaults = bindingContext().getMappingDefaults();
		final String explicitCatalogName = tableSourceNull ? null : tableSpecSource.getExplicitCatalogName();
		final String explicitSchemaName = tableSourceNull ? null : tableSpecSource.getExplicitSchemaName();
		final Schema.Name schemaName =
				new Schema.Name(
						createIdentifier( explicitCatalogName, mappingDefaults.getCatalogName() ),
						createIdentifier( explicitSchemaName, mappingDefaults.getSchemaName() )
				);
		return bindingContext().getMetadataCollector().getDatabase().locateSchema( schemaName );
	}

	private TableSpecification createTableSpecification(
			final Schema schema,
			final String tableName,
			final String logicTableName,
			final Table includedTable,
			String rowId) {
		final Identifier logicalTableId = createIdentifier( logicTableName );
		final Identifier physicalTableId = createIdentifier( tableName );
		final Table table = schema.locateTable( logicalTableId );
		if ( table != null ) {
			return table;
		}
		Table tableSpec;
		if ( includedTable == null ) {
			tableSpec = schema.createTable( logicalTableId, physicalTableId );
		}
		else {
			tableSpec = schema.createDenormalizedTable( logicalTableId, physicalTableId, includedTable );
		}

		tableSpec.setRowId( rowId );

		return tableSpec;
	}

	public Column locateOrCreateColumn(
			final TableSpecification table,
			final String columnName,
			final ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper) {
		if ( columnName == null && namingStrategyHelper == null ) {
			throw bindingContext().makeMappingException(
					"Cannot resolve name for column because no name was specified and namingStrategyHelper is null."
			);
		}
		final String resolvedColumnName = normalizeDatabaseIdentifier(
				columnName,
				namingStrategyHelper
		);
		return table.locateOrCreateColumn( resolvedColumnName );
	}

	public Column locateColumn(
			final TableSpecification table,
			final String columnName,
			final ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper) {
		if ( columnName == null && namingStrategyHelper == null ) {
			throw bindingContext().makeMappingException(
					"Cannot resolve name for column because no name was specified and namingStrategyHelper is null."
			);
		}
		final String resolvedColumnName = normalizeDatabaseIdentifier(
				columnName,
				namingStrategyHelper
		);
		return table.locateColumn( resolvedColumnName );
	}

	public Column locateOrCreateColumn(
			final TableSpecification table,
			final ColumnSource columnSource,
			final ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper,
			final boolean forceNotNull,
			final boolean isNullableByDefault) {
		final Column column = locateOrCreateColumn( table, columnSource.getName(), namingStrategyHelper );
		resolveColumnNullable( table, columnSource, forceNotNull, isNullableByDefault, column );
		column.setDefaultValue( columnSource.getDefaultValue() );
		column.setSqlType( columnSource.getSqlType() );
		if ( columnSource.getSizeSource() != null ) {
			final SizeSource sizeSource = columnSource.getSizeSource();
			if ( sizeSource.isLengthDefined() ) {
				column.getSize().setLength( sizeSource.getLength() );
			}
			if ( sizeSource.isPrecisionDefined() ) {
				column.getSize().setPrecision( sizeSource.getPrecision() );
			}
			if ( sizeSource.isScaleDefined() ) {
				column.getSize().setScale( sizeSource.getScale() );
			}
		}
		column.setJdbcDataType( columnSource.getDatatype() );
		column.setReadFragment( columnSource.getReadFragment() );
		column.setWriteFragment( columnSource.getWriteFragment() );
		column.setCheckCondition( columnSource.getCheckCondition() );
		column.setComment( columnSource.getComment() );

		if (columnSource.isUnique()) {
			createUniqueKey( table, Collections.singletonList( column ), null );
		}
		return column;
	}
	
	public void createUniqueKey(
			final TableSpecification table,
			final List<Column> columns,
			final String name) {
		final UniqueKey uk = new UniqueKey();
		for ( final Column column : columns ) {
			uk.addColumn( column );
		}
		uk.setTable( table );
		
		final String normalizedName = normalizeDatabaseIdentifier( name, new UniqueKeyNamingStrategyHelper( table, columns ) );
		
		uk.setName( Identifier.toIdentifier( normalizedName ) );
		table.addUniqueKey( uk );
	}
	
	public void createIndex(
			final TableSpecification table,
			final List<Column> columns,
			final String name,
			final boolean isUnique) {
		final Index idx = new Index(isUnique);
		for ( final Column column : columns ) {
			idx.addColumn( column );
		}
		idx.setTable( table );
		
		final String normalizedName = normalizeDatabaseIdentifier( name, new IndexNamingStrategyHelper( table, columns ) );
		
		idx.setName( Identifier.toIdentifier( normalizedName ) );
		table.addIndex( idx );
	}

	private void resolveColumnNullable(
			final TableSpecification table,
			final ColumnSource columnSource,
			final boolean forceNotNull,
			final boolean isNullableByDefault,
			final Column column) {
		if ( forceNotNull ) {
			column.setNullable( false );
			if ( columnSource.isNullable() == TruthValue.TRUE ) {
				log.warnf(
						"Natural Id column[%s] from table[%s] has explicit set to allow nullable, we have to make it force not null ",
						columnSource.getName(),
						table.getLogicalName().getText()
				);
			}
		}
		else {
			// if the column is already non-nullable, leave it alone
			if ( column.isNullable() ) {
				column.setNullable( TruthValue.toBoolean( columnSource.isNullable(), isNullableByDefault ) );
			}
		}
	}

	private Identifier createIdentifier(String name) {
		return helperContext.relationalIdentifierHelper().createIdentifier( name );
	}

	private Identifier createIdentifier(String name, String defaultName) {
		return helperContext.relationalIdentifierHelper().createIdentifier( name, defaultName );
	}

	private String normalizeDatabaseIdentifier(String explicitName, ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper) {
		return helperContext.relationalIdentifierHelper().normalizeDatabaseIdentifier( explicitName, namingStrategyHelper );
	}

	private BinderLocalBindingContext bindingContext() {
		return helperContext.getLocalBindingContextSelector().getCurrentBinderLocalBindingContext();
	}
}
