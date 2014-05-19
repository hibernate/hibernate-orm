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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.TransientObjectException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.StructuredCollectionCacheEntry;
import org.hibernate.cache.spi.entry.StructuredMapCacheEntry;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
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
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.FilterHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.PrimitiveTypeDescriptor;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.PluralAttributeNature;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.Cascadeable;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.ListBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBindingManyToMany;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
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
import org.hibernate.sql.Alias;
import org.hibernate.sql.SelectFragment;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.Template;
import org.hibernate.sql.ordering.antlr.ColumnMapper;
import org.hibernate.sql.ordering.antlr.ColumnReference;
import org.hibernate.sql.ordering.antlr.FormulaReference;
import org.hibernate.sql.ordering.antlr.OrderByAliasResolver;
import org.hibernate.sql.ordering.antlr.OrderByTranslation;
import org.hibernate.sql.ordering.antlr.SqlValueReference;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

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
	private String mappedByProperty;

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
	protected final boolean isInverse;
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
	protected final SqlExceptionHelper sqlExceptionHelper;
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
			AbstractPluralAttributeBinding collection,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			MetadataImplementor metadataImplementor,
			SessionFactoryImplementor factory) throws MappingException, CacheException {

		this.factory = factory;
		this.cacheAccessStrategy = cacheAccessStrategy;
		if ( factory.getSettings().isStructuredCacheEntriesEnabled() ) {
			cacheEntryStructure = collection.getAttribute().getPluralAttributeNature() == PluralAttributeNature.MAP
					? StructuredMapCacheEntry.INSTANCE
					: StructuredCollectionCacheEntry.INSTANCE;
		}
		else {
			cacheEntryStructure = UnstructuredCacheEntry.INSTANCE;
		}


		dialect = factory.getDialect();
		sqlExceptionHelper = factory.getSQLExceptionHelper();
		if ( !collection.getHibernateTypeDescriptor().getResolvedTypeMapping().isCollectionType() ) {
			throw new MappingException(
					String.format(
							"Unexpected resolved type for %s; expected a CollectionType; instead it is %s",
							collection.getAttribute().getRole(),
							collection.getHibernateTypeDescriptor().getResolvedTypeMapping() )
			);
		}

		collectionType = (CollectionType) collection.getHibernateTypeDescriptor().getResolvedTypeMapping();
		role = collection.getAttribute().getRole();
		entityName = collection.getContainer().seekEntityBinding().getEntityName();
		ownerPersister = factory.getEntityPersister( entityName );
		queryLoaderName = collection.getCustomLoaderName();
		// TODO: is nodeName obsolete?
		//nodeName = collection.getNodeName();
		nodeName = null;
		isMutable = collection.isMutable();

		TableSpecification table = collection.getPluralAttributeKeyBinding().getCollectionTable();
		fetchMode = collection.getFetchMode();
		elementType = collection.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getResolvedTypeMapping();
		// isSet = collection.isSet();
		// isSorted = collection.isSorted();
		isArray = collectionType.isArrayType();
		isPrimitiveArray =
				isArray &&
						PrimitiveType.class.isInstance(
								collection.getPluralAttributeElementBinding()
										.getHibernateTypeDescriptor()
										.getResolvedTypeMapping()
						);
		subselectLoadable = collection.getFetchStyle() == FetchStyle.SUBSELECT;

		qualifiedTableName = table.getQualifiedName( dialect );

		// TODO: fix this when synchronized tables are available
		spaces = new String[1];
		spaces[0] = qualifiedTableName;
		//int spacesSize = 1 + collection.getSynchronizedTables().size();
		//spaces = new String[spacesSize];
		//spaces[0] = qualifiedTableName;
		//Iterator iter = collection.getSynchronizedTables().iterator();
		//for ( int i = 1; i < spacesSize; i++ ) {
		//	spaces[i] = (String) iter.next();
		//}

		sqlWhereString = StringHelper.isNotEmpty( collection.getWhere() ) ? "( " + collection.getWhere() + ") " : null;
		hasWhere = sqlWhereString != null;
		sqlWhereStringTemplate = hasWhere ?
				Template.renderWhereStringTemplate( sqlWhereString, dialect, factory.getSqlFunctionRegistry() ) :
				null;

		hasOrphanDelete =
				collection.getPluralAttributeElementBinding() instanceof Cascadeable &&
						( (Cascadeable) collection.getPluralAttributeElementBinding() ).getCascadeStyle().hasOrphanDelete();

		int batch = collection.getBatchSize();
		if ( batch == -1 ) {
			batch = factory.getSettings().getDefaultBatchFetchSize();
		}
		batchSize = batch;

		isVersioned = collection.isIncludedInOptimisticLocking();

		// KEY

		PluralAttributeKeyBinding keyBinding = collection.getPluralAttributeKeyBinding();
		keyType = keyBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();

		int keySpan = keyBinding.getRelationalValueBindings().size();
		keyColumnNames = new String[keySpan];
		keyColumnAliases = new String[keySpan];
		int k = 0;
		for ( RelationalValueBinding keyRelationalValueBinding : keyBinding.getRelationalValueBindings() ) {
			org.hibernate.metamodel.spi.relational.Column keyColumn =
					(org.hibernate.metamodel.spi.relational.Column) keyRelationalValueBinding.getValue();
			// NativeSQL: collect key column and auto-aliases
			keyColumnNames[k] = keyColumn.getColumnName().getText( dialect );
			// TODO: does the owner root table need to be in alias?
			keyColumnAliases[k] = keyColumn.getAlias(
					dialect,
					collection.getContainer().seekEntityBinding().getPrimaryTable()
			);
			// keyColumnAliases[k] = col.getAlias( dialect, collection.getOwner().getRootTable() );
			k++;
		}

		// ELEMENT

		if ( elementType.isEntityType() ) {
			String entityName = ( (EntityType) elementType ).getAssociatedEntityName();
			elementPersister = factory.getEntityPersister( entityName );
		}
		else {
			elementPersister = null;
		}
		elementNodeName = null;
		int elementSpan = collection.getPluralAttributeElementBinding().getRelationalValueContainer().relationalValueBindings() == null
				? 0
				: collection.getPluralAttributeElementBinding().getRelationalValueContainer().relationalValueBindings().size();
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
		if ( elementSpan > 0 ) {
			for ( RelationalValueBinding relationalValueBinding : collection.getPluralAttributeElementBinding().getRelationalValueContainer().relationalValueBindings() ) {
				final Value value = relationalValueBinding.getValue();
				elementColumnAliases[j] = value.getAlias( dialect, table );
				if ( DerivedValue.class.isInstance( value ) ) {
					DerivedValue form = (DerivedValue) value;
					elementFormulaTemplates[j] = getTemplateFromString( form.getExpression(), factory);
					elementFormulas[j] = form.getExpression();
				}
				else {
					org.hibernate.metamodel.spi.relational.Column col =
							(org.hibernate.metamodel.spi.relational.Column) value;
					elementColumnNames[j] = col.getColumnName().getText( dialect );
					elementColumnWriters[j] = col.getWriteFragment() == null ? "?" : col.getWriteFragment();
					elementColumnReaders[j] = col.getReadFragment() == null ?
							col.getColumnName().getText( factory.getDialect() ) :
							col.getReadFragment();
					elementColumnReaderTemplates[j] = getTemplateFromColumn( col, factory );
					elementColumnIsSettable[j] = true;
					elementColumnIsInPrimaryKey[j] = !col.isNullable();
					if ( !col.isNullable() ) {
						hasNotNullableColumns = true;
					}
					isPureFormula = false;
				}
				j++;
			}
		}
		elementIsPureFormula = isPureFormula;

		// workaround, for backward compatibility of sets with no
		// not-null columns, assume all columns are used in the
		// row locator SQL
		if ( !hasNotNullableColumns ) {
			Arrays.fill( elementColumnIsInPrimaryKey, true );
		}

		// INDEX AND ROW SELECT

		hasIndex = collection.hasIndex();

		indexNodeName = null;

		if ( hasIndex ) {
			// NativeSQL: collect index column and auto-aliases
			IndexedPluralAttributeBinding indexedBinding = (IndexedPluralAttributeBinding) collection;
			PluralAttributeIndexBinding indexBinding = indexedBinding.getPluralAttributeIndexBinding();
			indexType = indexBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
			baseIndex = indexBinding instanceof ListBinding ? ( ( ListBinding ) indexBinding ).base() : 0;
			final java.util.List<RelationalValueBinding> indexRelationalValueBindings = indexBinding.getRelationalValueBindings();
			int indexSpan = indexRelationalValueBindings.size();
			boolean hasFormulas = false;
			indexColumnNames = new String[indexSpan];
			indexFormulaTemplates = new String[indexSpan];
			indexFormulas = new String[indexSpan];
			indexColumnIsSettable = new boolean[indexSpan];
			indexColumnAliases = new String[indexSpan];
			for ( int i = 0 ; i < indexSpan ; i++ ) {
				final RelationalValueBinding rb = indexRelationalValueBindings.get( i );
				final Value value = rb.getValue();
				indexColumnAliases[ i ] = value.getAlias( dialect,
						collection.getContainer().seekEntityBinding().getPrimaryTable() );
				if ( !rb.isDerived() ) {
					indexColumnIsSettable[ i ] = true;
					org.hibernate.metamodel.spi.relational.Column column =
							( org.hibernate.metamodel.spi.relational.Column ) value;
					indexColumnNames[ i ] = column.getColumnName().getText( dialect );
				} else {
					DerivedValue derivedValue = ( DerivedValue ) value;
					indexFormulaTemplates[ i ] = getTemplateFromString( derivedValue.getExpression(), factory);
					indexFormulas[ i ] = derivedValue.getExpression();
					hasFormulas = true;
				}
			}
			this.indexContainsFormula = hasFormulas;
		} else {
			indexColumnIsSettable = null;
			indexFormulaTemplates = null;
			indexFormulas = null;
			indexType = null;
			indexColumnNames = null;
			indexColumnAliases = null;
			baseIndex = 0;
			indexContainsFormula = false;
		}

		hasIdentifier = collection.getAttribute().getPluralAttributeNature() == PluralAttributeNature.ID_BAG;
		// TODO: fix this when IdBags are supported.
		//if ( hasIdentifier ) {
		//}
		//else {
		identifierType = null;
		identifierColumnName = null;
		identifierColumnAlias = null;
		// unquotedIdentifierColumnName = null;
		identifierGenerator = null;
		//}

		// GENERATE THE SQL:

		// sqlSelectString = sqlSelectString();
		// sqlSelectRowString = sqlSelectRowString();

		if ( collection.getCustomSqlInsert() == null ) {
			sqlInsertRowString = generateInsertRowString();
			insertCallable = false;
			insertCheckStyle = ExecuteUpdateResultCheckStyle.COUNT;
		}
		else {
			final CustomSQL customSqlInsert = collection.getCustomSqlInsert();
			sqlInsertRowString = customSqlInsert.getSql();
			insertCallable = customSqlInsert.isCallable();
			insertCheckStyle = customSqlInsert.getCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( customSqlInsert.getSql(), insertCallable )
					: customSqlInsert.getCheckStyle();
		}

		if ( collection.getCustomSqlUpdate() == null ) {
			sqlUpdateRowString = generateUpdateRowString();
			updateCallable = false;
			updateCheckStyle = ExecuteUpdateResultCheckStyle.COUNT;
		}
		else {
			final CustomSQL customSqlUpdate = collection.getCustomSqlUpdate();
			sqlUpdateRowString = customSqlUpdate.getSql();
			updateCallable = customSqlUpdate.isCallable();
			updateCheckStyle = customSqlUpdate.getCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( customSqlUpdate.getSql(), insertCallable )
					: customSqlUpdate.getCheckStyle();
		}

		if ( collection.getCustomSqlDelete() == null ) {
			sqlDeleteRowString = generateDeleteRowString();
			deleteCallable = false;
			deleteCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}
		else {
			final CustomSQL customSqlDelete = collection.getCustomSqlDelete();
			sqlDeleteRowString = customSqlDelete.getSql();
			deleteCallable = customSqlDelete.isCallable();
			deleteCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}

		if ( collection.getCustomSqlDeleteAll() == null ) {
			sqlDeleteString = generateDeleteString();
			deleteAllCallable = false;
			deleteAllCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}
		else {
			final CustomSQL customSqlDeleteAll = collection.getCustomSqlDeleteAll();
			sqlDeleteString = customSqlDeleteAll.getSql();
			deleteAllCallable = customSqlDeleteAll.isCallable();
			deleteAllCheckStyle = ExecuteUpdateResultCheckStyle.NONE;
		}

		sqlSelectSizeString = generateSelectSizeString(
				collection.hasIndex() &&
						collection.getAttribute().getPluralAttributeNature() != PluralAttributeNature.MAP
		);
		sqlDetectRowByIndexString = generateDetectRowByIndexString();
		sqlDetectRowByElementString = generateDetectRowByElementString();
		sqlSelectRowByIndexString = generateSelectRowByIndexString();

		logStaticSQL();

		isLazy = collection.getFetchTiming() != FetchTiming.IMMEDIATE;
		isExtraLazy = collection.getFetchTiming() == FetchTiming.EXTRA_LAZY;

		isInverse = keyBinding.isInverse();
		if ( isArray ) {
			// TODO: Move into a util?
			final JavaTypeDescriptor descriptor = collection.getAttribute().getElementType().getDescriptor();
			if (PrimitiveTypeDescriptor.class.isInstance( descriptor )) {
				elementClass = ( (PrimitiveTypeDescriptor) descriptor ).getClassType();
			}
			else {
				final ClassLoaderService cls = factory.getServiceRegistry().getService( ClassLoaderService.class );
				elementClass = cls.classForName( descriptor.getName().toString() );
			}
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
		filterHelper = new FilterHelper( collection.getFilterConfigurations(), factory );

		if ( collection.getPluralAttributeElementBinding()
				.getNature() == PluralAttributeElementNature.MANY_TO_MANY ) {
			final PluralAttributeElementBindingManyToMany manyToManyElementBinding =
					(PluralAttributeElementBindingManyToMany) collection.getPluralAttributeElementBinding();
			manyToManyFilterHelper = new FilterHelper( manyToManyElementBinding.getFilterConfigurations(), factory );
			manyToManyWhereString = StringHelper.isNotEmpty( manyToManyElementBinding.getManyToManyWhere() ) ?
					"( " +manyToManyElementBinding.getManyToManyWhere() + ")" :
					null;
			manyToManyWhereTemplate = manyToManyWhereString == null ?
					null :
					Template.renderWhereStringTemplate(
							manyToManyWhereString, factory.getDialect(), factory.getSqlFunctionRegistry()
					);

			hasManyToManyOrder = manyToManyElementBinding.getManyToManyOrderBy() != null;
			if ( hasManyToManyOrder ) {
				manyToManyOrderByTranslation = Template.translateOrderBy(
						manyToManyElementBinding.getManyToManyOrderBy(),
						new ColumnMapperImpl(),
						factory,
						dialect,
						factory.getSqlFunctionRegistry()
				);
			}
			else {
				manyToManyOrderByTranslation = null;
			}
		}
		else {
			manyToManyFilterHelper = new FilterHelper( Collections.<FilterConfiguration>emptyList(), factory );
			manyToManyWhereString = null;
			manyToManyWhereTemplate = null;
			hasManyToManyOrder = false;
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
//					final int propertyIndex = elementPersister.getEntityMetamodel().getPropertyIndex( reference );
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

	protected static String getTemplateFromString(String string, SessionFactoryImplementor factory) {
		return string == null ?
				null :
				Template.renderWhereStringTemplate( string, factory.getDialect(), factory.getSqlFunctionRegistry() );
	}

	public String getTemplateFromColumn(org.hibernate.metamodel.spi.relational.Column column, SessionFactoryImplementor factory) {
		String templateString;
		if ( column.getReadFragment() != null ) {
			templateString = getTemplateFromString( column.getReadFragment(), factory );
		}
		else {
			String columnName = column.getColumnName().getText( factory.getDialect() );
			templateString = Template.TEMPLATE + '.' + columnName;
		}
		return templateString;
	}

	@Override
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

	@Override
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

	@Override
	public CollectionRegionAccessStrategy getCacheAccessStrategy() {
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

	protected String getSQLWhereString(String alias) {
		return StringHelper.replace( sqlWhereStringTemplate, Template.TEMPLATE, alias );
	}

	@Override
	public String getSQLOrderByString(String alias) {
		return hasOrdering()
				? orderByTranslation.injectAliases( new StandardOrderByAliasResolver( alias ) )
				: "";
	}

	@Override
	public String getManyToManyOrderByString(String alias) {
		return hasManyToManyOrdering()
				? manyToManyOrderByTranslation.injectAliases( new StandardOrderByAliasResolver( alias ) )
				: "";
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

	/**
	 * Return the element class of an array, or null otherwise.  needed by arrays
	 */
	@Override
	public Class getElementClass() {
		return elementClass;
	}

	@Override
	public Object readElement(ResultSet rs, Object owner, String[] aliases, SessionImplementor session)
			throws HibernateException, SQLException {
		return getElementType().nullSafeGet( rs, aliases, session, owner );
	}

	@Override
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

	@Override
	public Object readIdentifier(ResultSet rs, String alias, SessionImplementor session)
			throws HibernateException, SQLException {
		Object id = getIdentifierType().nullSafeGet( rs, alias, session, null );
		if ( id == null ) {
			throw new HibernateException( "null identifier column for collection: " + role );
		}
		return id;
	}

	@Override
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
						expectation.verifyOutcome( session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( st ), st, -1 );
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
						session.getTransactionCoordinator().getJdbcCoordinator().release( st );
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
									expectation.verifyOutcome( session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( st ), st, -1 );
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
									session.getTransactionCoordinator().getJdbcCoordinator().release( st );
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

	@Override
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
								expectation.verifyOutcome( session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( st ), st, -1 );
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
								session.getTransactionCoordinator().getJdbcCoordinator().release( st );
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

	@Override
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
								expectation.verifyOutcome( session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().executeUpdate( st ), st, -1 );
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
								session.getTransactionCoordinator().getJdbcCoordinator().release( st );
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

	@Override
	public String getRole() {
		return role;
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
	public abstract boolean isManyToMany();

	@Override
	public String getManyToManyFilterFragment(String alias, Map enabledFilters) {
		StringBuilder buffer = new StringBuilder();
		manyToManyFilterHelper.render( buffer, elementPersister.getFilterAliasGenerator(alias), enabledFilters );

		if ( manyToManyWhereString != null ) {
			buffer.append( " and " )
					.append( StringHelper.replace( manyToManyWhereTemplate, Template.TEMPLATE, alias ) );
		}

		return buffer.toString();
	}

	@Override
	public String[] toColumns(String alias, String propertyName) throws QueryException {
		if ( "index".equals( propertyName ) ) {
			return qualify( alias, indexColumnNames, indexFormulaTemplates );
		}
		return elementPropertyMapping.toColumns( alias, propertyName );
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
	public Type getType() {
		return elementPropertyMapping.getType(); // ==elementType ??
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

	@Override
	public void processQueuedOps(PersistentCollection collection, Serializable key, SessionImplementor session)
			throws HibernateException {
		if ( collection.hasQueuedOperations() ) {
			int nextIndex = getSize( key, session );
			doProcessQueuedOps( collection, key, nextIndex, session );
		}
	}

	/**
	 * Process queued operations within the PersistentCollection.
	 *
	 * @param collection The collection
	 * @param key The collection key
	 * @param nextIndex The next index to write
	 * @param session The session
	 * @throws HibernateException
	 *
	 * @deprecated Use {@link #doProcessQueuedOps(org.hibernate.collection.spi.PersistentCollection, java.io.Serializable, org.hibernate.engine.spi.SessionImplementor)}
	 */
	@Deprecated
	protected void doProcessQueuedOps(PersistentCollection collection, Serializable key,
			int nextIndex, SessionImplementor session)
			throws HibernateException {
		doProcessQueuedOps( collection, key, session );
	}

	protected abstract void doProcessQueuedOps(PersistentCollection collection, Serializable key, SessionImplementor session)
			throws HibernateException;

	@Override
	public CollectionMetadata getCollectionMetadata() {
		return this;
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	protected String filterFragment(String alias) throws MappingException {
		return hasWhere() ? " and " + getSQLWhereString( alias ) : "";
	}

	protected String filterFragment(String alias, Set<String> treatAsDeclarations) throws MappingException {
		return hasWhere() ? " and " + getSQLWhereString( alias ) : "";
	}

	@Override
	public String filterFragment(String alias, Map enabledFilters) throws MappingException {
		StringBuilder sessionFilterFragment = new StringBuilder();
		filterHelper.render( sessionFilterFragment, getFilterAliasGenerator(alias), enabledFilters );

		return sessionFilterFragment.append( filterFragment( alias ) ).toString();
	}

	@Override
	public String filterFragment(
			String alias,
			Map enabledFilters,
			Set<String> treatAsDeclarations) {
		StringBuilder sessionFilterFragment = new StringBuilder();
		filterHelper.render( sessionFilterFragment, getFilterAliasGenerator(alias), enabledFilters );

		return sessionFilterFragment.append( filterFragment( alias, treatAsDeclarations ) ).toString();
	}

	@Override
	public String oneToManyFilterFragment(String alias) throws MappingException {
		return "";
	}

	@Override
	public String oneToManyFilterFragment(String alias, Set<String> treatAsDeclarations) {
		return oneToManyFilterFragment( alias );
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
		return StringHelper.unqualify( getClass().getName() ) + '(' + role + ')';
	}

	@Override
	public boolean isVersioned() {
		return isVersioned && getOwnerEntityPersister().isVersioned();
	}

	@Override
	public String getNodeName() {
		return nodeName;
	}

	@Override
	public String getElementNodeName() {
		return elementNodeName;
	}

	@Override
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

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		return cacheEntryStructure;
	}

	@Override
	public boolean isAffectedByEnabledFilters(SessionImplementor session) {
		return filterHelper.isAffectedBy( session.getEnabledFilters() ) ||
				( isManyToMany() && manyToManyFilterHelper.isAffectedBy( session.getEnabledFilters() ) );
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
		String[] rawAliases = (String[]) collectionPropertyColumnAliases.get( propertyName );

		if ( rawAliases == null ) {
			return null;
		}

		String[] result = new String[rawAliases.length];
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

	@Override
	public int getSize(Serializable key, SessionImplementor session) {
		try {
			PreparedStatement st = session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( sqlSelectSizeString );
			try {
				getKeyType().nullSafeSet( st, key, 1, session );
				ResultSet rs = session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( st );
				try {
					return rs.next() ? rs.getInt( 1 ) - baseIndex : 0;
				}
				finally {
					session.getTransactionCoordinator().getJdbcCoordinator().release( rs, st );
				}
			}
			finally {
				session.getTransactionCoordinator().getJdbcCoordinator().release( st );
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
	public boolean indexExists(Serializable key, Object index, SessionImplementor session) {
		return exists( key, incrementIndexByBase( index ), getIndexType(), sqlDetectRowByIndexString, session );
	}

	@Override
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
				ResultSet rs = session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( st );
				try {
					return rs.next();
				}
				finally {
					session.getTransactionCoordinator().getJdbcCoordinator().release( rs, st );
				}
			}
			catch ( TransientObjectException e ) {
				return false;
			}
			finally {
				session.getTransactionCoordinator().getJdbcCoordinator().release( st );
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
	public Object getElementByIndex(Serializable key, Object index, SessionImplementor session, Object owner) {
		try {
			PreparedStatement st = session.getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( sqlSelectRowByIndexString );
			try {
				getKeyType().nullSafeSet( st, key, 1, session );
				getIndexType().nullSafeSet( st, incrementIndexByBase( index ), keyColumnNames.length + 1, session );
				ResultSet rs = session.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( st );
				try {
					if ( rs.next() ) {
						return getElementType().nullSafeGet( rs, elementColumnAliases, session, owner );
					}
					else {
						return null;
					}
				}
				finally {
					session.getTransactionCoordinator().getJdbcCoordinator().release( rs, st );
				}
			}
			finally {
				session.getTransactionCoordinator().getJdbcCoordinator().release( st );
			}
		}
		catch ( SQLException sqle ) {
			throw getSQLExceptionHelper().convert(
					sqle,
					"could not read row: " +
							MessageHelper.collectionInfoString( this, key, getFactory() ),
					sqlSelectSizeString
			);
		}
	}

	@Override
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

	@Override
	public String getMappedByProperty() {
		return mappedByProperty;
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
}
