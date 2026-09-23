/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.boot.model.naming.internal.ColumnNameHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.dialect.Dialect;


import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;

/**
 * A mapping model object representing a relational database {@linkplain jakarta.persistence.Table table}.
 *
 * @author Gavin King
 */
public abstract class Table extends ColumnContainer implements Contributable {
	private final String contributor;

	private PrimaryKey primaryKey;
	private final List<ForeignKeyEntry> foreignKeys = new ArrayList<>();
	private final Map<String,UniqueKey> uniqueKeys = new LinkedHashMap<>();
	private int uniqueInteger;
	private String rowId;
	private boolean isAbstract;
	private boolean hasDenormalizedTables;


	protected Table(String contributor) {
		this.contributor = contributor;
	}

	@Override
	public String getContributor() {
		return contributor;
	}

	public abstract String getTableExpression(SqlStringGenerationContext context);

	public String getQualifiedName(SqlStringGenerationContext context) {
		return getTableExpression( context );
	}

	/**
	 * @deprecated Should build a {@link QualifiedTableName}
	 * then use {@link SqlStringGenerationContext#format(QualifiedTableName)}.
	 */
	@Deprecated
	public static String qualify(String catalog, String schema, String table) {
		final var qualifiedName = new StringBuilder();
		if ( catalog != null ) {
			qualifiedName.append( catalog ).append( '.' );
		}
		if ( schema != null ) {
			qualifiedName.append( schema ).append( '.' );
		}
		return qualifiedName.append( table ).toString();
	}

	public abstract String getName();
	public abstract boolean isQuoted();
	public abstract String getSchema();
	public abstract boolean isSchemaQuoted();
	public abstract String getCatalog();
	public abstract boolean isCatalogQuoted();

	public String getQuotedName() { return quoted( getName(), isQuoted() ); }
	public String getQuotedName(Dialect dialect) { return render( getName(), isQuoted(), dialect ); }
	public String getQuotedSchema() { return quoted( getSchema(), isSchemaQuoted() ); }
	public String getQuotedSchema(Dialect dialect) { return render( getSchema(), isSchemaQuoted(), dialect ); }
	public String getQuotedCatalog() { return quoted( getCatalog(), isCatalogQuoted() ); }
	public String getQuotedCatalog(Dialect dialect) { return render( getCatalog(), isCatalogQuoted(), dialect ); }

	private static String quoted(String text, boolean quoted) {
		return text == null ? null : quoted ? '`' + text + '`' : text;
	}

	private static String render(String text, boolean quoted, Dialect dialect) {
		return text == null ? null : quoted ? dialect.openQuote() + text + dialect.closeQuote() : text;
	}

	@Internal
	public Column getColumn(InFlightMetadataCollector collector, String logicalName) {
		return logicalName == null ? null
				: getColumn( ColumnNameHelper.physicalName( collector.getPhysicalColumnName( this, logicalName ), collector.getDatabase() ) );
	}

	@Override
	public void addColumn(Column column) {
		if ( getColumn( column ) == null && primaryKey != null ) {
			for ( var primaryKeyColumn : primaryKey.getColumns() ) {
				if ( Objects.equals( column.getPhysicalName(), primaryKeyColumn.getPhysicalName() ) ) {
					column.setNullable( false );
				}
			}
		}
		super.addColumn( column );
	}


	@Incubating(since = "7.0")
	public Collection<ForeignKey> getForeignKeyCollection() {
		return foreignKeys.stream().map( ForeignKeyEntry::foreignKey ).toList();
	}

	public Collection<ForeignKey> getForeignKeys() {
		return foreignKeys.stream().map( ForeignKeyEntry::foreignKey ).toList();
	}

	java.util.Collection<UniqueKey> uniqueKeysForNameAttachment() {
		return uniqueKeys.values();
	}

	private boolean uniqueKeysFinalized;

	public boolean areUniqueKeysFinalized() { return uniqueKeysFinalized; }

	public void finalizeUniqueKeys(Map<String, UniqueKey> keys) {
		uniqueKeys.clear();
		uniqueKeys.putAll( keys );
		uniqueKeysFinalized = true;
	}

