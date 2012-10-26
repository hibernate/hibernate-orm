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
package org.hibernate.persister.collection;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.TransientObjectException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.StructuredCollectionCacheEntry;
import org.hibernate.cache.spi.entry.StructuredMapCacheEntry;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.FilterHelper;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Alias;
import org.hibernate.sql.SelectFragment;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.Template;
import org.hibernate.sql.ordering.antlr.ColumnMapper;
import org.hibernate.sql.ordering.antlr.ColumnReference;
import org.hibernate.sql.ordering.antlr.FormulaReference;
import org.hibernate.sql.ordering.antlr.OrderByAliasResolver;
import org.hibernate.sql.ordering.antlr.OrderByFragmentParser;
import org.hibernate.sql.ordering.antlr.OrderByTranslation;
import org.hibernate.sql.ordering.antlr.SqlValueReference;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Base implementation of the <tt>QueryableCollection</tt> interface.
 * 
 * @author Gavin King
 * @see BasicCollectionPersister
 * @see OneToManyPersister
 */
public abstract class AbstractCollectionPersister
		implements CollectionMetadata, SQLLoadableCollection {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class,
			AbstractCollectionPersister.class.getName() );

	// TODO: encapsulate the protected instance variables!

	private final String role;

	// SQL statements
	private final String sqlDeleteString;
	private final String sqlInsertRowString;
	private final String sqlUpdateRowString;
	private final String sqlDeleteRowString;
	private final String sqlSelectSizeString;
	private final String sqlSelectRowByIndexString;
	private final String sqlDetectRowByIndexString;
	private final String sqlDetectRowByElementString;

	protected final boolean hasWhere;
	protected final String sqlWhereString;
	private final String sqlWhereStringTemplate;

	private final boolean hasOrder;
	private final OrderByTranslation orderByTranslation;

	private final boolean hasManyToManyOrder;
	private final OrderByTranslation manyToManyOrderByTranslation;

	private final int baseIndex;

	private final String nodeName;
	private final String elementNodeName;
	private final String indexNodeName;

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
	protected final boolean[] indexColumnIsSettable;
	protected final String[] elementColumnNames;
	protected final String[] elementColumnWriters;
	protected final String[] elementColumnReaders;
	protected final String[] elementColumnReaderTemplates;
	protected final String[] elementFormulaTemplates;
	protected final String[] elementFormulas;
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
	private final boolean isInverse;
	private final boolean isMutable;
	private final boolean isVersioned;
	protected final int batchSize;
	private final FetchMode fetchMode;
	private final boolean hasOrphanDelete;
	private final boolean subselectLoadable;

	// extra information about the element type
	private final Class elementClass;
	private final String entityName;

	private final Dialect dialect;
	private final SqlExceptionHelper sqlExceptionHelper;
	private final SessionFactoryImplementor factory;
	private final EntityPersister ownerPersister;
	private final IdentifierGenerator identifierGenerator;
	private final PropertyMapping elementPropertyMapping;
	private final EntityPersister elementPersister;
	private final CollectionRegionAccessStrategy cacheAccessStrategy;
	private final CollectionType collectionType;
	private CollectionInitializer initializer;

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
	private ExecuteUpdateResultCheckStyle insertCheckStyle;
	private ExecuteUpdateResultCheckStyle updateCheckStyle;
	private ExecuteUpdateResultCheckStyle deleteCheckStyle;
	private ExecuteUpdateResultCheckStyle deleteAllCheckStyle;

	private final Serializable[] spaces;

	private Map collectionPropertyColumnAliases = new HashMap();
	private Map collectionPropertyColumnNames = new HashMap();

	public AbstractCollectionPersister(
			final Collection collection,
			final CollectionRegionAccessStrategy cacheAccessStrategy,
			final Configuration cfg,
			final SessionFactoryImplementor factory) throws MappingException, CacheException {

		this.factory = factory;
		this.cacheAccessStrategy = cacheAccessStrategy;
		if ( factory.getSettings().isStructuredCacheEntriesEnabled() ) {
			cacheEntryStructure = collection.isMap() ?
					(CacheEntryStructure) new StructuredMapCacheEntry() :
					(CacheEntryStructure) new StructuredCollectionCacheEntry();
		}
		else {
			cacheEntryStructure = new UnstructuredCacheEntry();
		}

		dialect = factory.getDialect();
		sqlExceptionHelper = factory.getSQLExceptionHelper();
		collectionType = collection.getCollectionType();
		role = collection.getRole();
		entityName = collection.getOwnerEntityName();
		ownerPersister = factory.getEntityPersister( entityName );
		queryLoaderName = collection.getLoaderName();
		nodeName = collection.getNodeName();
		isMutable = collection.isMutable();

		Table table = collection.getCollectionTable();
		fetchMode = collection.getElement().getFetchMode();
		elementType = collection.getElement().getType();
		// isSet = collection.isSet();
		// isSorted = collection.isSorted();
		isPrimitiveArray = collection.isPrimitiveArray();
		isArray = collection.isArray();
		subselectLoadable = collection.isSubselectLoadable();

		qualifiedTableName = table.getQualifiedName(
				dialect,
				factory.getSettings().getDefaultCatalogName(),
				factory.getSettings().getDefaultSchemaName()
				);

		int spacesSize = 1 + collection.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = qualifiedTableName;
		Iterator iter = collection.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = (String) iter.next();
		}

		sqlWhereString = StringHelper.isNotEmpty( collection.getWhere() ) ? "( " + collection.getWhere() + ") " : null;
		hasWhere = sqlWhereString != null;
		sqlWhereStringTemplate = hasWhere ?
				Template.renderWhereStringTemplate( sqlWhereString, dialect, factory.getSqlFunctionRegistry() ) :
				null;

		hasOrphanDelete = collection.hasOrphanDelete();

		int batch = collection.getBatchSize();
		if ( batch == -1 ) {
			batch = factory.getSettings().getDefaultBatchFetchSize();
		}
		batchSize = batch;

		isVersioned = collection.isOptimisticLocked();

		// KEY

		keyType = collection.getKey().getType();
		iter = collection.getKey().getColumnIterator();
		int keySpan = collection.getKey().getColumnSpan();
		keyColumnNames = new String[keySpan];
		keyColumnAliases = new String[keySpan];
		int k = 0;
		while ( iter.hasNext() ) {
			// NativeSQL: collect key column and auto-aliases
			Column col = ( (Column) iter.next() );
			keyColumnNames[k] = col.getQuotedName( dialect );
			keyColumnAliases[k] = col.getAlias( dialect, collection.getOwner().getRootTable() );
			k++;
		}

		// unquotedKeyColumnNames = StringHelper.unQuote(keyColumnAliases);

		// ELEMENT

		String elemNode = collection.getElementNodeName();
		if ( elementType.isEntityType() ) {
			String entityName = ( (EntityType) elementType ).getAssociatedEntityName();
			elementPersister = factory.getEntityPersister( entityName );
			if ( elemNode == null ) {
				elemNode = cfg.getClassMapping( entityName ).getNodeName();
			}
			// NativeSQL: collect element column and auto-aliases

		}
		else {
			elementPersister = null;
		}
		elementNodeName = elemNode;

		int elementSpan = collection.getElement().getColumnSpan();
		elementColumnAliases = new String[elementSpan];
		elementColumnNames = new String[elementSpan];
		elementColumnWriters = new String[elementSpan];
		elementColumnReaders = new String[elementSpan];
		elementColumnReaderTemplates = new String[elementSpan];
		elementFormulaTemplates = new String[elementSpan];
		elementFormulas = new String[elementSpan];
		elementColumnIsSettable = new boolean[elementSpan];
		elementColumnIsInPrimaryKey = new boolean[elementSpan];
		boolean isPureFormula = true;
		boolean hasNotNullableColumns = false;
		int j = 0;
		iter = collection.getElement().getColumnIterator();
		while ( iter.hasNext() ) {
			Selectable selectable = (Selectable) iter.next();
			elementColumnAliases[j] = selectable.getAlias( dialect, table );
			if ( selectable.isFormula() ) {
				Formula form = (Formula) selectable;
				elementFormulaTemplates[j] = form.getTemplate( dialect, factory.getSqlFunctionRegistry() );
				elementFormulas[j] = form.getFormula();
			}
			else {
				Column col = (Column) selectable;
				elementColumnNames[j] = col.getQuotedName( dialect );
				elementColumnWriters[j] = col.getWriteExpr();
				elementColumnReaders[j] = col.getReadExpr( dialect );
				elementColumnReaderTemplates[j] = col.getTemplate( dialect, factory.getSqlFunctionRegistry() );
				elementColumnIsSettable[j] = true;
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

		hasIndex = collection.isIndexed();
		if ( hasIndex ) {
			// NativeSQL: collect index column and auto-aliases
			IndexedCollection indexedCollection = (IndexedCollection) collection;
			indexType = indexedCollection.getIndex().getType();
			int indexSpan = indexedCollection.getIndex().getColumnSpan();
			iter = indexedCollection.getIndex().getColumnIterator();
			indexColumnNames = new String[indexSpan];
			indexFormulaTemplates = new String[indexSpan];
			indexFormulas = new String[indexSpan];
			indexColumnIsSettable = new boolean[indexSpan];
			indexColumnAliases = new String[indexSpan];
			int i = 0;
			boolean hasFormula = false;
			while ( iter.hasNext() ) {
				Selectable s = (Selectable) iter.next();
				indexColumnAliases[i] = s.getAlias( dialect );
				if ( s.isFormula() ) {
					Formula indexForm = (Formula) s;
					indexFormulaTemplates[i] = indexForm.getTemplate( dialect, factory.getSqlFunctionRegistry() );
					indexFormulas[i] = indexForm.getFormula();
					hasFormula = true;
				}
				else {
					Column indexCol = (Column) s;
					indexColumnNames[i] = indexCol.getQuotedName( dialect );
					indexColumnIsSettable[i] = true;
				}
				i++;
			}
			indexContainsFormula = hasFormula;
			baseIndex = indexedCollection.isList() ?
					( (List) indexedCollection ).getBaseIndex() : 0;

			indexNodeName = indexedCollection.getIndexNodeName();

		}
		else {
			indexContainsFormula = false;
			indexColumnIsSettable = null;
			indexFormulaTemplates = null;
			indexFormulas = null;
			indexType = null;
			indexColumnNames = null;
			indexColumnAliases = null;
			baseIndex = 0;
			indexNodeName = null;
		}

		hasIdentifier = collection.isIdentified();
		if ( hasIdentifier ) {
			if ( collection.isOneToMany() ) {
				throw new MappingException( "one-to-many collections with identifiers are not supported" );
			}
			IdentifierCollection idColl = (IdentifierCollection) collection;
			identifierType = idColl.getIdentifier().getType();
			iter = idColl.getIdentifier().getColumnIterator();
			Column col = (Column) iter.next();
			identifierColumnName = col.getQuotedName( dialect );
			identifierColumnAlias = col.getAlias( dialect );
			// unquotedIdentifierColumnName = identifierColumnAlias;
			identifierGenerator = idColl.getIdentifier().createIdentifierGenerator(
					cfg.getIdentifierGeneratorFactory(),
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName(),
					null
					);
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

		if ( collection.getCustomSQLInsert() == null ) {
			sqlInsertRowString = generateInsertRowString();
			insertCallable = false;
			insertCheckStyle = ExecuteUpdateResultCheckStyle.COUNT;
		}
		else {
			sqlInsertRowString = collection.getCustomSQLInsert();
			insertCallable = collection.isCustomInsertCallable();
			insertCheckStyle = collection.getCustomSQLInsertCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( collection.getCustomSQLInsert(), insertCallable )
					: collection.getCustomSQLInsertCheckStyle();
		}

		if ( collection.getCustomSQLUpdate() == null ) {
			sqlUpdateRowString = generateUpdateRowString();
			updateCallable = false;
			updateCheckStyle = ExecuteUpdateResultCheckStyle.COUNT;
		}
		else {
			sqlUpdateRowString = collection.getCustomSQLUpdate();
			updateCallable = collection.isCustomUpdateCallable();
			updateCheckStyle = collection.getCustomSQLUpdateCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( collection.getCustomSQLUpdate(), insertCallable )
					: collection.getCustomSQLUpdateCheckStyle();
		}

		if ( collection.getCustomSQLDelete() == null ) {
			sqlDeleteRowString = generateDeleteRowString();
			deleteCallable = false;
			deleteCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}
		else {
			sqlDeleteRowString = collection.getCustomSQLDelete();
			deleteCallable = collection.isCustomDeleteCallable();
			deleteCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}

		if ( collection.getCustomSQLDeleteAll() == null ) {
			sqlDeleteString = generateDeleteString();
			deleteAllCallable = false;
			deleteAllCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}
		else {
			sqlDeleteString = collection.getCustomSQLDeleteAll();
			deleteAllCallable = collection.isCustomDeleteAllCallable();
			deleteAllCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}

		sqlSelectSizeString = generateSelectSizeString( collection.isIndexed() && !collection.isMap() );
		sqlDetectRowByIndexString = generateDetectRowByIndexString();
		sqlDetectRowByElementString = generateDetectRowByElementString();
		sqlSelectRowByIndexString = generateSelectRowByIndexString();

		logStaticSQL();

		isLazy = collection.isLazy();
		isExtraLazy = collection.isExtraLazy();

		isInverse = collection.isInverse();

		if ( collection.isArray() ) {
			elementClass = ( (org.hibernate.mapping.Array) collection ).getElementClass();
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
			if ( elementPersister instanceof PropertyMapping ) { // not all classpersisters implement PropertyMapping!
				elementPropertyMapping = (PropertyMapping) elementPersister;
			}
			else {
				elementPropertyMapping = new ElementPropertyMapping(
						elementColumnNames,
						elementType
						);
			}
		}

		hasOrder = collection.getOrderBy() != null;
		if ( hasOrder ) {
			orderByTranslation = Template.translateOrderBy(
					collection.getOrderBy(),
					new ColumnMapperImpl(),
					factory,
					dialect,
					factory.getSqlFunctionRegistry()
			);
		}
		else {
			orderByTranslation = null;
		}

		// Handle any filters applied to this collection
		filterHelper = new FilterHelper( collection.getFilters(), factory);

		// Handle any filters applied to this collection for many-to-many
		manyToManyFilterHelper = new FilterHelper( collection.getManyToManyFilters(), factory);
		manyToManyWhereString = StringHelper.isNotEmpty( collection.getManyToManyWhere() ) ?
				"( " + collection.getManyToManyWhere() + ")" :
				null;
		manyToManyWhereTemplate = manyToManyWhereString == null ?
				null :
				Template.renderWhereStringTemplate( manyToManyWhereString, factory.getDialect(), factory.getSqlFunctionRegistry() );

		hasManyToManyOrder = collection.getManyToManyOrdering() != null;
		if ( hasManyToManyOrder ) {
			manyToManyOrderByTranslation = Template.translateOrderBy(
					collection.getManyToManyOrdering(),
					new ColumnMapperImpl(),
					factory,
					dialect,
					factory.getSqlFunctionRegistry()
			);
		}
		else {
			manyToManyOrderByTranslation = null;
		}

		initCollectionPropertyMap();
	}

	private class ColumnMapperImpl implements ColumnMapper {
		@Override
		public SqlValueReference[] map(String reference) {
			final String[] columnNames;
			final String[] formulaTemplates;

			// handle the special "$element$" property name...
			if ( "$element$".equals( reference ) ) {
				columnNames = elementColumnNames;
				formulaTemplates = elementFormulaTemplates;
			}
			else {
				columnNames = elementPropertyMapping.toColumns( reference );
				formulaTemplates = formulaTemplates( reference, columnNames.length );
			}

			final SqlValueReference[] result = new SqlValueReference[ columnNames.length ];
			int i = 0;
			for ( final String columnName : columnNames ) {
				if ( columnName == null ) {
					// if the column name is null, it indicates that this index in the property value mapping is
					// actually represented by a formula.
					final int propertyIndex = elementPersister.getEntityMetamodel().getPropertyIndex( reference );
					final String formulaTemplate = formulaTemplates[i];
					result[i] = new FormulaReference() {
						@Override
						public String getFormulaFragment() {
							return formulaTemplate;
						}
					};
				}
				else {
					result[i] = new ColumnReference() {
						@Override
						public String getColumnName() {
							return columnName;
						}
					};
				}
				i++;
			}
			return result;
		}
	}

	private String[] formulaTemplates(String reference, int expectedSize) {
		try {
			final int propertyIndex = elementPersister.getEntityMetamodel().getPropertyIndex( reference );
			return  ( (Queryable) elementPersister ).getSubclassPropertyFormulaTemplateClosure()[propertyIndex];
		}
		catch (Exception e) {
			return new String[expectedSize];
		}
	}

	public void postInstantiate() throws MappingException {
		initializer = queryLoaderName == null ?
				createCollectionInitializer( LoadQueryInfluencers.NONE ) :
				new NamedQueryCollectionInitializer( queryLoaderName, this );
	}

	protected void logStaticSQL() {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Static SQL for collection: %s", getRole() );
			if ( getSQLInsertRowString() != null ) LOG.debugf( " Row insert: %s", getSQLInsertRowString() );
			if ( getSQLUpdateRowString() != null ) LOG.debugf( " Row update: %s", getSQLUpdateRowString() );
			if ( getSQLDeleteRowString() != null ) LOG.debugf( " Row delete: %s", getSQLDeleteRowString() );
			if ( getSQLDeleteString() != null ) LOG.debugf( " One-shot delete: %s", getSQLDeleteString() );
		}
	}

	public void initialize(Serializable key, SessionImplementor session) throws HibernateException {
		getAppropriateInitializer( key, session ).initialize( key, session );
	}

	protected CollectionInitializer getAppropriateInitializer(Serializable key, SessionImplementor session) {
		if ( queryLoaderName != null ) {
			// if there is a user-specified loader, return that
			// TODO: filters!?
			return initializer;
		}
		CollectionInitializer subselectInitializer = getSubselectInitializer( key, session );
		if ( subselectInitializer != null ) {
			return subselectInitializer;
		}
		else if ( session.getEnabledFilters().isEmpty() ) {
			return initializer;
		}
		else {
			return createCollectionInitializer( session.getLoadQueryInfluencers() );
		}
	}

	private CollectionInitializer getSubselectInitializer(Serializable key, SessionImplementor session) {

		if ( !isSubselectLoadable() ) {
			return null;
		}

		final PersistenceContext persistenceContext = session.getPersistenceContext();

		SubselectFetch subselect = persistenceContext.getBatchFetchQueue()
				.getSubselect( session.generateEntityKey( key, getOwnerEntityPersister() ) );

		if ( subselect == null ) {
			return null;
		}
		else {

			// Take care of any entities that might have
			// been evicted!
			Iterator iter = subselect.getResult().iterator();
			while ( iter.hasNext() ) {
				if ( !persistenceContext.containsEntity( (EntityKey) iter.next() ) ) {
					iter.remove();
				}
			}

			// Run a subquery loader
			return createSubselectInitializer( subselect, session );
		}
	}

	protected abstract CollectionInitializer createSubselectInitializer(SubselectFetch subselect, SessionImplementor session);

	protected abstract CollectionInitializer createCollectionInitializer(LoadQueryInfluencers loadQueryInfluencers)
			throws MappingException;

	public CollectionRegionAccessStrategy getCacheAccessStrategy() {
		return cacheAccessStrategy;
	}

	public boolean hasCache() {
		return cacheAccessStrategy != null;
	}

	public CollectionType getCollectionType() {
		return collectionType;
	}

	protected String getSQLWhereString(String alias) {
		return StringHelper.replace( sqlWhereStringTemplate, Template.TEMPLATE, alias );
	}

	public String getSQLOrderByString(String alias) {
		return hasOrdering()
				? orderByTranslation.injectAliases( new StandardOrderByAliasResolver( alias ) )
				: "";
	}

	public String getManyToManyOrderByString(String alias) {
		return hasManyToManyOrdering()
				? manyToManyOrderByTranslation.injectAliases( new StandardOrderByAliasResolver( alias ) )
				: "";
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public boolean hasOrdering() {
		return hasOrder;
	}

	public boolean hasManyToManyOrdering() {
		return isManyToMany() && hasManyToManyOrder;
	}

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

	public Type getKeyType() {
		return keyType;
	}

	public Type getIndexType() {
		return indexType;
	}

	public Type getElementType() {
		return elementType;
	}

	/**
	 * Return the element class of an array, or null otherwise
	 */
	public Class getElementClass() { // needed by arrays
		return elementClass;
	}

	public Object readElement(ResultSet rs, Object owner, String[] aliases, SessionImplementor session)
			throws HibernateException, SQLException {
		return getElementType().nullSafeGet( rs, aliases, session, owner );
	}

	public Object readIndex(ResultSet rs, String[] aliases, SessionImplementor session)
			throws HibernateException, SQLException {
		Object index = getIndexType().nullSafeGet( rs, aliases, session, null );
		if ( index == null ) {
			throw new HibernateException( "null index column for collection: " + role );
		}
		index = decrementIndexByBase( index );
		return index;
	}

	protected Object decrementIndexByBase(Object index) {
		if ( baseIndex != 0 ) {
            index = (Integer)index - baseIndex;
		}
		return index;
	}

	public Object readIdentifier(ResultSet rs, String alias, SessionImplementor session)
			throws HibernateException, SQLException {
		Object id = getIdentifierType().nullSafeGet( rs, alias, session, null );
		if ( id == null ) {
			throw new HibernateException( "null identifier column for collection: " + role );
		}
		return id;
	}

	public Object readKey(ResultSet rs, String[] aliases, SessionImplementor session)
			throws HibernateException, SQLException {
		return getKeyType().nullSafeGet( rs, aliases, session, null );
	}

	/**
	 * Write the key to a JDBC <tt>PreparedStatement</tt>
	 */
	protected int writeKey(PreparedStatement st, Serializable key, int i, SessionImplementor session)
			throws HibernateException, SQLException {

		if ( key == null ) {
			throw new NullPointerException( "null key for collection: " + role ); // an assertion
		}
		getKeyType().nullSafeSet( st, key, i, session );
		return i + keyColumnAliases.length;
	}

	/**
	 * Write the element to a JDBC <tt>PreparedStatement</tt>
	 */
	protected int writeElement(PreparedStatement st, Object elt, int i, SessionImplementor session)
			throws HibernateException, SQLException {
		getElementType().nullSafeSet( st, elt, i, elementColumnIsSettable, session );
		return i + ArrayHelper.countTrue( elementColumnIsSettable );

	}

	/**
	 * Write the index to a JDBC <tt>PreparedStatement</tt>
	 */
	protected int writeIndex(PreparedStatement st, Object index, int i, SessionImplementor session)
			throws HibernateException, SQLException {
		getIndexType().nullSafeSet( st, incrementIndexByBase( index ), i, indexColumnIsSettable, session );
		return i + ArrayHelper.countTrue( indexColumnIsSettable );
	}

	protected Object incrementIndexByBase(Object index) {
		if ( baseIndex != 0 ) {
            index = (Integer)index + baseIndex;
		}
		return index;
	}

	/**
	 * Write the element to a JDBC <tt>PreparedStatement</tt>
	 */
	protected int writeElementToWhere(PreparedStatement st, Object elt, int i, SessionImplementor session)
			throws HibernateException, SQLException {
		if ( elementIsPureFormula ) {
			throw new AssertionFailure( "cannot use a formula-based element in the where condition" );
		}
		getElementType().nullSafeSet( st, elt, i, elementColumnIsInPrimaryKey, session );
		return i + elementColumnAliases.length;

	}

	/**
	 * Write the index to a JDBC <tt>PreparedStatement</tt>
	 */
	protected int writeIndexToWhere(PreparedStatement st, Object index, int i, SessionImplementor session)
			throws HibernateException, SQLException {
		if ( indexContainsFormula ) {
			throw new AssertionFailure( "cannot use a formula-based index in the where condition" );
		}
		getIndexType().nullSafeSet( st, incrementIndexByBase( index ), i, session );
		return i + indexColumnAliases.length;
	}

	/**
	 * Write the identifier to a JDBC <tt>PreparedStatement</tt>
	 */
	public int writeIdentifier(PreparedStatement st, Object id, int i, SessionImplementor session)
			throws HibernateException, SQLException {

		getIdentifierType().nullSafeSet( st, id, i, session );
		return i + 1;
	}

	public boolean isPrimitiveArray() {
		return isPrimitiveArray;
	}

	public boolean isArray() {
		return isArray;
	}

	public String[] getKeyColumnAliases(String suffix) {
		return new Alias( suffix ).toAliasStrings( keyColumnAliases );
	}

	public String[] getElementColumnAliases(String suffix) {
		return new Alias( suffix ).toAliasStrings( elementColumnAliases );
	}

	public String[] getIndexColumnAliases(String suffix) {
		if ( hasIndex ) {
			return new Alias( suffix ).toAliasStrings( indexColumnAliases );
		}
		else {
			return null;
		}
	}

	public String getIdentifierColumnAlias(String suffix) {
		if ( hasIdentifier ) {
			return new Alias( suffix ).toAliasString( identifierColumnAlias );
		}
		else {
			return null;
		}
	}

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
	public String selectFragment(String alias, String columnSuffix) {
		SelectFragment frag = generateSelectFragment( alias, columnSuffix );
		appendElementColumns( frag, alias );
		appendIndexColumns( frag, alias );
		appendIdentifierColumns( frag, alias );

		return frag.toFragmentString()
				.substring( 2 ); // strip leading ','
	}

	protected String generateSelectSizeString(boolean isIntegerIndexed) {
		String selectValue = isIntegerIndexed ?
				"max(" + getIndexColumnNames()[0] + ") + 1" : // lists, arrays
				"count(" + getElementColumnNames()[0] + ")"; // sets, maps, bags
		return new SimpleSelect( dialect )
				.setTableName( getTableName() )
				.addCondition( getKeyColumnNames(), "=?" )
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
				.addColumn( "1" )
				.toStatementString();
	}

	protected String generateSelectRowByIndexString() {
		if ( !hasIndex() ) {
			return null;
		}
		return new SimpleSelect( dialect )
				.setTableName( getTableName() )
				.addCondition( getKeyColumnNames(), "=?" )
				.addCondition( getIndexColumnNames(), "=?" )
				.addCondition( indexFormulas, "=?" )
				.addColumns( getElementColumnNames(), elementColumnAliases )
				.addColumns( indexFormulas, indexColumnAliases )
				.toStatementString();
	}

	protected String generateDetectRowByElementString() {
		return new SimpleSelect( dialect )
				.setTableName( getTableName() )
				.addCondition( getKeyColumnNames(), "=?" )
				.addCondition( getElementColumnNames(), "=?" )
				.addCondition( elementFormulas, "=?" )
				.addColumn( "1" )
				.toStatementString();
	}

	protected SelectFragment generateSelectFragment(String alias, String columnSuffix) {
		return new SelectFragment()
				.setSuffix( columnSuffix )
				.addColumns( alias, keyColumnNames, keyColumnAliases );
	}

	protected void appendElementColumns(SelectFragment frag, String elemAlias) {
		for ( int i = 0; i < elementColumnIsSettable.length; i++ ) {
			if ( elementColumnIsSettable[i] ) {
				frag.addColumnTemplate( elemAlias, elementColumnReaderTemplates[i], elementColumnAliases[i] );
			}
			else {
				frag.addFormula( elemAlias, elementFormulaTemplates[i], elementColumnAliases[i] );
			}
		}
	}

	protected void appendIndexColumns(SelectFragment frag, String alias) {
		if ( hasIndex ) {
			for ( int i = 0; i < indexColumnIsSettable.length; i++ ) {
				if ( indexColumnIsSettable[i] ) {
					frag.addColumn( alias, indexColumnNames[i], indexColumnAliases[i] );
				}
				else {
					frag.addFormula( alias, indexFormulaTemplates[i], indexColumnAliases[i] );
				}
			}
		}
	}

	protected void appendIdentifierColumns(SelectFragment frag, String alias) {
		if ( hasIdentifier ) {
			frag.addColumn( alias, identifierColumnName, identifierColumnAlias );
		}
	}

	public String[] getIndexColumnNames() {
		return indexColumnNames;
	}

	public String[] getIndexFormulas() {
		return indexFormulas;
	}

	public String[] getIndexColumnNames(String alias) {
		return qualify( alias, indexColumnNames, indexFormulaTemplates );

	}

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

	public String[] getElementColumnNames() {
		return elementColumnNames; // TODO: something with formulas...
	}

	public String[] getKeyColumnNames() {
		return keyColumnNames;
	}

	public boolean hasIndex() {
		return hasIndex;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public boolean isInverse() {
		return isInverse;
	}

	public String getTableName() {
		return qualifiedTableName;
	}

	private BasicBatchKey removeBatchKey;

	public void remove(Serializable id, SessionImplementor session) throws HibernateException {
		if ( !isInverse && isRowDeleteEnabled() ) {

			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Deleting collection: %s",
						MessageHelper.collectionInfoString( this, id, getFactory() ) );
			}

			// Remove all the old entries

			try {
				int offset = 1;
				PreparedStatement st = null;
				Expectation expectation = Expectations.appropriateExpectation( getDeleteAllCheckStyle() );
				boolean callable = isDeleteAllCallable();
				boolean useBatch = expectation.canBeBatched();
				String sql = getSQLDeleteString();
				if ( useBatch ) {
					if ( removeBatchKey == null ) {
						removeBatchKey = new BasicBatchKey(
								getRole() + "#REMOVE",
								expectation
								);
					}
					st = session.getTransactionCoordinator()
							.getJdbcCoordinator()
							.getBatch( removeBatchKey )
							.getBatchStatement( sql, callable );
				}
				else {
					st = session.getTransactionCoordinator()
							.getJdbcCoordinator()
							.getStatementPreparer()
							.prepareStatement( sql, callable );
				}

				try {
					offset += expectation.prepare( st );

					writeKey( st, id, offset, session );
					if ( useBatch ) {
						session.getTransactionCoordinator()
								.getJdbcCoordinator()
								.getBatch( removeBatchKey )
								.addToBatch();
					}
					else {
						expectation.verifyOutcome( st.executeUpdate(), st, -1 );
					}
				}
				catch ( SQLException sqle ) {
					if ( useBatch ) {
						session.getTransactionCoordinator().getJdbcCoordinator().abortBatch();
					}
					throw sqle;
				}
				finally {
					if ( !useBatch ) {
						st.close();
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

	private BasicBatchKey recreateBatchKey;

	public void recreate(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowInsertEnabled() ) {

			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Inserting collection: %s",
						MessageHelper.collectionInfoString( this, collection, id, session ) );
			}

			try {
				// create all the new entries
				Iterator entries = collection.entries( this );
				if ( entries.hasNext() ) {
					Expectation expectation = Expectations.appropriateExpectation( getInsertCheckStyle() );
					collection.preInsert( this );
					int i = 0;
					int count = 0;
					while ( entries.hasNext() ) {

						final Object entry = entries.next();
						if ( collection.entryExists( entry, i ) ) {
							int offset = 1;
							PreparedStatement st = null;
							boolean callable = isInsertCallable();
							boolean useBatch = expectation.canBeBatched();
							String sql = getSQLInsertRowString();

							if ( useBatch ) {
								if ( recreateBatchKey == null ) {
									recreateBatchKey = new BasicBatchKey(
											getRole() + "#RECREATE",
											expectation
											);
								}
								st = session.getTransactionCoordinator()
										.getJdbcCoordinator()
										.getBatch( recreateBatchKey )
										.getBatchStatement( sql, callable );
							}
							else {
								st = session.getTransactionCoordinator()
										.getJdbcCoordinator()
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
									session.getTransactionCoordinator()
											.getJdbcCoordinator()
											.getBatch( recreateBatchKey )
											.addToBatch();
								}
								else {
									expectation.verifyOutcome( st.executeUpdate(), st, -1 );
								}

								collection.afterRowInsert( this, entry, i );
								count++;
							}
							catch ( SQLException sqle ) {
								if ( useBatch ) {
									session.getTransactionCoordinator().getJdbcCoordinator().abortBatch();
								}
								throw sqle;
							}
							finally {
								if ( !useBatch ) {
									st.close();
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
	}

	protected boolean isRowDeleteEnabled() {
		return true;
	}

	private BasicBatchKey deleteBatchKey;

	public void deleteRows(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowDeleteEnabled() ) {

			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Deleting rows of collection: %s",
						MessageHelper.collectionInfoString( this, collection, id, session ) );
			}

			boolean deleteByIndex = !isOneToMany() && hasIndex && !indexContainsFormula;
			final Expectation expectation = Expectations.appropriateExpectation( getDeleteCheckStyle() );
			try {
				// delete all the deleted entries
				Iterator deletes = collection.getDeletes( this, !deleteByIndex );
				if ( deletes.hasNext() ) {
					int offset = 1;
					int count = 0;
					while ( deletes.hasNext() ) {
						PreparedStatement st = null;
						boolean callable = isDeleteCallable();
						boolean useBatch = expectation.canBeBatched();
						String sql = getSQLDeleteRowString();

						if ( useBatch ) {
							if ( deleteBatchKey == null ) {
								deleteBatchKey = new BasicBatchKey(
										getRole() + "#DELETE",
										expectation
										);
							}
							st = session.getTransactionCoordinator()
									.getJdbcCoordinator()
									.getBatch( deleteBatchKey )
									.getBatchStatement( sql, callable );
						}
						else {
							st = session.getTransactionCoordinator()
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
								session.getTransactionCoordinator()
										.getJdbcCoordinator()
										.getBatch( deleteBatchKey )
										.addToBatch();
							}
							else {
								expectation.verifyOutcome( st.executeUpdate(), st, -1 );
							}
							count++;
						}
						catch ( SQLException sqle ) {
							if ( useBatch ) {
								session.getTransactionCoordinator().getJdbcCoordinator().abortBatch();
							}
							throw sqle;
						}
						finally {
							if ( !useBatch ) {
								st.close();
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
	}

	protected boolean isRowInsertEnabled() {
		return true;
	}

	private BasicBatchKey insertBatchKey;

	public void insertRows(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowInsertEnabled() ) {

			if ( LOG.isDebugEnabled() ) LOG.debugf( "Inserting rows of collection: %s",
					MessageHelper.collectionInfoString( this, collection, id, session ) );

			try {
				// insert all the new entries
				collection.preInsert( this );
				Iterator entries = collection.entries( this );
				Expectation expectation = Expectations.appropriateExpectation( getInsertCheckStyle() );
				boolean callable = isInsertCallable();
				boolean useBatch = expectation.canBeBatched();
				String sql = getSQLInsertRowString();
				int i = 0;
				int count = 0;
				while ( entries.hasNext() ) {
					int offset = 1;
					Object entry = entries.next();
					PreparedStatement st = null;
					if ( collection.needsInserting( entry, i, elementType ) ) {

						if ( useBatch ) {
							if ( insertBatchKey == null ) {
								insertBatchKey = new BasicBatchKey(
										getRole() + "#INSERT",
										expectation
										);
							}
							if ( st == null ) {
								st = session.getTransactionCoordinator()
										.getJdbcCoordinator()
										.getBatch( insertBatchKey )
										.getBatchStatement( sql, callable );
							}
						}
						else {
							st = session.getTransactionCoordinator()
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
								session.getTransactionCoordinator().getJdbcCoordinator().getBatch( insertBatchKey ).addToBatch();
							}
							else {
								expectation.verifyOutcome( st.executeUpdate(), st, -1 );
							}
							collection.afterRowInsert( this, entry, i );
							count++;
						}
						catch ( SQLException sqle ) {
							if ( useBatch ) {
								session.getTransactionCoordinator().getJdbcCoordinator().abortBatch();
							}
							throw sqle;
						}
						finally {
							if ( !useBatch ) {
								st.close();
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
	}

	public String getRole() {
		return role;
	}

	public String getOwnerEntityName() {
		return entityName;
	}

	public EntityPersister getOwnerEntityPersister() {
		return ownerPersister;
	}

	public IdentifierGenerator getIdentifierGenerator() {
		return identifierGenerator;
	}

	public Type getIdentifierType() {
		return identifierType;
	}

	public boolean hasOrphanDelete() {
		return hasOrphanDelete;
	}

	public Type toType(String propertyName) throws QueryException {
		if ( "index".equals( propertyName ) ) {
			return indexType;
		}
		return elementPropertyMapping.toType( propertyName );
	}

	public abstract boolean isManyToMany();

	public String getManyToManyFilterFragment(String alias, Map enabledFilters) {
		StringBuilder buffer = new StringBuilder();
		manyToManyFilterHelper.render( buffer, elementPersister.getFilterAliasGenerator(alias), enabledFilters );

		if ( manyToManyWhereString != null ) {
			buffer.append( " and " )
					.append( StringHelper.replace( manyToManyWhereTemplate, Template.TEMPLATE, alias ) );
		}

		return buffer.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] toColumns(String alias, String propertyName) throws QueryException {
		if ( "index".equals( propertyName ) ) {
			return qualify( alias, indexColumnNames, indexFormulaTemplates );
		}
		return elementPropertyMapping.toColumns( alias, propertyName );
	}

	private String[] indexFragments;

	/**
	 * {@inheritDoc}
	 */
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

	public Type getType() {
		return elementPropertyMapping.getType(); // ==elementType ??
	}

	public String getName() {
		return getRole();
	}

	public EntityPersister getElementPersister() {
		if ( elementPersister == null ) {
			throw new AssertionFailure( "not an association" );
		}
		return elementPersister;
	}

	public boolean isCollection() {
		return true;
	}

	public Serializable[] getCollectionSpaces() {
		return spaces;
	}

	protected abstract String generateDeleteString();

	protected abstract String generateDeleteRowString();

	protected abstract String generateUpdateRowString();

	protected abstract String generateInsertRowString();

	public void updateRows(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && collection.isRowUpdatePossible() ) {

			LOG.debugf( "Updating rows of collection: %s#%s", role, id );

			// update all the modified entries
			int count = doUpdateRows( id, collection, session );

			LOG.debugf( "Done updating rows: %s updated", count );
		}
	}

	protected abstract int doUpdateRows(Serializable key, PersistentCollection collection, SessionImplementor session)
			throws HibernateException;

	public CollectionMetadata getCollectionMetadata() {
		return this;
	}

	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	protected String filterFragment(String alias) throws MappingException {
		return hasWhere() ? " and " + getSQLWhereString( alias ) : "";
	}

	public String filterFragment(String alias, Map enabledFilters) throws MappingException {

		StringBuilder sessionFilterFragment = new StringBuilder();
		filterHelper.render( sessionFilterFragment, getFilterAliasGenerator(alias), enabledFilters );

		return sessionFilterFragment.append( filterFragment( alias ) ).toString();
	}

	public String oneToManyFilterFragment(String alias) throws MappingException {
		return "";
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

	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + '(' + role + ')';
	}

	public boolean isVersioned() {
		return isVersioned && getOwnerEntityPersister().isVersioned();
	}

	public String getNodeName() {
		return nodeName;
	}

	public String getElementNodeName() {
		return elementNodeName;
	}

	public String getIndexNodeName() {
		return indexNodeName;
	}

	// TODO: deprecate???
	protected SQLExceptionConverter getSQLExceptionConverter() {
		return getSQLExceptionHelper().getSqlExceptionConverter();
	}

	// TODO: needed???
	protected SqlExceptionHelper getSQLExceptionHelper() {
		return sqlExceptionHelper;
	}

	public CacheEntryStructure getCacheEntryStructure() {
		return cacheEntryStructure;
	}

	public boolean isAffectedByEnabledFilters(SessionImplementor session) {
		return filterHelper.isAffectedBy( session.getEnabledFilters() ) ||
				( isManyToMany() && manyToManyFilterHelper.isAffectedBy( session.getEnabledFilters() ) );
	}

	public boolean isSubselectLoadable() {
		return subselectLoadable;
	}

	public boolean isMutable() {
		return isMutable;
	}

	public String[] getCollectionPropertyColumnAliases(String propertyName, String suffix) {
		String rawAliases[] = (String[]) collectionPropertyColumnAliases.get( propertyName );

		if ( rawAliases == null ) {
			return null;
		}

		String result[] = new String[rawAliases.length];
		for ( int i = 0; i < rawAliases.length; i++ ) {
			result[i] = new Alias( suffix ).toUnquotedAliasString( rawAliases[i] );
		}
		return result;
	}

	// TODO: formulas ?
	public void initCollectionPropertyMap() {

		initCollectionPropertyMap( "key", keyType, keyColumnAliases, keyColumnNames );
		initCollectionPropertyMap( "element", elementType, elementColumnAliases, elementColumnNames );
		if ( hasIndex ) {
			initCollectionPropertyMap( "index", indexType, indexColumnAliases, indexColumnNames );
		}
		if ( hasIdentifier ) {
			initCollectionPropertyMap(
					"id",
					identifierType,
					new String[] { identifierColumnAlias },
					new String[] { identifierColumnName } );
		}
	}

	private void initCollectionPropertyMap(String aliasName, Type type, String[] columnAliases, String[] columnNames) {

		collectionPropertyColumnAliases.put( aliasName, columnAliases );
		collectionPropertyColumnNames.put( aliasName, columnNames );

		if ( type.isComponentType() ) {
			CompositeType ct = (CompositeType) type;
			String[] propertyNames = ct.getPropertyNames();
			for ( int i = 0; i < propertyNames.length; i++ ) {
				String name = propertyNames[i];
				collectionPropertyColumnAliases.put( aliasName + "." + name, columnAliases[i] );
				collectionPropertyColumnNames.put( aliasName + "." + name, columnNames[i] );
			}
		}

	}

	public int getSize(Serializable key, SessionImplementor session) {
		try {
			PreparedStatement st = session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( sqlSelectSizeString );
			try {
				getKeyType().nullSafeSet( st, key, 1, session );
				ResultSet rs = st.executeQuery();
				try {
					return rs.next() ? rs.getInt( 1 ) - baseIndex : 0;
				}
				finally {
					rs.close();
				}
			}
			finally {
				st.close();
			}
		}
		catch ( SQLException sqle ) {
			throw getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not retrieve collection size: " +
							MessageHelper.collectionInfoString( this, key, getFactory() ),
					sqlSelectSizeString
					);
		}
	}

	public boolean indexExists(Serializable key, Object index, SessionImplementor session) {
		return exists( key, incrementIndexByBase( index ), getIndexType(), sqlDetectRowByIndexString, session );
	}

	public boolean elementExists(Serializable key, Object element, SessionImplementor session) {
		return exists( key, element, getElementType(), sqlDetectRowByElementString, session );
	}

	private boolean exists(Serializable key, Object indexOrElement, Type indexOrElementType, String sql, SessionImplementor session) {
		try {
			PreparedStatement st = session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( sql );
			try {
				getKeyType().nullSafeSet( st, key, 1, session );
				indexOrElementType.nullSafeSet( st, indexOrElement, keyColumnNames.length + 1, session );
				ResultSet rs = st.executeQuery();
				try {
					return rs.next();
				}
				finally {
					rs.close();
				}
			}
			catch ( TransientObjectException e ) {
				return false;
			}
			finally {
				st.close();
			}
		}
		catch ( SQLException sqle ) {
			throw getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not check row existence: " +
							MessageHelper.collectionInfoString( this, key, getFactory() ),
					sqlSelectSizeString
					);
		}
	}

	public Object getElementByIndex(Serializable key, Object index, SessionImplementor session, Object owner) {
		try {
			PreparedStatement st = session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( sqlSelectRowByIndexString );
			try {
				getKeyType().nullSafeSet( st, key, 1, session );
				getIndexType().nullSafeSet( st, incrementIndexByBase( index ), keyColumnNames.length + 1, session );
				ResultSet rs = st.executeQuery();
				try {
					if ( rs.next() ) {
						return getElementType().nullSafeGet( rs, elementColumnAliases, session, owner );
					}
					else {
						return null;
					}
				}
				finally {
					rs.close();
				}
			}
			finally {
				st.close();
			}
		}
		catch ( SQLException sqle ) {
			throw getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not read row: " +
							MessageHelper.collectionInfoString( this, key, getFactory() ),
					sqlSelectSizeString
					);
		}
	}

	public boolean isExtraLazy() {
		return isExtraLazy;
	}

	protected Dialect getDialect() {
		return dialect;
	}

	/**
	 * Intended for internal use only. In fact really only currently used from
	 * test suite for assertion purposes.
	 * 
	 * @return The default collection initializer for this persister/collection.
	 */
	public CollectionInitializer getInitializer() {
		return initializer;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	private class StandardOrderByAliasResolver implements OrderByAliasResolver {
		private final String rootAlias;

		private StandardOrderByAliasResolver(String rootAlias) {
			this.rootAlias = rootAlias;
		}

		@Override
		public String resolveTableAlias(String columnReference) {
			if ( elementPersister == null ) {
				// we have collection of non-entity elements...
				return rootAlias;
			}
			else {
				return ( (Loadable) elementPersister ).getTableAliasForColumn( columnReference, rootAlias );
			}
		}
	}
	
	public abstract FilterAliasGenerator getFilterAliasGenerator(final String rootAlias);
}
