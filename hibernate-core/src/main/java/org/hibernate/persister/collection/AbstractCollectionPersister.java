/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.QueryException;
import org.hibernate.TransientObjectException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.StructuredCollectionCacheEntry;
import org.hibernate.cache.spi.entry.StructuredMapCacheEntry;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.FilterHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.loader.ast.internal.CollectionElementLoaderByIndex;
import org.hibernate.loader.ast.internal.CollectionLoaderBatchKey;
import org.hibernate.loader.ast.internal.CollectionLoaderNamedQuery;
import org.hibernate.loader.ast.internal.CollectionLoaderSingleKey;
import org.hibernate.loader.ast.internal.CollectionLoaderSubSelectFetch;
import org.hibernate.loader.ast.internal.LoaderSqlAstCreationState;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.metamodel.mapping.internal.PluralAttributeMappingImpl;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.walking.internal.CompositionSingularSubAttributesHelper;
import org.hibernate.persister.walking.internal.StandardAnyTypeDefinition;
import org.hibernate.persister.walking.spi.AnyMappingDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
import org.hibernate.persister.walking.spi.CompositeCollectionElementDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.Alias;
import org.hibernate.sql.Delete;
import org.hibernate.sql.Insert;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.Template;
import org.hibernate.sql.Update;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.AliasedExpression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Base implementation of the {@code QueryableCollection} interface.
 *
 * @author Gavin King
 * @see BasicCollectionPersister
 * @see OneToManyPersister
 */