	public Map<String, UniqueKey> getUniqueKeys() {
		cleanseUniqueKeyMapIfNeeded();
		return unmodifiableMap( uniqueKeys );
	}

	private int sizeOfUniqueKeyMapOnLastCleanse;

	private void cleanseUniqueKeyMapIfNeeded() {
		if ( !uniqueKeysFinalized && uniqueKeys.size() != sizeOfUniqueKeyMapOnLastCleanse ) {
			cleanseUniqueKeyMap();
			sizeOfUniqueKeyMapOnLastCleanse = uniqueKeys.size();
		}
	}

	private void cleanseUniqueKeyMap() {
		// We need to account for a few conditions here...
		// 	1) If there are multiple unique keys contained in the uniqueKeys Map, we need to deduplicate
		// 		any sharing the same columns as other defined unique keys; this is needed for the annotation
		// 		processor since it creates unique constraints automagically for the user
		//	2) Remove any unique keys that share the same columns as the primary key; again, this is
		//		needed for the annotation processor to handle @Id @OneToOne cases.  In such cases we handle
		//		this case specifically because some databases fail if you try to apply a unique key to
		//		the primary key columns which causes schema export to fail in these cases. Furthermore, we
		//		pass the unique key to a primary key for reordering columns specified by the unique key.
		if ( !uniqueKeys.isEmpty() ) {
			if ( uniqueKeys.size() == 1 ) {
				// we have to worry about condition 2 above, but not condition 1
				final var uniqueKeyEntry = uniqueKeys.entrySet().iterator().next();
				if ( isSameAsPrimaryKeyColumns( uniqueKeyEntry.getValue() ) ) {
					primaryKey.setOrderingUniqueKey( uniqueKeyEntry.getValue() );
					uniqueKeys.remove( uniqueKeyEntry.getKey() );
				}
			}
			else {
				// we have to check both conditions 1 and 2
				//uniqueKeys.remove( uniqueKeyEntry.getKey() );
				uniqueKeys.entrySet().removeIf( entry -> isRedundantUniqueKey( entry.getValue() ) );
			}
		}
	}

	public boolean isRedundantUniqueKey(UniqueKey uniqueKey) {

		// Never remove explicit unique keys based on column matching
		if ( !uniqueKey.isExplicit() ) {
			// condition 1: check against other unique keys
			for ( var otherUniqueKey : uniqueKeys.values() ) {
				// make sure it's a different unique key
				if ( uniqueKey != otherUniqueKey
						&& otherUniqueKey.getColumns().containsAll( uniqueKey.getColumns() )
						&& uniqueKey.getColumns().containsAll( otherUniqueKey.getColumns() ) ) {
					return true;
				}
			}
		}

		// condition 2: check against the primary key
		if ( isSameAsPrimaryKeyColumns( uniqueKey ) ) {
			primaryKey.setOrderingUniqueKey( uniqueKey );
			return true;
		}

		return false;
	}

	private boolean isSameAsPrimaryKeyColumns(UniqueKey uniqueKey) {
		return primaryKey != null && !primaryKey.getColumns().isEmpty() // happens for many-to-many tables
			&& primaryKey.getColumns().size() == uniqueKey.getColumns().size()
			&& primaryKey.getColumns().containsAll( uniqueKey.getColumns() );
	}

	public boolean isPrimaryKey(Column column) {
		return hasPrimaryKey()
			&& getPrimaryKey().getColumnSpan() == 1
			&& getPrimaryKey().containsColumn( column );
	}

	public boolean hasPrimaryKey() {
		return getPrimaryKey() != null;
	}

	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(PrimaryKey primaryKey) {
		this.primaryKey = primaryKey;
		checkPrimaryKeyUniqueKey();
	}




	public UniqueKey addUniqueKey(UniqueKey uniqueKey) {
		final var current = uniqueKeys.get( uniqueKey.getName() );
		if ( current != null ) {
			throw new MappingException( "UniqueKey " + uniqueKey.getName() + " already exists" );
		}
		uniqueKeys.put( uniqueKey.getName(), uniqueKey );
		return uniqueKey;
	}

	public UniqueKey getUniqueKey(String keyName) {
		return uniqueKeys.get( keyName );
	}

