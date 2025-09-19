/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.model.relational.ContributableDatabaseObject;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;

import org.jboss.logging.Logger;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;

/**
 * A mapping model object representing a relational database {@linkplain jakarta.persistence.Table table}.
 *
 * @author Gavin King
 */
public class Table implements Serializable, ContributableDatabaseObject {
	private static final Logger LOG = Logger.getLogger( Table.class );
	private static final Column[] EMPTY_COLUMN_ARRAY = new Column[0];

	private final String contributor;

	private Identifier catalog;
	private Identifier schema;
	private Identifier name;

	/**
	 * contains all columns, including the primary key
	 */
	private final Map<String, Column> columns = new LinkedHashMap<>();
	private PrimaryKey primaryKey;
	private final Map<ForeignKeyKey, ForeignKey> foreignKeys = new LinkedHashMap<>();
	private final Map<String, Index> indexes = new LinkedHashMap<>();
	private final Map<String,UniqueKey> uniqueKeys = new LinkedHashMap<>();
	private int uniqueInteger;
	private final List<CheckConstraint> checkConstraints = new ArrayList<>();
	private String rowId;
	private String subselect;
	private boolean isAbstract;
	private boolean hasDenormalizedTables;
	private String comment;
	private String viewQuery;
	private String options;

	private List<Function<SqlStringGenerationContext, InitCommand>> initCommandProducers;

	@Deprecated(since="6.2", forRemoval = true)
	public Table() {
		this( "orm" );
	}

	public Table(String contributor) {
		this( contributor, null );
	}

	public Table(String contributor, String name) {
		this.contributor = contributor;
		setName( name );
	}

	public Table(
			String contributor,
			Namespace namespace,
			Identifier physicalTableName,
			boolean isAbstract) {
		this.contributor = contributor;
		this.catalog = namespace.getPhysicalName().catalog();
		this.schema = namespace.getPhysicalName().schema();
		this.name = physicalTableName;
		this.isAbstract = isAbstract;
	}

	public Table(
			String contributor,
			Namespace namespace,
			Identifier physicalTableName,
			String subselect,
			boolean isAbstract) {
		this.contributor = contributor;
		this.catalog = namespace.getPhysicalName().catalog();
		this.schema = namespace.getPhysicalName().schema();
		this.name = physicalTableName;
		this.subselect = subselect;
		this.isAbstract = isAbstract;
	}

	public Table(String contributor, Namespace namespace, String subselect, boolean isAbstract) {
		this.contributor = contributor;
		this.catalog = namespace.getPhysicalName().catalog();
		this.schema = namespace.getPhysicalName().schema();
		this.subselect = subselect;
		this.isAbstract = isAbstract;
	}

	@Override
	public String getContributor() {
		return contributor;
	}