public abstract class AbstractCollectionPersister
		implements CollectionMetadata, SQLLoadableCollection, PluralAttributeMappingImpl.Aware {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class,
			AbstractCollectionPersister.class.getName() );

	// TODO: encapsulate the protected instance variables!

	private final NavigableRole navigableRole;

	// SQL statements
	private final String sqlDeleteString;
	private final String sqlInsertRowString;
	private final String sqlUpdateRowString;
	private final String sqlDeleteRowString;
	private final String sqlSelectSizeString;
	private final String sqlDetectRowByIndexString;
	private final String sqlDetectRowByElementString;

	protected final boolean hasWhere;
	protected final String sqlWhereString;
	private final String sqlWhereStringTemplate;

	private final boolean hasOrder;
	private final boolean hasManyToManyOrder;

	private final int baseIndex;

	private final String mappedByProperty;

	protected final boolean indexContainsFormula;
	protected final boolean elementIsPureFormula;

	// types
	private final Type keyType;
	private final Type indexType;
	protected final Type elementType;
	private final Type identifierType;

	// columns
	protected final String[] keyColumnNames;
	protected final String[] indexColumnNames;
	protected final String[] indexFormulaTemplates;
	protected final String[] indexFormulas;
	protected final boolean[] indexColumnIsGettable;
	protected final boolean[] indexColumnIsSettable;
	protected final String[] elementColumnNames;
	protected final String[] elementColumnWriters;
	protected final String[] elementColumnReaders;
	protected final String[] elementColumnReaderTemplates;
	protected final String[] elementFormulaTemplates;
	protected final String[] elementFormulas;
	protected final boolean[] elementColumnIsGettable;
	protected final boolean[] elementColumnIsSettable;
	protected final boolean[] elementColumnIsInPrimaryKey;
	protected final String[] indexColumnAliases;
	protected final String[] elementColumnAliases;
	protected final String[] keyColumnAliases;

	protected final String identifierColumnName;
	private final String identifierColumnAlias;
	// private final String unquotedIdentifierColumnName;

	protected final String qualifiedTableName;

	private final String queryLoaderName;

	private final boolean isPrimitiveArray;
	private final boolean isArray;
	protected final boolean hasIndex;
	protected final boolean hasIdentifier;
	private final boolean isLazy;
	private final boolean isExtraLazy;
	protected final boolean isInverse;
	private final boolean isMutable;
	private final boolean isVersioned;
	protected final int batchSize;
	private final FetchMode fetchMode;
	private final boolean hasOrphanDelete;
	private final boolean subselectLoadable;

	// extra information about the element type
	private final Class<?> elementClass;
	private final String entityName;

	private final Dialect dialect;
	protected final SqlExceptionHelper sqlExceptionHelper;
	private final SessionFactoryImplementor factory;
	private final EntityPersister ownerPersister;
	private final IdentifierGenerator identifierGenerator;
	private final PropertyMapping elementPropertyMapping;
	private final EntityPersister elementPersister;
	private final CollectionDataAccess cacheAccessStrategy;
	private final CollectionType collectionType;

	private final CacheEntryStructure cacheEntryStructure;

	// dynamic filters for the collection
	private final FilterHelper filterHelper;

	// dynamic filters specifically for many-to-many inside the collection
	private final FilterHelper manyToManyFilterHelper;

	private final String manyToManyWhereString;
	private final String manyToManyWhereTemplate;

	// custom sql
	private final boolean insertCallable;
	private final boolean updateCallable;
	private final boolean deleteCallable;
	private final boolean deleteAllCallable;
	private final ExecuteUpdateResultCheckStyle insertCheckStyle;
	private final ExecuteUpdateResultCheckStyle updateCheckStyle;
	private final ExecuteUpdateResultCheckStyle deleteCheckStyle;
	private final ExecuteUpdateResultCheckStyle deleteAllCheckStyle;

	private final Serializable[] spaces;

	private final Map<String,String[]> collectionPropertyColumnAliases = new HashMap<>();

	private final Comparator<?> comparator;

	private CollectionLoader collectionLoader;
	private volatile CollectionLoader standardCollectionLoader;
	private CollectionElementLoaderByIndex collectionElementLoaderByIndex;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// "mapping model"

	private final CollectionSemantics<?,?> collectionSemantics;

	private final BasicValueConverter elementConverter;
	private final BasicValueConverter indexConverter;

	// temporary
	private final JdbcMapping convertedElementType;
	private final JdbcMapping convertedIndexType;

	public AbstractCollectionPersister(
			Collection collectionBootDescriptor,
			CollectionDataAccess cacheAccessStrategy,
			PersisterCreationContext persisterCreationContext) throws MappingException, CacheException {
		assert persisterCreationContext instanceof RuntimeModelCreationContext;

		final RuntimeModelCreationContext creationContext = (RuntimeModelCreationContext) persisterCreationContext;

		final Value elementBootDescriptor = collectionBootDescriptor.getElement();
		final Value indexBootDescriptor = collectionBootDescriptor instanceof IndexedCollection
				? ( (IndexedCollection) collectionBootDescriptor ).getIndex()
				: null;

		this.factory = creationContext.getSessionFactory();
		this.cacheAccessStrategy = cacheAccessStrategy;
		if ( factory.getSessionFactoryOptions().isStructuredCacheEntriesEnabled() ) {
			cacheEntryStructure = collectionBootDescriptor.isMap()
					? StructuredMapCacheEntry.INSTANCE
					: StructuredCollectionCacheEntry.INSTANCE;
		}
		else {
			cacheEntryStructure = UnstructuredCacheEntry.INSTANCE;
		}

		dialect = factory.getJdbcServices().getDialect();
		sqlExceptionHelper = factory.getJdbcServices().getSqlExceptionHelper();
		collectionType = collectionBootDescriptor.getCollectionType();
		navigableRole = new NavigableRole( collectionBootDescriptor.getRole() );
		entityName = collectionBootDescriptor.getOwnerEntityName();
		ownerPersister = creationContext.getDomainModel().getEntityDescriptor( entityName );
		queryLoaderName = collectionBootDescriptor.getLoaderName();
		isMutable = collectionBootDescriptor.isMutable();
		mappedByProperty = collectionBootDescriptor.getMappedByProperty();

		Table table = collectionBootDescriptor.getCollectionTable();
		fetchMode = elementBootDescriptor.getFetchMode();
		elementType = elementBootDescriptor.getType();
		// isSet = collectionBinding.isSet();
		// isSorted = collectionBinding.isSorted();
		isPrimitiveArray = collectionBootDescriptor.isPrimitiveArray();
		isArray = collectionBootDescriptor.isArray();
		subselectLoadable = collectionBootDescriptor.isSubselectLoadable();

		qualifiedTableName = determineTableName( table );

		int spacesSize = 1 + collectionBootDescriptor.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = qualifiedTableName;
		Iterator<String> tables = collectionBootDescriptor.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = tables.next();
		}

		if ( StringHelper.isNotEmpty( collectionBootDescriptor.getWhere() ) ) {
			hasWhere = true;
			sqlWhereString = "(" + collectionBootDescriptor.getWhere() + ") ";
			sqlWhereStringTemplate = Template.renderWhereStringTemplate(
					sqlWhereString,
					dialect,
					factory.getQueryEngine().getSqmFunctionRegistry()
			);
		}
		else {
			hasWhere = false;
			sqlWhereString = null;
			sqlWhereStringTemplate = null;
		}

		hasOrphanDelete = collectionBootDescriptor.hasOrphanDelete();

		int batch = collectionBootDescriptor.getBatchSize();
		if ( batch == -1 ) {
			batch = factory.getSessionFactoryOptions().getDefaultBatchFetchSize();
		}
		batchSize = batch;

		isVersioned = collectionBootDescriptor.isOptimisticLocked();

		// KEY

		keyType = collectionBootDescriptor.getKey().getType();
		Iterator<Selectable> columnIter = collectionBootDescriptor.getKey().getColumnIterator();
		int keySpan = collectionBootDescriptor.getKey().getColumnSpan();
		keyColumnNames = new String[keySpan];
		keyColumnAliases = new String[keySpan];
		int k = 0;
		while ( columnIter.hasNext() ) {
			// NativeSQL: collect key column and auto-aliases
			Column column = (Column) columnIter.next();
			keyColumnNames[k] = column.getQuotedName( dialect );
			keyColumnAliases[k] = column.getAlias( dialect, table );
			k++;
		}

		// unquotedKeyColumnNames = StringHelper.unQuote(keyColumnAliases);

		// ELEMENT

		if ( elementType.isEntityType() ) {
			String entityName = ( (EntityType) elementType ).getAssociatedEntityName();
			elementPersister = creationContext.getDomainModel().getEntityDescriptor( entityName );
			// NativeSQL: collect element column and auto-aliases

		}
		else {
			elementPersister = null;
		}

		int elementSpan = elementBootDescriptor.getColumnSpan();
		elementColumnAliases = new String[elementSpan];
		elementColumnNames = new String[elementSpan];
		elementColumnWriters = new String[elementSpan];
		elementColumnReaders = new String[elementSpan];
		elementColumnReaderTemplates = new String[elementSpan];
		elementFormulaTemplates = new String[elementSpan];
		elementFormulas = new String[elementSpan];
		elementColumnIsSettable = new boolean[elementSpan];
		elementColumnIsGettable = new boolean[elementSpan];
		elementColumnIsInPrimaryKey = new boolean[elementSpan];
		boolean isPureFormula = true;
		boolean hasNotNullableColumns = false;
		boolean oneToMany = collectionBootDescriptor.isOneToMany();
		boolean[] columnInsertability = null;
		if ( !oneToMany ) {
			columnInsertability = elementBootDescriptor.getColumnInsertability();
		}
		int j = 0;
		columnIter = elementBootDescriptor.getColumnIterator();
		while ( columnIter.hasNext() ) {
			Selectable selectable = columnIter.next();
			elementColumnAliases[j] = selectable.getAlias( dialect, table );
			if ( selectable.isFormula() ) {
				Formula form = (Formula) selectable;
				elementFormulaTemplates[j] = form.getTemplate( dialect, factory.getQueryEngine().getSqmFunctionRegistry() );
				elementFormulas[j] = form.getFormula();
			}
			else {
				Column col = (Column) selectable;
				elementColumnNames[j] = col.getQuotedName( dialect );
				elementColumnWriters[j] = col.getWriteExpr();
				elementColumnReaders[j] = col.getReadExpr( dialect );
				elementColumnReaderTemplates[j] = col.getTemplate( dialect, factory.getQueryEngine().getSqmFunctionRegistry() );
				elementColumnIsGettable[j] = true;
				if ( elementType.isComponentType() ) {
					// Implements desired behavior specifically for @ElementCollection mappings.
					elementColumnIsSettable[j] = columnInsertability[j];
				}
				else {
					// Preserves legacy non-@ElementCollection behavior
					elementColumnIsSettable[j] = true;
				}
				elementColumnIsInPrimaryKey[j] = !col.isNullable();
				if ( !col.isNullable() ) {
					hasNotNullableColumns = true;
				}
				isPureFormula = false;
			}
			j++;
		}
		elementIsPureFormula = isPureFormula;

		// workaround, for backward compatibility of sets with no
		// not-null columns, assume all columns are used in the
		// row locator SQL
		if ( !hasNotNullableColumns ) {
			Arrays.fill( elementColumnIsInPrimaryKey, true );
		}

		// INDEX AND ROW SELECT

		hasIndex = collectionBootDescriptor.isIndexed();
		if ( hasIndex ) {
			// NativeSQL: collect index column and auto-aliases
			IndexedCollection indexedCollection = (IndexedCollection) collectionBootDescriptor;
			indexType = indexedCollection.getIndex().getType();
			int indexSpan = indexedCollection.getIndex().getColumnSpan();
			boolean[] indexColumnInsertability = indexedCollection.getIndex().getColumnInsertability();
			boolean[] indexColumnUpdatability = indexedCollection.getIndex().getColumnUpdateability();
			columnIter = indexedCollection.getIndex().getColumnIterator();
			indexColumnNames = new String[indexSpan];
			indexFormulaTemplates = new String[indexSpan];
			indexFormulas = new String[indexSpan];
			indexColumnIsGettable = new boolean[indexSpan];
			indexColumnIsSettable = new boolean[indexSpan];
			indexColumnAliases = new String[indexSpan];
			int i = 0;
			boolean hasFormula = false;
			while ( columnIter.hasNext() ) {
				Selectable s = columnIter.next();
				indexColumnAliases[i] = s.getAlias( dialect );
				if ( s.isFormula() ) {
					Formula indexForm = (Formula) s;
					indexFormulaTemplates[i] = indexForm.getTemplate( dialect, factory.getQueryEngine().getSqmFunctionRegistry() );
					indexFormulas[i] = indexForm.getFormula();
					hasFormula = true;
				}
				// Treat a mapped-by index like a formula to avoid trying to set it in insert/update
				// Previously this was a sub-query formula, but was changed to represent the proper mapping
				// which enables optimizations for queries. The old insert/update code wasn't adapted yet though.
				// For now, this is good enough, because the formula is never used anymore,
				// since all read paths go through the new code that can properly handle this case
				else if ( indexedCollection instanceof org.hibernate.mapping.Map
						&& ( (org.hibernate.mapping.Map) indexedCollection ).getMapKeyPropertyName() != null ) {
					Column indexCol = (Column) s;
					indexFormulaTemplates[i] = Template.TEMPLATE + indexCol.getQuotedName( dialect );
					indexFormulas[i] = indexCol.getQuotedName( dialect );
					hasFormula = true;
				}
				else {
					Column indexCol = (Column) s;
					indexColumnNames[i] = indexCol.getQuotedName( dialect );
					indexColumnIsGettable[i] = true;
					indexColumnIsSettable[i] = indexColumnInsertability[i] || indexColumnUpdatability[i];
				}
				i++;
			}
			indexContainsFormula = hasFormula;
			baseIndex = indexedCollection.isList() ?
					( (List) indexedCollection ).getBaseIndex() : 0;
		}
		else {
			indexContainsFormula = false;
			indexColumnIsGettable = null;
			indexColumnIsSettable = null;
			indexFormulaTemplates = null;
			indexFormulas = null;
			indexType = null;
			indexColumnNames = null;
			indexColumnAliases = null;
			baseIndex = 0;
		}

		hasIdentifier = collectionBootDescriptor.isIdentified();
		if ( hasIdentifier ) {
			if ( collectionBootDescriptor.isOneToMany() ) {
				throw new MappingException( "one-to-many collections with identifiers are not supported" );
			}
			IdentifierCollection idColl = (IdentifierCollection) collectionBootDescriptor;
			identifierType = idColl.getIdentifier().getType();
			columnIter = idColl.getIdentifier().getColumnIterator();
			Column col = (Column) columnIter.next();
			identifierColumnName = col.getQuotedName( dialect );
			identifierColumnAlias = col.getAlias( dialect );
			identifierGenerator = idColl.getIdentifier().createIdentifierGenerator(
					persisterCreationContext.getBootstrapContext().getIdentifierGeneratorFactory(),
					factory.getJdbcServices().getDialect(),
					null
			);
			identifierGenerator.initialize( creationContext.getSessionFactory().getSqlStringGenerationContext() );
		}
		else {
			identifierType = null;
			identifierColumnName = null;
			identifierColumnAlias = null;
			// unquotedIdentifierColumnName = null;
			identifierGenerator = null;
		}

		// GENERATE THE SQL:

		// sqlSelectString = sqlSelectString();
		// sqlSelectRowString = sqlSelectRowString();

		if ( collectionBootDescriptor.getCustomSQLInsert() == null ) {
			sqlInsertRowString = generateInsertRowString();
			insertCallable = false;
			insertCheckStyle = ExecuteUpdateResultCheckStyle.COUNT;
		}
		else {
			sqlInsertRowString = collectionBootDescriptor.getCustomSQLInsert();
			insertCallable = collectionBootDescriptor.isCustomInsertCallable();
			insertCheckStyle = collectionBootDescriptor.getCustomSQLInsertCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( collectionBootDescriptor.getCustomSQLInsert(), insertCallable )
					: collectionBootDescriptor.getCustomSQLInsertCheckStyle();
		}

		if ( collectionBootDescriptor.getCustomSQLUpdate() == null ) {
			sqlUpdateRowString = generateUpdateRowString();
			updateCallable = false;
			updateCheckStyle = ExecuteUpdateResultCheckStyle.COUNT;
		}
		else {
			sqlUpdateRowString = collectionBootDescriptor.getCustomSQLUpdate();
			updateCallable = collectionBootDescriptor.isCustomUpdateCallable();
			updateCheckStyle = collectionBootDescriptor.getCustomSQLUpdateCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( collectionBootDescriptor.getCustomSQLUpdate(), insertCallable )
					: collectionBootDescriptor.getCustomSQLUpdateCheckStyle();
		}

		if ( collectionBootDescriptor.getCustomSQLDelete() == null ) {
			sqlDeleteRowString = generateDeleteRowString();
			deleteCallable = false;
			deleteCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}
		else {
			sqlDeleteRowString = collectionBootDescriptor.getCustomSQLDelete();
			deleteCallable = collectionBootDescriptor.isCustomDeleteCallable();
			deleteCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}

		if ( collectionBootDescriptor.getCustomSQLDeleteAll() == null ) {
			sqlDeleteString = generateDeleteString();
			deleteAllCallable = false;
			deleteAllCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}
		else {
			sqlDeleteString = collectionBootDescriptor.getCustomSQLDeleteAll();
			deleteAllCallable = collectionBootDescriptor.isCustomDeleteAllCallable();
			deleteAllCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}

		sqlSelectSizeString = generateSelectSizeString( collectionBootDescriptor.isIndexed() && !collectionBootDescriptor.isMap() );
		sqlDetectRowByIndexString = generateDetectRowByIndexString();
		sqlDetectRowByElementString = generateDetectRowByElementString();

		logStaticSQL();

		isLazy = collectionBootDescriptor.isLazy();
		isExtraLazy = collectionBootDescriptor.isExtraLazy();

		isInverse = collectionBootDescriptor.isInverse();

		if ( collectionBootDescriptor.isArray() ) {
			elementClass = ( (org.hibernate.mapping.Array) collectionBootDescriptor ).getElementClass();
		}
		else {
			// for non-arrays, we don't need to know the element class
			elementClass = null; // elementType.returnedClass();
		}

		if ( elementType.isComponentType() ) {
			elementPropertyMapping = new CompositeElementPropertyMapping(
					elementColumnNames,
					elementColumnReaders,
					elementColumnReaderTemplates,
					elementFormulaTemplates,
					(CompositeType) elementType,
					factory
					);
		}
		else if ( !elementType.isEntityType() ) {
			elementPropertyMapping = new ElementPropertyMapping(
					elementColumnNames,
					elementType
					);
		}
		else {
			// not all entity-persisters implement PropertyMapping!
			if ( elementPersister instanceof PropertyMapping ) {
				elementPropertyMapping = (PropertyMapping) elementPersister;
			}
			else {
				elementPropertyMapping = new ElementPropertyMapping( elementColumnNames, elementType );
			}
		}

		hasOrder = collectionBootDescriptor.getOrderBy() != null;
		hasManyToManyOrder = collectionBootDescriptor.getManyToManyOrdering() != null;

		// Handle any filters applied to this collectionBinding
		if ( collectionBootDescriptor.getFilters().isEmpty() ) {
			filterHelper = null;
		}
		else {
			filterHelper = new FilterHelper( collectionBootDescriptor.getFilters(), factory);
		}

		// Handle any filters applied to this collectionBinding for many-to-many
		if ( collectionBootDescriptor.getManyToManyFilters().isEmpty() ) {
			manyToManyFilterHelper = null;
		}
		else {
			manyToManyFilterHelper = new FilterHelper( collectionBootDescriptor.getManyToManyFilters(), factory);
		}

		if ( StringHelper.isEmpty( collectionBootDescriptor.getManyToManyWhere() ) ) {
			manyToManyWhereString = null;
			manyToManyWhereTemplate = null;
		}
		else {
			manyToManyWhereString = "( " + collectionBootDescriptor.getManyToManyWhere() + ")";
			manyToManyWhereTemplate = Template.renderWhereStringTemplate(
					manyToManyWhereString,
					factory.getJdbcServices().getDialect(),
					factory.getQueryEngine().getSqmFunctionRegistry()
			);
		}

		comparator = collectionBootDescriptor.getComparator();

		initCollectionPropertyMap();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// "mapping model"

		this.collectionSemantics = creationContext.getBootstrapContext()
				.getMetadataBuildingOptions()
				.getPersistentCollectionRepresentationResolver()
				.resolveRepresentation( collectionBootDescriptor );

		if ( elementBootDescriptor instanceof BasicValue ) {
			final BasicValue.Resolution<?> basicTypeResolution = ( (BasicValue) elementBootDescriptor ).resolve();
			this.elementConverter = basicTypeResolution.getValueConverter();
			this.convertedElementType = basicTypeResolution.getJdbcMapping();
		}
		else {
			this.elementConverter = null;
			this.convertedElementType = null;
		}

		if ( indexBootDescriptor instanceof BasicValue ) {
			final BasicValue.Resolution<?> basicTypeResolution = ( (BasicValue) indexBootDescriptor ).resolve();
			this.indexConverter = basicTypeResolution.getValueConverter();
			this.convertedIndexType = basicTypeResolution.getJdbcMapping();
		}
		else {
			this.indexConverter = null;
			this.convertedIndexType = null;
		}
		if ( queryLoaderName != null ) {
			// We must resolve the named query on-demand through the boot model because it isn't initialized yet
			final NamedQueryMemento namedQueryMemento = factory.getQueryEngine().getNamedObjectRepository()
					.resolve( factory, collectionBootDescriptor.getMetadata(), queryLoaderName );
			if ( namedQueryMemento == null ) {
				throw new IllegalArgumentException( "Could not resolve named load-query [" + navigableRole + "] : " + queryLoaderName );
			}
		}
	}

	@Override
	public Comparator<?> getSortingComparator() {
		return comparator;
	}

	protected String determineTableName(Table table) {
		return MappingModelCreationHelper.getTableIdentifierExpression( table, factory );
	}