	public UniqueKey getOrCreateUniqueKey(String keyName) {
		UniqueKey uniqueKey = uniqueKeys.get( keyName );
		if ( uniqueKey == null ) {
			uniqueKey = new UniqueKey( this );
			uniqueKey.setName( keyName );
			uniqueKeys.put( keyName, uniqueKey );
		}
		return uniqueKey;
	}

	public void markColumnUnique(String keyName, Column column) {
		column.setUniqueKeyName( keyName );
		column.setUnique( true );
	}

	public ForeignKey createForeignKey(
			String keyName,
			List<Column> keyColumns,
			String referencedEntityName,
			String keyDefinition,
			String options,
			List<Column> referencedColumns) {
		return createForeignKey(
				keyName,
				new ForeignKeyColumnMappings( toForeignKeyColumnMappings( keyColumns, referencedColumns ) ),
				referencedEntityName,
				keyDefinition,
				options
		);
	}

	public ForeignKey createForeignKey(
			String keyName,
			ForeignKeyColumnMappings columnMappings,
			String referencedEntityName,
			String keyDefinition,
			String options) {
		final var key = new ForeignKeyKey( columnMappings.mappings(), referencedEntityName );

		ForeignKey foreignKey = foreignKeyIndex().get( key );
		if ( foreignKey == null ) {
			foreignKey = new ForeignKey( this );
			foreignKey.setReferencedEntityName( referencedEntityName );
			foreignKey.setKeyDefinition( keyDefinition );
			foreignKey.setOptions( options );
			for ( var columnMapping : columnMappings.mappings() ) {
				foreignKey.addColumn( columnMapping.column() );
			}

			// null referenced columns mean a reference to the primary key
			final List<Column> referencedColumns = referencedColumns( columnMappings.mappings() );
			if ( !referencedColumns.isEmpty() ) {
				foreignKey.addReferencedColumns( referencedColumns );
			}

			// NOTE: if the name is null, we will generate an implicit name during second pass processing
			//       after we know the referenced table name (which might not be resolved yet).
			foreignKey.setName( keyName );

			foreignKeys.add( new ForeignKeyEntry( key, foreignKey ) );
		}

		if ( keyName != null ) {
			foreignKey.setName( keyName );
		}

		return foreignKey;
	}

	private List<ForeignKeyColumnMapping> toForeignKeyColumnMappings(
			List<Column> keyColumns,
			List<Column> referencedColumns) {
		if ( referencedColumns == null ) {
			return keyColumns.stream()
					.map( (keyColumn) -> new ForeignKeyColumnMapping( keyColumn, null ) )
					.toList();
		}
		if ( keyColumns.size() != referencedColumns.size() ) {
			throw new MappingException(
					"Foreign key column count did not match referenced column count for table " + getName()
							+ " - key columns: " + keyColumns
							+ ", referenced columns: " + referencedColumns
			);
		}
		final ArrayList<ForeignKeyColumnMapping> result = new ArrayList<>( keyColumns.size() );
		for ( int i = 0; i < keyColumns.size(); i++ ) {
			result.add( new ForeignKeyColumnMapping( keyColumns.get( i ), referencedColumns.get( i ) ) );
		}
		return result;
	}

	private List<Column> referencedColumns(List<ForeignKeyColumnMapping> columnMappings) {
		if ( columnMappings.stream().noneMatch( (columnMapping) -> columnMapping.referencedColumn() != null ) ) {
			return emptyList();
		}
		if ( columnMappings.stream().anyMatch( (columnMapping) -> columnMapping.referencedColumn() == null ) ) {
			throw new MappingException(
					"Foreign key column mappings for table " + getName()
							+ " must either all reference primary-key columns or all reference explicit columns"
			);
		}
		return columnMappings.stream()
				.map( ForeignKeyColumnMapping::referencedColumn )
				.toList();
	}

	/**
	 * Checks for unique key containing only whole primary key and sets
	 * order of the columns accordingly
	 */
	private void checkPrimaryKeyUniqueKey() {
		final var uniqueKeyEntries = uniqueKeys.entrySet().iterator();
		while ( uniqueKeyEntries.hasNext() ) {
			final var uniqueKeyEntry = uniqueKeyEntries.next();
			final var uniqueKey = uniqueKeyEntry.getValue();
			if ( isSameAsPrimaryKeyColumns( uniqueKey ) ) {
				primaryKey.setOrderingUniqueKey( uniqueKey );
				uniqueKeyEntries.remove();
			}
		}
	}