	public String getQualifiedName(SqlStringGenerationContext context) {
		return subselect != null
				? "( " + subselect + " )"
				: context.format( new QualifiedTableName( catalog, schema, name ) );
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

	public void setName(String name) {
		this.name = toIdentifier( name );
	}

	public String getName() {
		return name == null ? null : name.getText();
	}

	public Identifier getNameIdentifier() {
		return name;
	}

	public Identifier getSchemaIdentifier() {
		return schema;
	}

	public Identifier getCatalogIdentifier() {
		return catalog;
	}

	public String getQuotedName() {
		return name == null ? null : name.toString();
	}

	public String getQuotedName(Dialect dialect) {
		return name == null ? null : name.render( dialect );
	}

	public QualifiedTableName getQualifiedTableName() {
		return name == null ? null : new QualifiedTableName( catalog, schema, name );
	}

	public boolean isQuoted() {
		return name.isQuoted();
	}

	public void setQuoted(boolean quoted) {
		if ( quoted != name.isQuoted() ) {
			name = new Identifier( name.getText(), quoted );
		}
	}

	public void setSchema(String schema) {
		this.schema = toIdentifier( schema );
	}

	public String getSchema() {
		return schema == null ? null : schema.getText();
	}

	public String getQuotedSchema() {
		return schema == null ? null : schema.toString();
	}

	public String getQuotedSchema(Dialect dialect) {
		return schema == null ? null : schema.render( dialect );
	}

	public boolean isSchemaQuoted() {
		return schema != null && schema.isQuoted();
	}

	public void setCatalog(String catalog) {
		this.catalog = toIdentifier( catalog );
	}

	public String getCatalog() {
		return catalog == null ? null : catalog.getText();
	}

	public String getQuotedCatalog() {
		return catalog == null ? null : catalog.render();
	}

	public String getQuotedCatalog(Dialect dialect) {
		return catalog == null ? null : catalog.render( dialect );
	}

	public boolean isCatalogQuoted() {
		return catalog != null && catalog.isQuoted();
	}

	/**
	 * Return the column which is identified by column provided as argument.
	 *
	 * @param column column with at least a name.
	 * @return the underlying column or null if not inside this table.
	 *         Note: the instance *can* be different than the input parameter,
	 *         but the name will be the same.
	 */
	public Column getColumn(Column column) {
		if ( column == null ) {
			return null;
		}
		else {
			final Column existing = columns.get( column.getCanonicalName() );
			return column.equals( existing ) ? existing : null;
		}
	}

	public Column getColumn(Identifier name) {
		return name == null ? null
				: columns.get( name.getCanonicalName() );
	}

	@Internal
	public Column getColumn(InFlightMetadataCollector collector, String logicalName) {
		return name == null ? null
				: getColumn( new Column( collector.getPhysicalColumnName( this, logicalName ) ) );
	}

	public Column getColumn(int n) {
		final var iter = columns.values().iterator();
		for ( int i = 0; i < n - 1; i++ ) {
			iter.next();
		}
		return iter.next();
	}

	public void addColumn(Column column) {
		final var oldColumn = getColumn( column );
		if ( oldColumn == null ) {
			if ( primaryKey != null ) {
				for ( var primaryKeyColumn : primaryKey.getColumns() ) {
					if ( primaryKeyColumn.getCanonicalName().equals( column.getCanonicalName() ) ) {
						column.setNullable( false );
						if ( LOG.isTraceEnabled() ) {
							LOG.tracef(
									"Forcing column [%s] to be non-null as it is part of the primary key for table [%s]",
									column.getCanonicalName(),
									getNameIdentifier().getCanonicalName()
							);
						}
					}
				}
			}
			columns.put( column.getCanonicalName(), column );
			column.uniqueInteger = columns.size();
		}
		else {
			column.uniqueInteger = oldColumn.uniqueInteger;
		}
	}

	@Internal
	public void columnRenamed(Column column) {
		for ( var entry : columns.entrySet() ) {
			if ( entry.getValue() == column ) {
				columns.remove( entry.getKey() );
				columns.put( column.getCanonicalName(), column );
				break;
			}
		}
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Collection<Column> getColumns() {
		return columns.values();
	}

	public Map<String, Index> getIndexes() {
		return unmodifiableMap( indexes );
	}

	@Incubating
	public Collection<ForeignKey> getForeignKeyCollection() {
		return unmodifiableCollection( foreignKeys.values() );
	}

	/**
	 * @deprecated because {@link ForeignKeyKey} should be private.
	 */
	@Deprecated(since = "7", forRemoval = true)
	public Map<ForeignKeyKey, ForeignKey> getForeignKeys() {
		return unmodifiableMap( foreignKeys );
	}

	public Map<String, UniqueKey> getUniqueKeys() {
		cleanseUniqueKeyMapIfNeeded();
		return unmodifiableMap( uniqueKeys );
	}

	private int sizeOfUniqueKeyMapOnLastCleanse;

	private void cleanseUniqueKeyMapIfNeeded() {
		if ( uniqueKeys.size() != sizeOfUniqueKeyMapOnLastCleanse ) {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (catalog == null ? 0 : catalog.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + (schema == null ? 0 : schema.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof Table table
			&& equals( table );
	}

	public boolean equals(Table table) {
		if ( null == table ) {
			return false;
		}
		else if ( this == table ) {
			return true;
		}
		else {
			return Identifier.areEqual( name, table.name )
				&& Identifier.areEqual( schema, table.schema )
				&& Identifier.areEqual( catalog, table.catalog );
		}
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

	public Index getOrCreateIndex(String indexName) {
		Index index =  indexes.get( indexName );
		if ( index == null ) {
			index = new Index();
			index.setName( indexName );
			index.setTable( this );
			indexes.put( indexName, index );
		}
		return index;
	}

	public Index getIndex(String indexName) {
		return  indexes.get( indexName );
	}

	public Index addIndex(Index index) {
		final var current =  indexes.get( index.getName() );
		if ( current != null ) {
			throw new MappingException( "Index " + index.getName() + " already exists" );
		}
		indexes.put( index.getName(), index );
		return index;
	}

	public UniqueKey addUniqueKey(UniqueKey uniqueKey) {
		final var current = uniqueKeys.get( uniqueKey.getName() );
		if ( current != null ) {
			throw new MappingException( "UniqueKey " + uniqueKey.getName() + " already exists" );
		}
		uniqueKeys.put( uniqueKey.getName(), uniqueKey );
		return uniqueKey;
	}

	/**
	 * Mark the given column unique and assign a name to the unique key.
	 * <p>
	 * This method does not add a {@link UniqueKey} to the table itself!
	 */
	public void createUniqueKey(Column column, MetadataBuildingContext context) {
		final String keyName = context.getBuildingOptions().getImplicitNamingStrategy()
				.determineUniqueKeyName( new ImplicitUniqueKeyNameSource() {
					@Override
					public Identifier getTableName() {
						return name;
					}

					@Override
					public List<Identifier> getColumnNames() {
						return singletonList( column.getNameIdentifier( context ) );
					}

					@Override
					public Identifier getUserProvidedIdentifier() {
						return null;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return context;
					}
				} )
				.render( context.getMetadataCollector().getDatabase().getDialect() );
		column.setUniqueKeyName( keyName );
		column.setUnique( true );
	}

	/**
	 * If there is one given column, mark it unique, otherwise
	 * create a {@link UniqueKey} comprising the given columns.
	 */
	public void createUniqueKey(List<Column> keyColumns, MetadataBuildingContext context) {
		if ( keyColumns.size() == 1 ) {
			createUniqueKey( keyColumns.get(0), context );
		}
		else {
			final String keyName = context.getBuildingOptions().getImplicitNamingStrategy()
					.determineUniqueKeyName( new ImplicitUniqueKeyNameSource() {
						@Override
						public Identifier getTableName() {
							return name;
						}

						@Override
						public List<Identifier> getColumnNames() {
							return keyColumns.stream()
									.map( column -> column.getNameIdentifier( context ) )
									.collect(toList());
						}

						@Override
						public Identifier getUserProvidedIdentifier() {
							return null;
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return context;
						}
					} )
					.render( context.getMetadataCollector().getDatabase().getDialect() );
			final var uniqueKey = getOrCreateUniqueKey( keyName );
			for ( var keyColumn : keyColumns ) {
				uniqueKey.addColumn( keyColumn );
			}
		}
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

	public void createForeignKeys(MetadataBuildingContext context) {
	}

	@Deprecated(since="7.0", forRemoval = true)
	public ForeignKey createForeignKey(String keyName, List<Column> keyColumns, String referencedEntityName, String keyDefinition) {
		return createForeignKey( keyName, keyColumns, referencedEntityName, keyDefinition, null, null );
	}

	public ForeignKey createForeignKey(String keyName, List<Column> keyColumns, String referencedEntityName, String keyDefinition, String options) {
		return createForeignKey( keyName, keyColumns, referencedEntityName, keyDefinition, options, null );
	}

	public ForeignKey createForeignKey(
			String keyName,
			List<Column> keyColumns,
			String referencedEntityName,
			String keyDefinition,
			String options,
			List<Column> referencedColumns) {
		final var key = new ForeignKeyKey( keyColumns, referencedEntityName, referencedColumns );

		ForeignKey foreignKey = foreignKeys.get( key );
		if ( foreignKey == null ) {
			foreignKey = new ForeignKey( this );
			foreignKey.setReferencedEntityName( referencedEntityName );
			foreignKey.setKeyDefinition( keyDefinition );
			foreignKey.setOptions( options );
			for ( var keyColumn : keyColumns ) {
				foreignKey.addColumn( keyColumn );
			}

			// null referencedColumns means a reference to primary key
			if ( referencedColumns != null ) {
				foreignKey.addReferencedColumns( referencedColumns );
			}

			// NOTE: if the name is null, we will generate an implicit name during second pass processing
			//       after we know the referenced table name (which might not be resolved yet).
			foreignKey.setName( keyName );

			foreignKeys.put( key, foreignKey );
		}

		if ( keyName != null ) {
			foreignKey.setName( keyName );
		}

		return foreignKey;
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

	@Deprecated(since = "6.2")
	public void addCheckConstraint(String constraint) {
		addCheck( new CheckConstraint( constraint ) );
	}

	public void addCheck(CheckConstraint check) {
		checkConstraints.add( check );
	}

	public boolean containsColumn(Column column) {
		return columns.containsValue( column );
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
		return subselect;
	}

	public void setSubselect(String subselect) {
		this.subselect = subselect;
	}

	public boolean isSubselect() {
		return subselect != null;
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
		return viewQuery != null;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public List<CheckConstraint> getChecks() {
		return unmodifiableList( checkConstraints );
	}

	@Override
	public String getExportIdentifier() {
		return Table.qualify( render( catalog ), render( schema ), name.render() );
	}

	private String render(Identifier identifier) {
		return identifier == null ? null : identifier.render();
	}

	@Internal
	public void reorderColumns(List<Column> columns) {
		assert this.columns.size() == columns.size() && this.columns.values().containsAll( columns );
		this.columns.clear();
		for ( var column : columns ) {
			this.columns.put( column.getCanonicalName(), column );
		}
	}

	public String getViewQuery() {
		return viewQuery;
	}

	public void setViewQuery(String viewQuery) {
		this.viewQuery = viewQuery;
	}

	@Deprecated(since = "7") // this class should be private!
	public static class ForeignKeyKey implements Serializable {
		private final String referencedClassName;
		private final Column[] columns;
		private final Column[] referencedColumns;

		ForeignKeyKey(List<Column> columns, String referencedClassName, List<Column> referencedColumns) {
			Objects.requireNonNull( columns );
			Objects.requireNonNull( referencedClassName );
			this.referencedClassName = referencedClassName;
			this.columns = columns.toArray( EMPTY_COLUMN_ARRAY );
			this.referencedColumns = referencedColumns != null
					? referencedColumns.toArray( EMPTY_COLUMN_ARRAY )
					: EMPTY_COLUMN_ARRAY;
		}

		public int hashCode() {
			return Arrays.hashCode( columns ) + Arrays.hashCode( referencedColumns );
		}

		public boolean equals(Object other) {
			return other instanceof ForeignKeyKey foreignKeyKey
				&& Arrays.equals( foreignKeyKey.columns, columns )
				&& Arrays.equals( foreignKeyKey.referencedColumns, referencedColumns );
		}

		@Override
		public String toString() {
			return "ForeignKeyKey{columns=" + Arrays.toString( columns ) +
					", referencedClassName='" + referencedClassName +
					"', referencedColumns=" + Arrays.toString( referencedColumns ) +
					'}';
		}
	}

	/**
	 * @deprecated Use {@link #addInitCommand(Function)} instead.
	 */
	@Deprecated
	public void addInitCommand(InitCommand command) {
		addInitCommand( ignored -> command );
	}

	public void addInitCommand(Function<SqlStringGenerationContext, InitCommand> commandProducer) {
		if ( initCommandProducers == null ) {
			initCommandProducers = new ArrayList<>();
		}
		initCommandProducers.add( commandProducer );
	}

	public List<InitCommand> getInitCommands(SqlStringGenerationContext context) {
		if ( initCommandProducers == null ) {
			return emptyList();
		}
		else {
			final List<InitCommand> initCommands = new ArrayList<>();
			for ( Function<SqlStringGenerationContext, InitCommand> producer : initCommandProducers ) {
				initCommands.add( producer.apply( context ) );
			}
			return unmodifiableList( initCommands );
		}
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}
}