//	private class ColumnMapperImpl implements ColumnMapper {
//		@Override
//		public SqlValueReference[] map(String reference) {
//			final String[] columnNames;
//			final String[] formulaTemplates;
//
//			// handle the special "$element$" property name...
//			if ( "$element$".equals( reference ) ) {
//				columnNames = elementColumnNames;
//				formulaTemplates = elementFormulaTemplates;
//			}
//			else {
//				columnNames = elementPropertyMapping.toColumns( reference );
//				formulaTemplates = formulaTemplates( reference, columnNames.length );
//			}
//
//			final SqlValueReference[] result = new SqlValueReference[ columnNames.length ];
//			int i = 0;
//			for ( final String columnName : columnNames ) {
//				if ( columnName == null ) {
//					// if the column name is null, it indicates that this index in the property value mapping is
//					// actually represented by a formula.
////					final int propertyIndex = elementPersister.getEntityMetamodel().getPropertyIndex( reference );
//					final String formulaTemplate = formulaTemplates[i];
//					result[i] = new FormulaReference() {
//						@Override
//						public String getFormulaFragment() {
//							return formulaTemplate;
//						}
//					};
//				}
//				else {
//					result[i] = new ColumnReference() {
//						@Override
//						public String getColumnName() {
//							return columnName;
//						}
//					};
//				}
//				i++;
//			}
//			return result;
//		}
//	}