	// This must be done outside of Table, rather than statically, to ensure
	// deterministic alias names.  See HHH-2448.
	public void setUniqueInteger( int uniqueInteger ) {
		this.uniqueInteger = uniqueInteger;
	}

	public int getUniqueInteger() {
		return uniqueInteger;
	}


	public String getRowId() {
		return rowId;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	public String toString() {
		final var string = new StringBuilder()
				.append( getClass().getSimpleName() )
				.append( '(' );
		if ( getCatalog() != null ) {
			string.append( getCatalog() ).append( "." );
		}
		if ( getSchema() != null ) {
			string.append( getSchema() ).append( "." );
		}
		string.append( getName() ).append( ')' );
		return string.toString();
	}

	public String getSubselect() {
		return null;
	}


	public boolean isSubselect() {
		return this instanceof InlineView;
	}

	public boolean isAbstractUnionTable() {
		return hasDenormalizedTables() && isAbstract;
	}

	public boolean hasDenormalizedTables() {
		return hasDenormalizedTables;
	}

	void setHasDenormalizedTables() {
		hasDenormalizedTables = true;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public boolean isPhysicalTable() {
		return !isSubselect() && !isAbstractUnionTable();
	}

	public boolean isView() {
		return this instanceof DatabaseView;
	}






	public String getViewQuery() {
		return null;
	}


	void validateColumnRename(Column column, org.hibernate.relational.naming.spi.PhysicalName replacement) {
		final var keys = new java.util.HashSet<List<Object>>();
		for ( var entry : foreignKeys ) {
			final List<Object> parts = new ArrayList<>();
			parts.add( entry.key.referencedClassName );
			for ( var mapping : entry.key.columnMappings ) {
				parts.add( mapping.column() == column ? replacement : mapping.column().getPhysicalName() );
				parts.add( mapping.referencedColumn() == null ? null
						: mapping.referencedColumn() == column ? replacement : mapping.referencedColumn().getPhysicalName() );
			}
			if ( !keys.add( parts ) ) {
				throw new MappingException( "Column rename collides in foreign keys of " + getName() );
			}
		}
	}

	void validateForeignKeyIndex() { foreignKeyIndex(); }

	void visitForeignKeyColumns(java.util.function.Consumer<Column> consumer) {
		foreignKeys.forEach( entry -> {
			for ( var mapping : entry.key.columnMappings ) {
				consumer.accept( mapping.column() );
				consumer.accept( mapping.referencedColumn() );
			}
		} );
	}

	private Map<ForeignKeyKey, ForeignKey> foreignKeyIndex() {
		final Map<ForeignKeyKey, ForeignKey> index = new LinkedHashMap<>();
		for ( var entry : foreignKeys ) {
			if ( index.putIfAbsent( entry.key, entry.foreignKey ) != null ) {
				throw new MappingException( "Physical column collision in foreign keys of " + getName() );
			}
		}
		return index;
	}

	private record ForeignKeyEntry(ForeignKeyKey key, ForeignKey foreignKey) implements Serializable {}

	private record ForeignKeyKey(ForeignKeyColumnMapping[] columnMappings, String referencedClassName)
			implements Serializable {
		private ForeignKeyKey {
			Objects.requireNonNull( columnMappings );
			Objects.requireNonNull( referencedClassName );
		}

		private ForeignKeyKey(List<ForeignKeyColumnMapping> columnMappings, String referencedClassName) {
			this( columnMappings.toArray( ForeignKeyColumnMapping[]::new ), referencedClassName );
		}

		public int hashCode() {
			return Objects.hash( referencedClassName, Arrays.hashCode( columnMappings ) );
		}

		public boolean equals(Object other) {
			return other instanceof ForeignKeyKey foreignKeyKey
				&& Objects.equals( foreignKeyKey.referencedClassName, referencedClassName )
				&& Arrays.equals( foreignKeyKey.columnMappings, columnMappings );
		}
	}













}