//	private String[] formulaTemplates(String reference, int expectedSize) {
//		try {
//			final int propertyIndex = elementPersister.getEntityMetamodel().getPropertyIndex( reference );
//			return  ( (Queryable) elementPersister ).getSubclassPropertyFormulaTemplateClosure()[propertyIndex];
//		}
//		catch (Exception e) {
//			return new String[expectedSize];
//		}
//	}

	@Override
	public void postInstantiate() throws MappingException {
		if ( queryLoaderName == null ) {
			collectionLoader = createCollectionLoader( LoadQueryInfluencers.NONE );
		}
		else {
			// We pass null as metamodel because we did the initialization during construction already
			final NamedQueryMemento namedQueryMemento = factory.getQueryEngine().getNamedObjectRepository()
					.resolve( factory, null, queryLoaderName );
			collectionLoader = new CollectionLoaderNamedQuery( this, namedQueryMemento );
		}
		if ( attributeMapping.getIndexDescriptor() != null ) {
			collectionElementLoaderByIndex = new CollectionElementLoaderByIndex(
					attributeMapping,
					baseIndex,
					LoadQueryInfluencers.NONE,
					getFactory()
			);
		}
	}

	protected void logStaticSQL() {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Static SQL for collection: %s", getRole() );
			if ( getSQLInsertRowString() != null ) {
				LOG.debugf( " Row insert: %s", getSQLInsertRowString() );
			}
			if ( getSQLUpdateRowString() != null ) {
				LOG.debugf( " Row update: %s", getSQLUpdateRowString() );
			}
			if ( getSQLDeleteRowString() != null ) {
				LOG.debugf( " Row delete: %s", getSQLDeleteRowString() );
			}
			if ( getSQLDeleteString() != null ) {
				LOG.debugf( " One-shot delete: %s", getSQLDeleteString() );
			}
		}
	}

	@Override
	public void initialize(Object key, SharedSessionContractImplementor session) throws HibernateException {
//		getAppropriateInitializer( key, session ).initialize( key, session );
		determineLoaderToUse( key, session ).load( key, session );
	}

	// lazily initialize instance field via 'double-checked locking'
	// see https://en.wikipedia.org/wiki/Double-checked_locking on why 'volatile' and local copy is used
	protected CollectionLoader getStandardCollectionLoader() {
		CollectionLoader localCopy = standardCollectionLoader;
		if ( localCopy == null ) {
			synchronized (this) {
				localCopy = standardCollectionLoader;
				if ( localCopy == null ) {
					if ( queryLoaderName != null ) {
						localCopy = collectionLoader;
					}
					else {
						localCopy = createCollectionLoader( LoadQueryInfluencers.NONE );
					}
					standardCollectionLoader  = localCopy;
				}
			}
		}
		return localCopy;
	}

	protected CollectionLoader determineLoaderToUse(Object key, SharedSessionContractImplementor session) {
		if ( queryLoaderName != null ) {
			// if there is a user-specified loader, return that
			return getStandardCollectionLoader();
		}

		final CollectionLoader subSelectLoader = resolveSubSelectLoader( key, session );
		if ( subSelectLoader != null ) {
			return subSelectLoader;
		}

		if ( ! session.getLoadQueryInfluencers().hasEnabledFilters() && ! isAffectedByEnabledFetchProfiles( session.getLoadQueryInfluencers() ) ) {
			return getStandardCollectionLoader();
		}

		return createCollectionLoader( session.getLoadQueryInfluencers() );
	}

	private CollectionLoader resolveSubSelectLoader(Object key, SharedSessionContractImplementor session) {
		if ( !isSubselectLoadable() ) {
			return null;
		}

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		final EntityKey ownerEntityKey = session.generateEntityKey( key, getOwnerEntityPersister() );
		final SubselectFetch subselect = persistenceContext.getBatchFetchQueue().getSubselect( ownerEntityKey );
		if ( subselect == null ) {
			return null;
		}

		// Take care of any entities that might have
		// been evicted!
		subselect.getResultingEntityKeys().removeIf( o -> !persistenceContext.containsEntity( o ) );

		// Run a subquery loader
		return createSubSelectLoader( subselect, session );
	}

	protected CollectionLoader createSubSelectLoader(SubselectFetch subselect, SharedSessionContractImplementor session) {
		//noinspection RedundantCast
		return new CollectionLoaderSubSelectFetch(
				attributeMapping,
				(DomainResult<?>) null,
				subselect,
				session
		);
	}

	protected CollectionLoader createCollectionLoader(LoadQueryInfluencers loadQueryInfluencers) {
		final int batchSize = getBatchSize();
		if ( batchSize > 1 ) {
			return new CollectionLoaderBatchKey( attributeMapping, batchSize, loadQueryInfluencers, getFactory() );
		}


		return new CollectionLoaderSingleKey( attributeMapping, loadQueryInfluencers, getFactory() );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public CollectionDataAccess getCacheAccessStrategy() {
		return cacheAccessStrategy;
	}

	@Override
	public boolean hasCache() {
		return cacheAccessStrategy != null;
	}

	@Override
	public CollectionType getCollectionType() {
		return collectionType;
	}

	@Override
	public String getSQLOrderByString(String alias) {
//		return hasOrdering()
//				? orderByTranslation.injectAliases( new StandardOrderByAliasResolver( alias ) )
//				: "";
		if ( hasOrdering() ) {
			throw new NotYetImplementedFor6Exception( getClass() );
		}

		return "";
	}

	@Override
	public String getManyToManyOrderByString(String alias) {
//		return hasManyToManyOrdering()
//				? manyToManyOrderByTranslation.injectAliases( new StandardOrderByAliasResolver( alias ) )
//				: "";
		if ( hasManyToManyOrdering() ) {
			throw new NotYetImplementedFor6Exception( getClass() );
		}

		return "";
	}

	@Override
	public FetchMode getFetchMode() {
		return fetchMode;
	}

	@Override
	public boolean hasOrdering() {
		return hasOrder;
	}

	@Override
	public boolean hasManyToManyOrdering() {
		return isManyToMany() && hasManyToManyOrder;
	}

	@Override
	public boolean hasWhere() {
		return hasWhere;
	}

	protected String getSQLDeleteString() {
		return sqlDeleteString;
	}

	protected String getSQLInsertRowString() {
		return sqlInsertRowString;
	}

	protected String getSQLUpdateRowString() {
		return sqlUpdateRowString;
	}

	protected String getSQLDeleteRowString() {
		return sqlDeleteRowString;
	}

	@Override
	public Type getKeyType() {
		return keyType;
	}

	@Override
	public Type getIndexType() {
		return indexType;
	}

	@Override
	public Type getElementType() {
		return elementType;
	}

	@Override
	public BasicValueConverter<?,?> getElementConverter() {
		return elementConverter;
	}

	@Override
	public BasicValueConverter<?,?> getIndexConverter() {
		return indexConverter;
	}

	/**
	 * Return the element class of an array, or null otherwise.  needed by arrays
	 */
	@Override
	public Class<?> getElementClass() {
		return elementClass;
	}

	protected Object decrementIndexByBase(Object index) {
		if ( baseIndex != 0 ) {
			index = (Integer)index - baseIndex;
		}
		return index;
	}

	/**
	 * Write the key to a JDBC {@code PreparedStatement}
	 */
	protected int writeKey(PreparedStatement st, Object key, int i, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

		if ( key == null ) {
			throw new NullPointerException( "null key for collection: " + navigableRole.getFullPath() ); // an assertion
		}
		getKeyType().nullSafeSet( st, key, i, session );
		return i + keyColumnAliases.length;
	}

	/**
	 * Write the element to a JDBC {@code PreparedStatement}
	 */
	protected int writeElement(PreparedStatement st, Object elt, int i, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
//		if ( elementConverter != null ) {
//			//noinspection unchecked
//			final Object converted = elementConverter.toRelationalValue( elt );
//			convertedElementType.getJdbcValueBinder().bind( st, converted, i, session );
//		}
//		else {
//			getElementType().nullSafeSet( st, elt, i, elementColumnIsSettable, session );
//		}
		getElementType().nullSafeSet( st, elt, i, elementColumnIsSettable, session );

		return i + ArrayHelper.countTrue( elementColumnIsSettable );

	}

	/**
	 * Write the index to a JDBC {@code PreparedStatement}
	 */
	protected int writeIndex(PreparedStatement st, Object index, int i, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( indexConverter != null ) {
			//noinspection unchecked
			final Object converted = indexConverter.toRelationalValue( index );
			//noinspection unchecked
			convertedIndexType.getJdbcValueBinder().bind( st, converted, i, session );
		}
		else {
			getIndexType().nullSafeSet( st, incrementIndexByBase( index ), i, indexColumnIsSettable, session );
		}

		return i + ArrayHelper.countTrue( indexColumnIsSettable );
	}

	protected Object incrementIndexByBase(Object index) {
		if ( baseIndex != 0 ) {
			index = (Integer)index + baseIndex;
		}
		return index;
	}

	/**
	 * Write the element to a JDBC {@code PreparedStatement}
	 */
	protected int writeElementToWhere(PreparedStatement st, Object elt, int i, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( elementIsPureFormula ) {
			throw new AssertionFailure( "cannot use a formula-based element in the where condition" );
		}
		if ( elementConverter != null ) {
			final Object converted = elementConverter.toRelationalValue( elt );
			convertedElementType.getJdbcValueBinder().bind( st, converted, i, session );
		}
		else {
			getElementType().nullSafeSet( st, elt, i, elementColumnIsInPrimaryKey, session );
		}
		return i + elementColumnAliases.length;
	}

	/**
	 * Write the index to a JDBC {@code PreparedStatement}
	 */
	protected int writeIndexToWhere(PreparedStatement st, Object index, int i, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( indexContainsFormula ) {
			throw new AssertionFailure( "cannot use a formula-based index in the where condition" );
		}
		if ( indexConverter != null ) {
			final Object converted = indexConverter.toRelationalValue( index );
			convertedIndexType.getJdbcValueBinder().bind( st, converted, i, session );
		}
		else {
			getIndexType().nullSafeSet( st, incrementIndexByBase( index ), i, session );
		}
		return i + indexColumnAliases.length;
	}

	/**
	 * Write the identifier to a JDBC {@code PreparedStatement}
	 */
	public int writeIdentifier(PreparedStatement st, Object id, int i, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {

		getIdentifierType().nullSafeSet( st, id, i, session );
		return i + 1;
	}

	@Override
	public boolean isPrimitiveArray() {
		return isPrimitiveArray;
	}

	@Override
	public boolean isArray() {
		return isArray;
	}

	@Override
	public String[] getKeyColumnAliases(String suffix) {
		return new Alias( suffix ).toAliasStrings( keyColumnAliases );
	}

	@Override
	public String[] getElementColumnAliases(String suffix) {
		return new Alias( suffix ).toAliasStrings( elementColumnAliases );
	}

	@Override
	public String[] getIndexColumnAliases(String suffix) {
		if ( hasIndex ) {
			return new Alias( suffix ).toAliasStrings( indexColumnAliases );
		}
		else {
			return null;
		}
	}

	@Override
	public String getIdentifierColumnAlias(String suffix) {
		if ( hasIdentifier ) {
			return new Alias( suffix ).toAliasString( identifierColumnAlias );
		}
		else {
			return null;
		}
	}

	@Override
	public String getIdentifierColumnName() {
		if ( hasIdentifier ) {
			return identifierColumnName;
		}
		else {
			return null;
		}
	}

	/**
	 * Generate a list of collection index, key and element columns
	 */
	@Override
	public String selectFragment(String alias, String columnSuffix) {
		final PluralAttributeMapping attributeMapping = getAttributeMapping();
		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				LockOptions.NONE,
				(fetchParent, querySpec, creationState) -> new ArrayList<>(),
				true,
				getFactory()
		);

		final NavigablePath entityPath = new NavigablePath( attributeMapping.getRootPathName() );
		final TableGroup rootTableGroup = attributeMapping.createRootTableGroup(
				true,
				entityPath,
				null,
				() -> p -> {},
				new SqlAliasBaseConstant( alias ),
				sqlAstCreationState.getSqlExpressionResolver(),
				sqlAstCreationState.getFromClauseAccess(),
				getFactory()
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( entityPath, rootTableGroup );

		attributeMapping.createDomainResult( entityPath, rootTableGroup, null, sqlAstCreationState );

		// Wrap expressions with aliases
		final SelectClause selectClause = rootQuerySpec.getSelectClause();
		final java.util.List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
		int i = 0;
		for ( String keyAlias : keyColumnAliases ) {
			sqlSelections.set(
					i,
					new SqlSelectionImpl(
							i,
							i + 1,
							new AliasedExpression( sqlSelections.get( i ).getExpression(), keyAlias + columnSuffix )
					)
			);
			i++;
		}

		if ( hasIndex ) {
			for ( String indexAlias : indexColumnAliases ) {
				sqlSelections.set(
						i,
						new SqlSelectionImpl(
								i,
								i + 1,
								new AliasedExpression( sqlSelections.get( i ).getExpression(), indexAlias + columnSuffix )
						)
				);
				i++;
			}
		}
		if ( hasIdentifier ) {
			sqlSelections.set(
					i,
					new SqlSelectionImpl(
							i,
							i + 1,
							new AliasedExpression( sqlSelections.get( i ).getExpression(), identifierColumnAlias + columnSuffix )
					)
			);
			i++;
		}

		for ( int columnIndex = 0; i < sqlSelections.size(); i++, columnIndex++ ) {
			final SqlSelection sqlSelection = sqlSelections.get( i );
			sqlSelections.set(
					i,
					new SqlSelectionImpl(
							sqlSelection.getValuesArrayPosition(),
							sqlSelection.getJdbcResultSetIndex(),
							new AliasedExpression( sqlSelection.getExpression(), elementColumnAliases[columnIndex] + columnSuffix )
					)
			);
		}

		final String sql = getFactory().getJdbcServices()
				.getDialect()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( getFactory(), new SelectStatement( rootQuerySpec ) )
				.translate( null, QueryOptions.NONE )
				.getSql();
		final int fromIndex = sql.lastIndexOf( " from" );
		final String expression;
		if ( fromIndex != -1 ) {
			expression = sql.substring( "select ".length(), fromIndex );
		}
		else {
			expression = sql.substring( "select ".length() );
		}
		return expression;
	}

	protected String generateSelectSizeString(boolean isIntegerIndexed) {
		String selectValue = isIntegerIndexed ?
				"max(" + getIndexColumnNames()[0] + ") + 1" : // lists, arrays
				"count(" + getElementColumnNames()[0] + ")"; // sets, maps, bags
		return new SimpleSelect( dialect )
				.setTableName( getTableName() )
				.addCondition( getKeyColumnNames(), "=?" )
				.addWhereToken( sqlWhereString )
				.addColumn( selectValue )
				.toStatementString();
	}

	protected String generateDetectRowByIndexString() {
		if ( !hasIndex() ) {
			return null;
		}
		return new SimpleSelect( dialect )
				.setTableName( getTableName() )
				.addCondition( getKeyColumnNames(), "=?" )
				.addCondition( getIndexColumnNames(), "=?" )
				.addCondition( indexFormulas, "=?" )
				.addWhereToken( sqlWhereString )
				.addColumn( "1" )
				.toStatementString();
	}


	protected String generateDetectRowByElementString() {
		return new SimpleSelect( dialect )
				.setTableName( getTableName() )
				.addCondition( getKeyColumnNames(), "=?" )
				.addCondition( getElementColumnNames(), "=?" )
				.addCondition( elementFormulas, "=?" )
				.addWhereToken( sqlWhereString )
				.addColumn( "1" )
				.toStatementString();
	}

	@Override
	public String[] getIndexColumnNames() {
		return indexColumnNames;
	}

	@Override
	public String[] getIndexFormulas() {
		return indexFormulas;
	}

	@Override
	public String[] getIndexColumnNames(String alias) {
		return qualify( alias, indexColumnNames, indexFormulaTemplates );
	}

	@Override
	public String[] getElementColumnNames(String alias) {
		return qualify( alias, elementColumnNames, elementFormulaTemplates );
	}

	private static String[] qualify(String alias, String[] columnNames, String[] formulaTemplates) {
		int span = columnNames.length;
		String[] result = new String[span];
		for ( int i = 0; i < span; i++ ) {
			if ( columnNames[i] == null ) {
				result[i] = StringHelper.replace( formulaTemplates[i], Template.TEMPLATE, alias );
			}
			else {
				result[i] = StringHelper.qualify( alias, columnNames[i] );
			}
		}
		return result;
	}

	@Override
	public String[] getElementColumnNames() {
		return elementColumnNames; // TODO: something with formulas...
	}

	@Override
	public String[] getKeyColumnNames() {
		return keyColumnNames;
	}

	@Override
	public boolean hasIndex() {
		return hasIndex;
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	@Override
	public boolean isInverse() {
		return isInverse;
	}

	@Override
	public String getTableName() {
		return qualifiedTableName;
	}

	private BasicBatchKey removeBatchKey;

	@Override
	public void remove(Object id, SharedSessionContractImplementor session) throws HibernateException {
		if ( !isInverse && isRowDeleteEnabled() ) {

			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Deleting collection: %s",
						MessageHelper.collectionInfoString( this, id, getFactory() ) );
			}

			// Remove all the old entries

			try {
				int offset = 1;
				final PreparedStatement st;
				Expectation expectation = Expectations.appropriateExpectation( getDeleteAllCheckStyle() );
				boolean callable = isDeleteAllCallable();
				boolean useBatch = expectation.canBeBatched();
				final String sql = getSQLDeleteString();
				if ( useBatch ) {
					if ( removeBatchKey == null ) {
						removeBatchKey = new BasicBatchKey(
								getRole() + "#REMOVE",
								expectation
								);
					}
					st = session
							.getJdbcCoordinator()
							.getBatch( removeBatchKey )
							.getBatchStatement( sql, callable );
				}
				else {
					st = session
							.getJdbcCoordinator()
							.getStatementPreparer()
							.prepareStatement( sql, callable );
				}

				try {
					offset += expectation.prepare( st );

					writeKey( st, id, offset, session );
					if ( useBatch ) {
						session
								.getJdbcCoordinator()
								.getBatch( removeBatchKey )
								.addToBatch();
					}
					else {
						expectation.verifyOutcome( session.getJdbcCoordinator().getResultSetReturn().executeUpdate( st ), st, -1, sql );
					}
				}
				catch ( SQLException sqle ) {
					if ( useBatch ) {
						session.getJdbcCoordinator().abortBatch();
					}
					throw sqle;
				}
				finally {
					if ( !useBatch ) {
						session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
						session.getJdbcCoordinator().afterStatementExecution();
					}
				}

				LOG.debug( "Done deleting collection" );
			}
			catch ( SQLException sqle ) {
				throw sqlExceptionHelper.convert(
						sqle,
						"could not delete collection: " +
								MessageHelper.collectionInfoString( this, id, getFactory() ),
						getSQLDeleteString()
						);
			}

		}

	}

	protected BasicBatchKey recreateBatchKey;

	@Override
	public void recreate(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session)
			throws HibernateException {

		if ( isInverse ) {
			return;
		}

		if ( !isRowInsertEnabled() ) {
			return;
		}


		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Inserting collection: %s",
					MessageHelper.collectionInfoString( this, collection, id, session )
			);
		}

		try {
			// create all the new entries
			Iterator<?> entries = collection.entries( this );
			final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
			if ( entries.hasNext() ) {
				Expectation expectation = Expectations.appropriateExpectation( getInsertCheckStyle() );
				collection.preInsert( this );
				int i = 0;
				int count = 0;
				while ( entries.hasNext() ) {

					final Object entry = entries.next();
					if ( collection.entryExists( entry, i ) ) {
						int offset = 1;
						final PreparedStatement st;
						boolean callable = isInsertCallable();
						boolean useBatch = expectation.canBeBatched();
						final String sql = getSQLInsertRowString();

						if ( useBatch ) {
							if ( recreateBatchKey == null ) {
								recreateBatchKey = new BasicBatchKey(
										getRole() + "#RECREATE",
										expectation
								);
							}
							st = jdbcCoordinator
									.getBatch( recreateBatchKey )
									.getBatchStatement( sql, callable );
						}
						else {
							st = jdbcCoordinator
									.getStatementPreparer()
									.prepareStatement( sql, callable );
						}

						try {
							offset += expectation.prepare( st );

							// TODO: copy/paste from insertRows()
							int loc = writeKey( st, id, offset, session );
							if ( hasIdentifier ) {
								loc = writeIdentifier( st, collection.getIdentifier( entry, i ), loc, session );
							}
							if ( hasIndex /* && !indexIsFormula */) {
								loc = writeIndex( st, collection.getIndex( entry, i, this ), loc, session );
							}
							loc = writeElement( st, collection.getElement( entry ), loc, session );

							if ( useBatch ) {
								jdbcCoordinator
										.getBatch( recreateBatchKey )
										.addToBatch();
							}
							else {
								expectation.verifyOutcome( jdbcCoordinator
																.getResultSetReturn().executeUpdate( st ), st, -1, sql );
							}

							collection.afterRowInsert( this, entry, i );
							count++;
						}
						catch ( SQLException sqle ) {
							if ( useBatch ) {
								jdbcCoordinator.abortBatch();
							}
							throw sqle;
						}
						finally {
							if ( !useBatch ) {
								jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
								jdbcCoordinator.afterStatementExecution();
							}
						}

					}
					i++;
				}

				LOG.debugf( "Done inserting collection: %s rows inserted", count );

			}
			else {
				LOG.debug( "Collection was empty" );
			}
		}
		catch ( SQLException sqle ) {
			throw sqlExceptionHelper.convert(
					sqle,
					"could not insert collection: " +
							MessageHelper.collectionInfoString( this, collection, id, session ),
					getSQLInsertRowString()
			);
		}
	}

	protected boolean isRowDeleteEnabled() {
		return true;
	}

	private BasicBatchKey deleteBatchKey;

	@Override
	public void deleteRows(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session)
			throws HibernateException {

		if ( isInverse ) {
			return;
		}

		if ( !isRowDeleteEnabled() ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Deleting rows of collection: %s",
					MessageHelper.collectionInfoString( this, collection, id, session )
			);
		}

		boolean deleteByIndex = !isOneToMany() && hasIndex && !indexContainsFormula;
		final Expectation expectation = Expectations.appropriateExpectation( getDeleteCheckStyle() );
		try {
			// delete all the deleted entries
			Iterator<?> deletes = collection.getDeletes( this, !deleteByIndex );
			if ( deletes.hasNext() ) {
				int offset = 1;
				int count = 0;
				while ( deletes.hasNext() ) {
					final PreparedStatement st;
					boolean callable = isDeleteCallable();
					boolean useBatch = expectation.canBeBatched();
					final String sql = getSQLDeleteRowString();

					if ( useBatch ) {
						if ( deleteBatchKey == null ) {
							deleteBatchKey = new BasicBatchKey(
									getRole() + "#DELETE",
									expectation
									);
						}
						st = session
								.getJdbcCoordinator()
								.getBatch( deleteBatchKey )
								.getBatchStatement( sql, callable );
					}
					else {
						st = session
								.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( sql, callable );
					}

					try {
						expectation.prepare( st );

						Object entry = deletes.next();
						int loc = offset;
						if ( hasIdentifier ) {
							writeIdentifier( st, entry, loc, session );
						}
						else {
							loc = writeKey( st, id, loc, session );
							if ( deleteByIndex ) {
								writeIndexToWhere( st, entry, loc, session );
							}
							else {
								writeElementToWhere( st, entry, loc, session );
							}
						}

						if ( useBatch ) {
							session
									.getJdbcCoordinator()
									.getBatch( deleteBatchKey )
									.addToBatch();
						}
						else {
							expectation.verifyOutcome( session.getJdbcCoordinator().getResultSetReturn().executeUpdate( st ), st, -1, sql );
						}
						count++;
					}
					catch ( SQLException sqle ) {
						if ( useBatch ) {
							session.getJdbcCoordinator().abortBatch();
						}
						throw sqle;
					}
					finally {
						if ( !useBatch ) {
							session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
							session.getJdbcCoordinator().afterStatementExecution();
						}
					}

					LOG.debugf( "Done deleting collection rows: %s deleted", count );
				}
			}
			else {
				LOG.debug( "No rows to delete" );
			}
		}
		catch ( SQLException sqle ) {
			throw sqlExceptionHelper.convert(
					sqle,
					"could not delete collection rows: " +
							MessageHelper.collectionInfoString( this, collection, id, session ),
					getSQLDeleteRowString()
			);
		}
	}

	protected boolean isRowInsertEnabled() {
		return true;
	}

	private BasicBatchKey insertBatchKey;

	@Override
	public void insertRows(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session)
			throws HibernateException {

		if ( isInverse ) {
			return;
		}

		if ( !isRowInsertEnabled() ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Inserting rows of collection: %s",
					MessageHelper.collectionInfoString( this, collection, id, session )
			);
		}

		try {
			// insert all the new entries
			collection.preInsert( this );
			Iterator<?> entries = collection.entries( this );
			Expectation expectation = Expectations.appropriateExpectation( getInsertCheckStyle() );
			boolean callable = isInsertCallable();
			boolean useBatch = expectation.canBeBatched();
			String sql = getSQLInsertRowString();
			int i = 0;
			int count = 0;
			while ( entries.hasNext() ) {
				int offset = 1;
				Object entry = entries.next();
				PreparedStatement st;
				if ( collection.needsInserting( entry, i, elementType ) ) {

					if ( useBatch ) {
						if ( insertBatchKey == null ) {
							insertBatchKey = new BasicBatchKey(
									getRole() + "#INSERT",
									expectation
									);
						}
						st = session
								.getJdbcCoordinator()
								.getBatch( insertBatchKey )
								.getBatchStatement( sql, callable );
					}
					else {
						st = session
								.getJdbcCoordinator()
								.getStatementPreparer()
								.prepareStatement( sql, callable );
					}

					try {
						offset += expectation.prepare( st );
						// TODO: copy/paste from recreate()
						offset = writeKey( st, id, offset, session );
						if ( hasIdentifier ) {
							offset = writeIdentifier( st, collection.getIdentifier( entry, i ), offset, session );
						}
						if ( hasIndex /* && !indexIsFormula */) {
							offset = writeIndex( st, collection.getIndex( entry, i, this ), offset, session );
						}
						writeElement( st, collection.getElement( entry ), offset, session );

						if ( useBatch ) {
							session.getJdbcCoordinator().getBatch( insertBatchKey ).addToBatch();
						}
						else {
							expectation.verifyOutcome( session.getJdbcCoordinator().getResultSetReturn().executeUpdate( st ), st, -1, sql );
						}
						collection.afterRowInsert( this, entry, i );
						count++;
					}
					catch ( SQLException sqle ) {
						if ( useBatch ) {
							session.getJdbcCoordinator().abortBatch();
						}
						throw sqle;
					}
					finally {
						if ( !useBatch ) {
							session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
							session.getJdbcCoordinator().afterStatementExecution();
						}
					}
				}
				i++;
			}
			LOG.debugf( "Done inserting rows: %s inserted", count );
		}
		catch ( SQLException sqle ) {
			throw sqlExceptionHelper.convert(
					sqle,
					"could not insert collection rows: " +
							MessageHelper.collectionInfoString( this, collection, id, session ),
					getSQLInsertRowString()
			);
		}
	}

	@Override
	public String getRole() {
		return navigableRole.getFullPath();
	}

	public String getOwnerEntityName() {
		return entityName;
	}

	@Override
	public EntityPersister getOwnerEntityPersister() {
		return ownerPersister;
	}

	@Override
	public IdentifierGenerator getIdentifierGenerator() {
		return identifierGenerator;
	}

	@Override
	public Type getIdentifierType() {
		return identifierType;
	}

	@Override
	public boolean hasOrphanDelete() {
		return hasOrphanDelete;
	}

	@Override
	public Type toType(String propertyName) throws QueryException {
		if ( "index".equals( propertyName ) ) {
			return indexType;
		}
		return elementPropertyMapping.toType( propertyName );
	}

	@Override
	public void applyBaseRestrictions(Consumer<Predicate> predicateConsumer, TableGroup tableGroup, boolean useQualifier, Map<String, Filter> enabledFilters, Set<String> treatAsDeclarations, SqlAstCreationState creationState) {
		applyFilterRestrictions( predicateConsumer, tableGroup, useQualifier, enabledFilters, creationState );
		applyWhereRestrictions( predicateConsumer, tableGroup, useQualifier, creationState );
	}

	@Override
	public void applyWhereRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			SqlAstCreationState creationState) {
		TableReference tableReference;
		if ( isManyToMany() ) {
			tableReference = tableGroup.getPrimaryTableReference();
		}
		else if ( elementPersister instanceof Joinable ) {
			tableReference = tableGroup.getTableReference( tableGroup.getNavigablePath(), ( (Joinable) elementPersister ).getTableName() );
		}
		else {
			tableReference = tableGroup.getTableReference( tableGroup.getNavigablePath(), qualifiedTableName );
		}

		final String alias;
		if ( tableReference == null ) {
			alias = null;
		}
		else if ( useQualifier && tableReference.getIdentificationVariable() != null ) {
			alias = tableReference.getIdentificationVariable();
		}
		else {
			alias = tableReference.getTableId();
		}

		applyWhereFragments( predicateConsumer, alias, tableGroup, creationState );
	}

	protected void applyWhereFragments(
			Consumer<Predicate> predicateConsumer,
			String alias,
			TableGroup tableGroup,
			SqlAstCreationState astCreationState) {
		applyWhereFragments( predicateConsumer, alias, sqlWhereStringTemplate );
	}

	/**
	 * Applies all defined {@link org.hibernate.annotations.Where}
	 */
	private static void applyWhereFragments(Consumer<Predicate> predicateConsumer, String alias, String template) {
		if ( template == null ) {
			return;
		}

		final String fragment = StringHelper.replace( template, Template.TEMPLATE, alias );
		if ( StringHelper.isEmpty( fragment ) ) {
			return;
		}

		predicateConsumer.accept( new SqlFragmentPredicate( fragment ) );
	}

	@Override
	public void applyFilterRestrictions(Consumer<Predicate> predicateConsumer, TableGroup tableGroup, boolean useQualifier, Map<String, Filter> enabledFilters, SqlAstCreationState creationState) {
		if ( filterHelper != null ) {
			filterHelper.applyEnabledFilters(
					predicateConsumer,
					getFilterAliasGenerator( tableGroup ),
					enabledFilters
			);
		}
	}

	@Override
	public abstract boolean isManyToMany();

	@Override
	public void applyBaseManyToManyRestrictions(Consumer<Predicate> predicateConsumer, TableGroup tableGroup, boolean useQualifier, Map<String, Filter> enabledFilters, Set<String> treatAsDeclarations, SqlAstCreationState creationState) {
		if ( manyToManyFilterHelper == null && manyToManyWhereTemplate == null ) {
			return;
		}


		if ( manyToManyFilterHelper != null ) {
			final FilterAliasGenerator aliasGenerator = elementPersister.getFilterAliasGenerator( tableGroup );
			manyToManyFilterHelper.applyEnabledFilters( predicateConsumer, aliasGenerator, enabledFilters );
		}

		if ( manyToManyWhereString != null ) {
			final TableReference tableReference = tableGroup.resolveTableReference( ( (Joinable) elementPersister ).getTableName() );

			final String alias;
			if ( tableReference == null ) {
				alias = null;
			}
			else if ( useQualifier && tableReference.getIdentificationVariable() != null ) {
				alias = tableReference.getIdentificationVariable();
			}
			else {
				alias = tableReference.getTableId();
			}

			applyWhereFragments( predicateConsumer, alias, manyToManyWhereTemplate );
		}
	}

	@Override
	public String getManyToManyFilterFragment(TableGroup tableGroup, Map<String, Filter> enabledFilters) {
		final StringBuilder fragment = new StringBuilder();

		if ( manyToManyFilterHelper != null ) {
			manyToManyFilterHelper.render( fragment, elementPersister.getFilterAliasGenerator( tableGroup ), enabledFilters );
		}

		if ( manyToManyWhereString != null ) {
			if ( fragment.length() > 0 ) {
				fragment.append( " and " );
			}
			assert elementPersister instanceof Joinable;
			final TableReference tableReference = tableGroup.resolveTableReference( ( (Joinable) elementPersister ).getTableName() );
			fragment.append( StringHelper.replace( manyToManyWhereTemplate, Template.TEMPLATE, tableReference.getIdentificationVariable() ) );
		}

		return fragment.toString();
	}

	private String[] indexFragments;

	@Override
	public String[] toColumns(String propertyName) throws QueryException {
		if ( "index".equals( propertyName ) ) {
			if ( indexFragments == null ) {
				String[] tmp = new String[indexColumnNames.length];
				for ( int i = 0; i < indexColumnNames.length; i++ ) {
					tmp[i] = indexColumnNames[i] == null
							? indexFormulas[i]
							: indexColumnNames[i];
					indexFragments = tmp;
				}
			}
			return indexFragments;
		}

		return elementPropertyMapping.toColumns( propertyName );
	}

	@Override
	public String getName() {
		return getRole();
	}

	@Override
	public EntityPersister getElementPersister() {
		if ( elementPersister == null ) {
			throw new AssertionFailure( "not an association" );
		}
		return elementPersister;
	}

	protected EntityPersister getElementPersisterInternal() {
		return elementPersister;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public Serializable[] getCollectionSpaces() {
		return spaces;
	}

	protected abstract String generateDeleteString();

	protected abstract String generateDeleteRowString();

	protected abstract String generateUpdateRowString();

	protected abstract String generateInsertRowString();

	@Override
	public void updateRows(PersistentCollection<?> collection, Object id, SharedSessionContractImplementor session)
			throws HibernateException {

		if ( !isInverse && collection.isRowUpdatePossible() ) {

			LOG.debugf( "Updating rows of collection: %s#%s", navigableRole.getFullPath(), id );

			// update all the modified entries
			int count = doUpdateRows( id, collection, session );

			LOG.debugf( "Done updating rows: %s updated", count );
		}
	}

	protected abstract int doUpdateRows(Object key, PersistentCollection<?> collection, SharedSessionContractImplementor session);

	@Override
	public void processQueuedOps(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session) {
		if ( collection.hasQueuedOperations() ) {
			doProcessQueuedOps( collection, key, session );
		}
	}

//	/**
//	 * Process queued operations within the PersistentCollection.
//	 *
//	 * @param collection The collection
//	 * @param key The collection key
//	 * @param nextIndex The next index to write
//	 * @param session The session
//	 * @deprecated Use {@link #doProcessQueuedOps(PersistentCollection, Object, SharedSessionContractImplementor)}
//	 */
//	@Deprecated
//	protected void doProcessQueuedOps(PersistentCollection<?> collection, Object key,
//			int nextIndex, SharedSessionContractImplementor session)
//			throws HibernateException {
//		doProcessQueuedOps( collection, key, session );
//	}

	protected abstract void doProcessQueuedOps(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session)
			throws HibernateException;

	@Override
	public CollectionMetadata getCollectionMetadata() {
		return this;
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	protected boolean isInsertCallable() {
		return insertCallable;
	}

	protected ExecuteUpdateResultCheckStyle getInsertCheckStyle() {
		return insertCheckStyle;
	}

	protected boolean isUpdateCallable() {
		return updateCallable;
	}

	protected ExecuteUpdateResultCheckStyle getUpdateCheckStyle() {
		return updateCheckStyle;
	}

	protected boolean isDeleteCallable() {
		return deleteCallable;
	}

	protected ExecuteUpdateResultCheckStyle getDeleteCheckStyle() {
		return deleteCheckStyle;
	}

	protected boolean isDeleteAllCallable() {
		return deleteAllCallable;
	}

	protected ExecuteUpdateResultCheckStyle getDeleteAllCheckStyle() {
		return deleteAllCheckStyle;
	}

	@Override
	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + '(' + navigableRole.getFullPath() + ')';
	}

	@Override
	public boolean isVersioned() {
		return isVersioned && getOwnerEntityPersister().isVersioned();
	}

	// TODO: deprecate???
	protected SQLExceptionConverter getSQLExceptionConverter() {
		return getSQLExceptionHelper().getSqlExceptionConverter();
	}

	// TODO: needed???
	protected SqlExceptionHelper getSQLExceptionHelper() {
		return sqlExceptionHelper;
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		return cacheEntryStructure;
	}

	@Override
	public boolean isAffectedByEnabledFilters(SharedSessionContractImplementor session) {
		return isAffectedByEnabledFilters( session.getLoadQueryInfluencers() );
	}

	public boolean isSubselectLoadable() {
		return subselectLoadable;
	}

	@Override
	public boolean isMutable() {
		return isMutable;
	}

	@Override
	public String[] getCollectionPropertyColumnAliases(String propertyName, String suffix) {
		String[] rawAliases = collectionPropertyColumnAliases.get( propertyName );

		if ( rawAliases == null ) {
			return null;
		}

		String[] result = new String[rawAliases.length];
		final Alias alias = new Alias( suffix );
		for ( int i = 0; i < rawAliases.length; i++ ) {
			result[i] = alias.toUnquotedAliasString( rawAliases[i] );
		}
		return result;
	}

	// TODO: formulas ?
	public void initCollectionPropertyMap() {

		initCollectionPropertyMap( "key", keyType, keyColumnAliases );
		initCollectionPropertyMap( "element", elementType, elementColumnAliases );
		if ( hasIndex ) {
			initCollectionPropertyMap( "index", indexType, indexColumnAliases );
		}
		if ( hasIdentifier ) {
			initCollectionPropertyMap( "id", identifierType, new String[] { identifierColumnAlias } );
		}
	}

	private void initCollectionPropertyMap(String aliasName, Type type, String[] columnAliases) {

		collectionPropertyColumnAliases.put( aliasName, columnAliases );

		//TODO: this code is almost certainly obsolete and can be removed
		if ( type.isComponentType() ) {
			CompositeType ct = (CompositeType) type;
			String[] propertyNames = ct.getPropertyNames();
			for ( int i = 0; i < propertyNames.length; i++ ) {
				String name = propertyNames[i];
				collectionPropertyColumnAliases.put( aliasName + "." + name, new String[] {columnAliases[i]} );
			}
		}

	}

	@Override
	public int getSize(Object key, SharedSessionContractImplementor session) {
		try {
			final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
			PreparedStatement st = jdbcCoordinator
					.getStatementPreparer()
					.prepareStatement( sqlSelectSizeString );
			try {
				getKeyType().nullSafeSet( st, key, 1, session );
				ResultSet rs = jdbcCoordinator.getResultSetReturn().extract( st );
				try {
					return rs.next() ? rs.getInt( 1 ) - baseIndex : 0;
				}
				finally {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( rs, st );
				}
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
				jdbcCoordinator.afterStatementExecution();
			}
		}
		catch ( SQLException sqle ) {
			throw getSQLExceptionHelper().convert(
					sqle,
					"could not retrieve collection size: " +
							MessageHelper.collectionInfoString( this, key, getFactory() ),
					sqlSelectSizeString
			);
		}
	}

	@Override
	public boolean indexExists(Object key, Object index, SharedSessionContractImplementor session) {
		return exists( key, incrementIndexByBase( index ), getIndexType(), sqlDetectRowByIndexString, session );
	}

	@Override
	public boolean elementExists(Object key, Object element, SharedSessionContractImplementor session) {
		return exists( key, element, getElementType(), sqlDetectRowByElementString, session );
	}

	private boolean exists(Object key, Object indexOrElement, Type indexOrElementType, String sql, SharedSessionContractImplementor session) {
		try {
			final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
			PreparedStatement st = jdbcCoordinator
					.getStatementPreparer()
					.prepareStatement( sql );
			try {
				getKeyType().nullSafeSet( st, key, 1, session );
				indexOrElementType.nullSafeSet( st, indexOrElement, keyColumnNames.length + 1, session );
				ResultSet rs = jdbcCoordinator.getResultSetReturn().extract( st );
				try {
					return rs.next();
				}
				finally {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( rs, st );
				}
			}
			catch ( TransientObjectException e ) {
				return false;
			}
			finally {
				jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( st );
				jdbcCoordinator.afterStatementExecution();
			}
		}
		catch ( SQLException sqle ) {
			throw getSQLExceptionHelper().convert(
					sqle,
					"could not check row existence: " +
							MessageHelper.collectionInfoString( this, key, getFactory() ),
					sqlSelectSizeString
			);
		}
	}

	@Override
	public Object getElementByIndex(Object key, Object index, SharedSessionContractImplementor session, Object owner) {
		return collectionElementLoaderByIndex.load( key, index, session );
	}

	@Override
	public boolean isExtraLazy() {
		return isExtraLazy;
	}

	protected Dialect getDialect() {
		return dialect;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public String getMappedByProperty() {
		return mappedByProperty;
	}

//	private class StandardOrderByAliasResolver implements OrderByAliasResolver {
//		private final String rootAlias;
//
//		private StandardOrderByAliasResolver(String rootAlias) {
//			this.rootAlias = rootAlias;
//		}
//
//		@Override
//		public String resolveTableAlias(String columnReference) {
//			if ( elementPersister == null ) {
//				// we have collection of non-entity elements...
//				return rootAlias;
//			}
//			else {
//				return ( (Loadable) elementPersister ).getTableAliasForColumn( columnReference, rootAlias );
//			}
//		}
//	}

	public abstract FilterAliasGenerator getFilterAliasGenerator(String rootAlias);

	public abstract FilterAliasGenerator getFilterAliasGenerator(TableGroup tableGroup);

	// ColectionDefinition impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public CollectionPersister getCollectionPersister() {
		return this;
	}

	@Override
	public CollectionIndexDefinition getIndexDefinition() {
		if ( ! hasIndex() ) {
			return null;
		}

		return new CollectionIndexDefinition() {
			@Override
			public CollectionDefinition getCollectionDefinition() {
				return AbstractCollectionPersister.this;
			}

			@Override
			public Type getType() {
				return getIndexType();
			}

			@Override
			public EntityDefinition toEntityDefinition() {
				if ( !getType().isEntityType() ) {
					throw new IllegalStateException( "Cannot treat collection index type as entity" );
				}
				return (EntityPersister) ( (AssociationType) getIndexType() ).getAssociatedJoinable( getFactory() );
			}

			@Override
			public CompositionDefinition toCompositeDefinition() {
				if ( ! getType().isComponentType() ) {
					throw new IllegalStateException( "Cannot treat collection index type as composite" );
				}
				return new CompositeCollectionElementDefinition() {
					@Override
					public String getName() {
						return "index";
					}

					@Override
					public CompositeType getType() {
						return (CompositeType) getIndexType();
					}

					@Override
					public boolean isNullable() {
						return false;
					}

					@Override
					public AttributeSource getSource() {
						// TODO: what if this is a collection w/in an encapsulated composition attribute?
						// should return the encapsulated composition attribute instead???
						return getOwnerEntityPersister();
					}

					@Override
					public Iterable<AttributeDefinition> getAttributes() {
						return CompositionSingularSubAttributesHelper.getCompositeCollectionIndexSubAttributes( this );
					}
					@Override
					public CollectionDefinition getCollectionDefinition() {
						return AbstractCollectionPersister.this;
					}
				};
			}

			@Override
			public AnyMappingDefinition toAnyMappingDefinition() {
				final Type type = getType();
				if ( ! type.isAnyType() ) {
					throw new IllegalStateException( "Cannot treat collection index type as ManyToAny" );
				}
				return new StandardAnyTypeDefinition( (AnyType) type, isLazy() || isExtraLazy() );
			}
		};
	}

	@Override
	public CollectionElementDefinition getElementDefinition() {
		return new CollectionElementDefinition() {
			@Override
			public CollectionDefinition getCollectionDefinition() {
				return AbstractCollectionPersister.this;
			}

			@Override
			public Type getType() {
				return getElementType();
			}

			@Override
			public AnyMappingDefinition toAnyMappingDefinition() {
				final Type type = getType();
				if ( ! type.isAnyType() ) {
					throw new IllegalStateException( "Cannot treat collection element type as ManyToAny" );
				}
				return new StandardAnyTypeDefinition( (AnyType) type, isLazy() || isExtraLazy() );
			}

			@Override
			public EntityDefinition toEntityDefinition() {
				if ( !getType().isEntityType() ) {
					throw new IllegalStateException( "Cannot treat collection element type as entity" );
				}
				return getElementPersister();
			}

			@Override
			public CompositeCollectionElementDefinition toCompositeElementDefinition() {

				if ( ! getType().isComponentType() ) {
					throw new IllegalStateException( "Cannot treat entity collection element type as composite" );
				}

				return new CompositeCollectionElementDefinition() {
					@Override
					public String getName() {
						return "";
					}

					@Override
					public CompositeType getType() {
						return (CompositeType) getElementType();
					}

					@Override
					public boolean isNullable() {
						return false;
					}

					@Override
					public AttributeSource getSource() {
						// TODO: what if this is a collection w/in an encapsulated composition attribute?
						// should return the encapsulated composition attribute instead???
						return getOwnerEntityPersister();
					}

					@Override
					public Iterable<AttributeDefinition> getAttributes() {
						return CompositionSingularSubAttributesHelper.getCompositeCollectionElementSubAttributes( this );
					}

					@Override
					public CollectionDefinition getCollectionDefinition() {
						return AbstractCollectionPersister.this;
					}
				};
			}
		};
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// "mapping model"

	// todo (6.0) : atm there is no way to get a `PluralAttributeMapping` reference except through its declaring `ManagedTypeMapping` attributes.  this is a backhand way
	//		of getting access to it for use from the persister

	private PluralAttributeMapping attributeMapping;

	@Override
	public void injectAttributeMapping(PluralAttributeMapping attributeMapping) {
		this.attributeMapping = attributeMapping;
	}

	@Override
	public PluralAttributeMapping getAttributeMapping() {
		return attributeMapping;
	}

	@Override
	public boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers) {
		if ( influencers.hasEnabledFilters() ) {
			final Map<String, Filter> enabledFilters = influencers.getEnabledFilters();
			return ( filterHelper != null && filterHelper.isAffectedBy( enabledFilters ) )
					|| ( manyToManyFilterHelper != null && manyToManyFilterHelper.isAffectedBy( enabledFilters ) );
		}

		return false;
	}

	@Override
	public boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers) {
		// todo (6.0) : anything to do here?
		return false;
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers) {
		if ( influencers.hasEnabledFetchProfiles() ) {
			for ( String enabledFetchProfileName : influencers.getEnabledFetchProfileNames() ) {
				final FetchProfile fetchProfile = getFactory().getFetchProfile( enabledFetchProfileName );
				final Fetch fetch = fetchProfile.getFetchByRole( getRole() );
				if ( fetch != null && fetch.getStyle() == Fetch.Style.JOIN ) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public CollectionSemantics<?,?> getCollectionSemantics() {
		return collectionSemantics;
	}

	protected Insert createInsert() {
		return new Insert( getFactory().getJdbcServices().getDialect() );
	}

	protected Update createUpdate() {
		return new Update( getFactory().getJdbcServices().getDialect() );
	}

	protected Delete createDelete() {
		return new Delete();
	}
}
