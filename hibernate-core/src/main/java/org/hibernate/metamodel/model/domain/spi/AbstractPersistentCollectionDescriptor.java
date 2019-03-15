/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.Remove;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.StructuredCollectionCacheEntry;
import org.hibernate.cache.spi.entry.StructuredMapCacheEntry;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.internal.CollectionLoaderImpl;
import org.hibernate.loader.spi.CollectionLoader;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.internal.SqlAliasStemHelper;
import org.hibernate.metamodel.model.domain.internal.collection.AbstractCreationExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.BasicCollectionElementImpl;
import org.hibernate.metamodel.model.domain.internal.collection.BasicCollectionIndexImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionCreationExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionElementEmbeddedImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionElementEntityImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionElementExistsSelector;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionIndexEmbeddedImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionIndexEntityImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionIndexExistsSelector;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionRemovalExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionRowByIndexSelector;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionRowsDeletionExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionRowsIndexUpdateExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionRowsUpdateExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionSizeSelector;
import org.hibernate.metamodel.model.domain.internal.collection.FetchedTableReferenceCollectorImpl;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableCollectionRowByIndexSelector;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableCreationExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableRemovalExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableRowsDeleletionExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableRowsInsertExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableRowsUpdateExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyCreationExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyRemovalExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyRowsDeletionExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyRowsIndexUpdateExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyRowsInsertExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyRowsUpdateExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.RootTableReferenceCollectorImpl;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.naming.Identifier;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.results.DomainResultCreationException;
import org.hibernate.sql.results.internal.domain.collection.CollectionFetchImpl;
import org.hibernate.sql.results.internal.domain.collection.CollectionInitializerProducer;
import org.hibernate.sql.results.internal.domain.collection.CollectionResultImpl;
import org.hibernate.sql.results.internal.domain.collection.DelayedCollectionFetch;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;

import org.jboss.logging.Logger;

import static org.hibernate.metamodel.model.domain.spi.CollectionElement.ElementClassification.ONE_TO_MANY;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPersistentCollectionDescriptor<O, C, E>
		implements PersistentCollectionDescriptor<O, C, E> {
	private static final Logger log = Logger.getLogger( AbstractPersistentCollectionDescriptor.class );

	private final SessionFactoryImplementor sessionFactory;

	private final ManagedTypeDescriptor container;
	private final PluralPersistentAttribute attribute;
	private final NavigableRole navigableRole;
	private CollectionKey foreignKeyDescriptor;

	private Navigable foreignKeyTargetNavigable;

	private CollectionJavaDescriptor<C> javaTypeDescriptor;
	private CollectionMutabilityPlan mutabilityPlan;
	private CollectionIdentifier idDescriptor;
	private CollectionElement<E> elementDescriptor;
	private CollectionIndex indexDescriptor;

	private CollectionDataAccess cacheAccess;

	private CollectionLoader collectionLoader;
	private CollectionCreationExecutor collectionCreationExecutor;
	private CollectionRemovalExecutor collectionRemovalExecutor;
	private CollectionRowsDeletionExecutor collectionRowsDeletionExecutor;
	private CollectionRowsUpdateExecutor collectionRowsUpdateExecutor;
	private CollectionCreationExecutor collectionRowsInsertExecutor;
	private CollectionRowsIndexUpdateExecutor collectionRowsIndexUpdateExecutor;
	private CollectionSizeSelector collectionSizeSelector;
	private CollectionElementExistsSelector collectionElementExistsSelector;
	private CollectionIndexExistsSelector collectionIndexExistsSelector;
	private CollectionRowByIndexSelector collectionRowByIndexSelector;


	private final String mappedBy;
	private final String sqlWhereString;

	private boolean useOwnweIdentifier;

	// todo (6.0) - rework this (and friends) per todo item...
	//		* Redesign `org.hibernate.cache.spi.entry.CacheEntryStructure` and friends (with better names)
	// 			and make more efficient.  At the moment, to cache, we:
	//				.. Create a "cache entry" (object creation)
	//				.. "structure" the "cache entry" (object creation)
	//				.. add "structured data" to the cache.
	private final CacheEntryStructure cacheEntryStructure;

	private final String sqlAliasStem;

	private final JavaTypeDescriptor keyJavaTypeDescriptor;

	private final Set<String> spaces;

	private final int batchSize;
	private final boolean extraLazy;
	private final boolean hasOrphanDeletes;
	private final boolean inverse;

	private boolean cascadeDeleteEnabled;
	private boolean isRowInsertEnabled;
	private boolean isRowDeleteEnabled;
	private boolean fullyInitialized;
	private Table separateCollectionTable;
	private Table dmlTargetTable;


	@SuppressWarnings("unchecked")
	public AbstractPersistentCollectionDescriptor(
			Property pluralProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext creationContext) throws MappingException, CacheException {

		final Collection collectionBinding = (Collection) pluralProperty.getValue();

		this.sessionFactory = creationContext.getSessionFactory();
		this.container = runtimeContainer;

		this.navigableRole = container.getNavigableRole().append( pluralProperty.getName() );

		this.mutabilityPlan = determineMutabilityPlan( pluralProperty, creationContext );

		this.attribute = createAttribute(
				pluralProperty,
				runtimeContainer.getRepresentationStrategy().generatePropertyAccess(
						pluralProperty.getPersistentClass(),
						pluralProperty,
						runtimeContainer,
						sessionFactory.getSessionFactoryOptions().getBytecodeProvider()
				),
				creationContext
		);

		this.foreignKeyDescriptor = new CollectionKey( this, collectionBinding, creationContext );

		if ( sessionFactory.getSessionFactoryOptions().isStructuredCacheEntriesEnabled() ) {
			cacheEntryStructure = collectionBinding.isMap()
					? StructuredMapCacheEntry.INSTANCE
					: StructuredCollectionCacheEntry.INSTANCE;
		}
		else {
			cacheEntryStructure = UnstructuredCacheEntry.INSTANCE;
		}

		cacheAccess = creationContext.getCollectionCacheAccess( getNavigableRole() );

		int spacesSize = 1 + collectionBinding.getSynchronizedTables().size();
		spaces = new HashSet<>( spacesSize );
		spaces.add(
				collectionBinding.getMappedTable()
						.getNameIdentifier()
						.render( sessionFactory.getServiceRegistry().getService( JdbcServices.class ).getDialect() )
		);
		spaces.addAll( collectionBinding.getSynchronizedTables() );

		KeyValue key = collectionBinding.getKey();


		this.isRowDeleteEnabled = key.isNullable() && key.isUpdateable();
		this.isRowInsertEnabled = key.isUpdateable();

		this.keyJavaTypeDescriptor = key.getJavaTypeMapping().getJavaTypeDescriptor();

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( pluralProperty.getName() );

		int batch = collectionBinding.getBatchSize();
		if ( batch == -1 ) {
			batch = sessionFactory.getSessionFactoryOptions().getDefaultBatchFetchSize();
		}
		this.batchSize = batch;

		this.extraLazy = collectionBinding.isExtraLazy();
		this.hasOrphanDeletes = collectionBinding.hasOrphanDelete();
		this.inverse = collectionBinding.isInverse();
		this.mappedBy = collectionBinding.getMappedByProperty();

		this.sqlWhereString = StringHelper.isNotEmpty( collectionBinding.getWhere() )
				? "( " + collectionBinding.getWhere() + ") "
				: null;
	}

	protected static CollectionJavaDescriptor findJavaTypeDescriptor(
			Class javaType,
			RuntimeModelCreationContext creationContext) {
		final JavaTypeDescriptorRegistry jtdRegistry = creationContext.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry();

		CollectionJavaDescriptor descriptor = (CollectionJavaDescriptor) jtdRegistry.getDescriptor( javaType );
		if ( descriptor == null ) {
			throw new HibernateException( "Could not locate JavaTypeDescriptor for requested Java type : " + javaType.getName() );
		}

		return descriptor;
	}

	private CollectionMutabilityPlan determineMutabilityPlan(
			Property bootProperty,
			RuntimeModelCreationContext creationContext) {
		// todo (6.0) : implement this properly
		return CollectionMutabilityPlan.INSTANCE;

		// todo (6.0) : delegate this to CollectionSemantics?
		// 		support for users extending Hibernate with custom collection types
		//return collectionDescriptor.getSemantics().determineMutabilityPlan( ... );
	}

	protected Table resolveCollectionTable(
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext) {
		final MappedTable mappedTable = collectionBinding.getMappedTable();
		if ( mappedTable == null ) {
			return null;
		}

		return creationContext.resolve( mappedTable );
	}

	@Override
	public boolean finishInitialization(Collection bootCollectionDescriptor, RuntimeModelCreationContext creationContext) {
		if ( ! fullyInitialized ) {
			try {
				tryFinishInitialization( bootCollectionDescriptor, creationContext );
				fullyInitialized = true;
			}
			catch (Exception e) {
				log.debugf( e, "#finishInitialization resulted in error [%s]", bootCollectionDescriptor );
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean useOwnerIndetifier(){
		return useOwnweIdentifier;
	}

	@SuppressWarnings("WeakerAccess")
	protected void tryFinishInitialization(
			Collection bootCollectionDescriptor,
			RuntimeModelCreationContext creationContext) {
		final String referencedPropertyName = bootCollectionDescriptor.getReferencedPropertyName();
		if ( referencedPropertyName == null ) {
			useOwnweIdentifier = true;
			foreignKeyTargetNavigable = getContainer().findNavigable( EntityIdentifier.NAVIGABLE_ID );
		}
		else {
			foreignKeyTargetNavigable = getContainer().findPersistentAttribute( referencedPropertyName );
		}

		final Database database = creationContext.getMetadata().getDatabase();
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final MappedNamespace defaultNamespace = creationContext.getMetadata().getDatabase().getDefaultNamespace();

		final Identifier defaultCatalogName = defaultNamespace.getName().getCatalog();
		final String defaultCatalogNameString = defaultCatalogName == null ? null : defaultNamespace.getName().getCatalog().render( dialect );

		final Identifier defaultSchemaName = defaultNamespace.getName().getSchema();
		final String defaultSchemaNameString = defaultSchemaName == null ? null : defaultNamespace.getName().getSchema().render( dialect );

		if ( bootCollectionDescriptor instanceof IdentifierCollection ) {
			final IdentifierCollection identifierCollection = (IdentifierCollection) bootCollectionDescriptor;

			assert identifierCollection.getIdentifier().getColumnSpan() == 1;
			final Column idColumn = creationContext.getDatabaseObjectResolver().resolveColumn(
					(MappedColumn) identifierCollection.getIdentifier().getMappedColumns().get( 0 )
			);

			final IdentifierGenerator identifierGenerator = (identifierCollection).getIdentifier().createIdentifierGenerator(
					creationContext.getIdentifierGeneratorFactory(),
					dialect,
					defaultCatalogNameString,
					defaultSchemaNameString,
					null
			);

			this.idDescriptor = new CollectionIdentifier(
					this,
					idColumn,
					identifierGenerator
			);
		}
		else {
			idDescriptor = null;
		}

		if ( bootCollectionDescriptor instanceof IndexedCollection ) {
			this.indexDescriptor = resolveIndexDescriptor( this, (IndexedCollection) bootCollectionDescriptor, creationContext );
		}
		else {
			this.indexDescriptor = null;
		}

		// todo (6.0) : this is technically not the `separateCollectionTable` as for one-to-many it returns the element entity's table.
		//		need to decide how we want to model tables for collections.
		//
		//	^^ the better option seems to be exposing through `#createRootTableGroup` and `#createTableGroupJoin`
		if ( !( bootCollectionDescriptor.getElement() instanceof OneToMany ) ) {
			separateCollectionTable = resolveCollectionTable( bootCollectionDescriptor, creationContext );
		}

		this.elementDescriptor = resolveElementDescriptor( this, bootCollectionDescriptor, separateCollectionTable, creationContext );

		if ( !isOneToMany() ) {
			this.isRowDeleteEnabled = true;
			this.isRowInsertEnabled = true;

		}
		else {
			this.cascadeDeleteEnabled = bootCollectionDescriptor.getKey().isCascadeDeleteEnabled()
					&& creationContext.getSessionFactory().getDialect().supportsCascadeDelete();
		}

		this.javaTypeDescriptor = (CollectionJavaDescriptor<C>) bootCollectionDescriptor.getJavaTypeMapping().getJavaTypeDescriptor();

		this.collectionLoader = resolveCollectionLoader( bootCollectionDescriptor, creationContext );

		this.dmlTargetTable = resolveDmlTargetTable( separateCollectionTable, bootCollectionDescriptor, creationContext );
	}

	@SuppressWarnings("unchecked")
	private static <J,T extends Type<J>> CollectionIndex<J> resolveIndexDescriptor(
			PersistentCollectionDescriptor descriptor,
			IndexedCollection collectionBinding,
			RuntimeModelCreationContext creationContext) {
		final Value indexValueMapping = collectionBinding.getIndex();

		if ( indexValueMapping instanceof Any ) {
			throw new NotYetImplementedException(  );
		}

		if ( indexValueMapping instanceof BasicValueMapping ) {
			return new BasicCollectionIndexImpl(
					descriptor,
					collectionBinding,
					creationContext
			);
		}

		if ( indexValueMapping instanceof EmbeddedValueMapping ) {
			return new CollectionIndexEmbeddedImpl(
					descriptor,
					collectionBinding,
					creationContext
			);
		}

		if ( indexValueMapping instanceof OneToMany || indexValueMapping instanceof ManyToOne ) {
			// NOTE : ManyToOne is used to signify the index is a many-to-many
			return new CollectionIndexEntityImpl(
					descriptor,
					collectionBinding,
					creationContext
			);
		}

		throw new IllegalArgumentException(
				"Could not determine proper CollectionIndex descriptor to generate.  Unrecognized ValueMapping : " +
						indexValueMapping
		);
	}

	@SuppressWarnings("unchecked")
	private static CollectionElement resolveElementDescriptor(
			AbstractPersistentCollectionDescriptor descriptor,
			Collection bootCollectionDescriptor,
			Table separateCollectionTable,
			RuntimeModelCreationContext creationContext) {

		if ( bootCollectionDescriptor.getElement() instanceof Any ) {
			throw new NotYetImplementedException(  );
		}

		if ( bootCollectionDescriptor.getElement() instanceof BasicValueMapping ) {
			return new BasicCollectionElementImpl(
					descriptor,
					bootCollectionDescriptor,
					creationContext
			);
		}

		if ( bootCollectionDescriptor.getElement() instanceof EmbeddedValueMapping ) {
			return new CollectionElementEmbeddedImpl(
					descriptor,
					bootCollectionDescriptor,
					creationContext
			);
		}

		if ( bootCollectionDescriptor.getElement() instanceof ToOne ) {
			return new CollectionElementEntityImpl(
					descriptor,
					bootCollectionDescriptor,
					CollectionElement.ElementClassification.MANY_TO_MANY,
					creationContext
			);
		}

		if ( bootCollectionDescriptor.getElement() instanceof OneToMany ) {
			return new CollectionElementEntityImpl(
					descriptor,
					bootCollectionDescriptor,
					ONE_TO_MANY,
					creationContext
			);
		}

		throw new IllegalArgumentException(
				"Could not determine proper CollectionElement descriptor to generate.  Unrecognized ValueMapping : " +
						bootCollectionDescriptor.getElement()
		);
	}

	private CollectionLoader resolveCollectionLoader(
			Collection bootCollectionDescriptor,
			RuntimeModelCreationContext creationContext) {
		if ( StringHelper.isNotEmpty( bootCollectionDescriptor.getLoaderName() ) ) {
			// need a NamedQuery based CollectionLoader impl
			throw new NotYetImplementedFor6Exception();
		}

		return new CollectionLoaderImpl( getDescribedAttribute(), getSessionFactory() );
	}

	protected Table resolveDmlTargetTable(
			Table separateCollectionTable,
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext) {
		if ( separateCollectionTable != null ) {
			return separateCollectionTable;
		}

		assert getElementDescriptor().getClassification() == ONE_TO_MANY;

		final Table table = getElementDescriptor().getPrimaryDmlTable();
		if ( table == null ) {
			throw new IllegalStateException( "Could not determine DML target table for collection: " + this );
		}

		return table;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public JavaTypeDescriptor getKeyJavaTypeDescriptor() {
		return keyJavaTypeDescriptor;
	}

	@Override
	public Set<String> getCollectionSpaces() {
		return spaces;
	}

	@Override
	public void initialize(
			Object loadedKey,
			SharedSessionContractImplementor session) {
		getLoader().load( loadedKey, LockOptions.READ, session );
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}


	@Override
	public CollectionSemantics<C> getSemantics() {
		return getJavaTypeDescriptor().getSemantics();
	}

	@Override
	public TableGroup createRootTableGroup(
			String uid,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			JoinType tableReferenceJoinType,
			LockMode lockMode,
			SqlAstCreationState creationState) {
		final SqlAliasBase sqlAliasBase = creationState.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );

		RootTableReferenceCollectorImpl collector = new RootTableReferenceCollectorImpl(
				uid,
				navigablePath,
				this,
				explicitSourceAlias,
				lockMode
		);

		applyTableReferenceJoins(
				null,
				tableReferenceJoinType,
				sqlAliasBase,
				collector
		);

		return collector.generateTableGroup();
	}

	@Override
	public PersistentCollectionDescriptor getCollectionDescriptor() {
		return this;
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			String uid,
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			JoinType joinType,
			LockMode lockMode,
			SqlAstCreationState creationState) {
		final SqlAliasBase sqlAliasBase = creationState.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );

		final FetchedTableReferenceCollectorImpl joinCollector = new FetchedTableReferenceCollectorImpl(
				navigablePath,
				this,
				lhs,
				explicitSourceAlias,
				lockMode
		);

		applyTableReferenceJoins(
				lhs,
				joinType,
				sqlAliasBase,
				joinCollector
		);

		// handle optional entity references to be outer joins.
		if ( getDescribedAttribute().isNullable() && JoinType.INNER.equals( joinType ) ) {
			joinType = JoinType.LEFT;
		}

		return joinCollector.generateTableGroup( joinType, navigablePath.getFullPath() );
	}


	@Override
	public EntityTypeDescriptor findEntityOwnerDescriptor() {
		return findFirstEntityDescriptor();
	}

	protected abstract CollectionInitializerProducer createInitializerProducer(
			NavigablePath navigablePath,
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState);

	protected abstract AbstractPluralPersistentAttribute createAttribute(
			Property pluralProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext);

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return getDescribedAttribute().getMappedFetchStrategy();
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			String resultVariable,
			DomainResultCreationState creationState) {
		final LockMode lockMode = creationState.determineLockMode( resultVariable );
		return new CollectionResultImpl(
				attribute,
				navigablePath,
				resultVariable,
				lockMode,
				getCollectionKeyDescriptor().createDomainResult(
						navigablePath.append( "{key}" ),
						null,
						creationState
				),
				createInitializerProducer(
						navigablePath,
						null,
						true,
						resultVariable,
						lockMode,
						creationState
				)

		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		if ( !isArray() && fetchTiming == FetchTiming.DELAYED ) {
			// for delayed fetching, use a specialized Fetch impl
			return generateDelayedFetch(
					fetchParent,
					resultVariable,
					creationState
			);
		}
		else {
			return generateImmediateFetch(
					fetchParent,
					resultVariable,
					lockMode,
					creationState
			);
		}
	}

	private boolean isArray() {
		return getCollectionDescriptor().getCollectionClassification() == CollectionClassification.ARRAY;
	}

	@SuppressWarnings({"WeakerAccess", "unused"})
	protected Fetch generateDelayedFetch(
			FetchParent fetchParent,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new DelayedCollectionFetch(
				fetchParent,
				getDescribedAttribute(),
				resultVariable,
				getCollectionKeyDescriptor().createContainerResult(
						creationState.getFromClauseAccess().getTableGroup( fetchParent.getNavigablePath() ),
						creationState
				)
		);
	}

	private Fetch generateImmediateFetch(
			FetchParent fetchParent,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState) {
		final NavigablePath navigablePath = fetchParent.getNavigablePath().append( getNavigableName() );

		final TableGroup parentTableGroup = creationState.getFromClauseAccess().getTableGroup( navigablePath.getParent() );

		final TableGroup collectionTableGroup = creationState.getFromClauseAccess().resolveTableGroup(
				navigablePath,
				np -> {
					if ( parentTableGroup == null ) {
						throw new DomainResultCreationException( "Could not locate LHS TableGroup for collection fetch : " + navigablePath );
					}

					creationState.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );
					final TableGroupJoin tableGroupJoin = createTableGroupJoin(
							null,
							navigablePath,
							parentTableGroup,
							resultVariable,
							JoinType.INNER,
							lockMode,
							creationState.getSqlAstCreationState()
					);
					parentTableGroup.addTableGroupJoin( tableGroupJoin );

					return tableGroupJoin.getJoinedGroup();
				}
		);

		return CollectionFetchImpl.create(
				navigablePath,
				collectionTableGroup,
				parentTableGroup,
				fetchParent,
				getDescribedAttribute(),
				resultVariable,
				lockMode,
				createInitializerProducer(
						navigablePath,
						fetchParent,
						true,
						resultVariable,
						lockMode,
						creationState
				),
				creationState
		);
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		if ( getIndexDescriptor() instanceof Fetchable ) {
			fetchableConsumer.accept( (Fetchable) getIndexDescriptor() );
		}

		if ( getElementDescriptor() instanceof Fetchable ) {
			fetchableConsumer.accept( (Fetchable) getElementDescriptor() );
		}
	}

	/**
	 * @deprecated todo (6.0) remove
	 */
	@Override
	@Deprecated
	@Remove
	public CollectionSemantics getTuplizer() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ManagedTypeDescriptor getContainer() {
		return container;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String asLoggableText() {
		return String.format(
				Locale.ROOT,
				"%s(%s)",
				PersistentCollectionDescriptor.class.getSimpleName(),
				getNavigableRole().getFullPath()
		);
	}

	@Override
	public PluralPersistentAttribute getDescribedAttribute() {
		return attribute;
	}

	@Override
	public CollectionMutabilityPlan<C> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public CollectionKey getCollectionKeyDescriptor() {
		return foreignKeyDescriptor;
	}

	@Override
	public Navigable getForeignKeyTargetNavigable() {
		return foreignKeyTargetNavigable;
	}

	@Override
	public boolean contains(Object collection, E childObject) {
		return false;
	}

	@Override
	public CollectionIdentifier getIdDescriptor() {
		return idDescriptor;
	}

	@Override
	public CollectionElement<E> getElementDescriptor() {
		return elementDescriptor;
	}

	@Override
	public CollectionIndex getIndexDescriptor() {
		return indexDescriptor;
	}

	@Override
	public CollectionLoader getLoader() {
		return collectionLoader;
	}

	@Override
	public Table getSeparateCollectionTable() {
		return separateCollectionTable;
	}

	@Override
	public boolean isInverse() {
		return inverse;
	}

	@Override
	public boolean hasOrphanDelete() {
		return hasOrphanDeletes;
	}

	@Override
	public boolean isOneToMany() {
		return getElementDescriptor().getClassification() == ONE_TO_MANY;
	}

	@Override
	public boolean isExtraLazy() {
		return extraLazy;
	}

	@Override
	public boolean isDirty(Object old, Object value, SharedSessionContractImplementor session) {
		return getElementDescriptor().isDirty( old, value, session );
	}

	@Override
	public int getSize(Object loadedKey, SharedSessionContractImplementor session) {
		if ( collectionSizeSelector == null ) {
			collectionSizeSelector = new CollectionSizeSelector(
					this,
					dmlTargetTable,
					sqlWhereString,
					sessionFactory
			);
		}

		int baseIndex = getIndexDescriptor() != null ? getIndexDescriptor().getBaseIndex() : 0;
		return collectionSizeSelector.execute( loadedKey, session ) - baseIndex;
	}

	@Override
	public Boolean indexExists(
			Object loadedKey,
			Object index,
			SharedSessionContractImplementor session) {
		if ( collectionIndexExistsSelector == null ) {
			collectionIndexExistsSelector = new CollectionIndexExistsSelector(
					this,
					dmlTargetTable,
					sqlWhereString,
					sessionFactory
			);
		}
		return collectionIndexExistsSelector.indexExists(
				loadedKey,
				incrementIndexByBase( index ),
				session
		);
	}

	protected Object incrementIndexByBase(Object index) {
		int baseIndex = getIndexDescriptor().getBaseIndex();
		if ( baseIndex != 0 ) {
			index = (Integer) index + baseIndex;
		}
		return index;
	}

	@Override
	public Boolean elementExists(
			Object loadedKey,
			Object element,
			PersistentCollection collection,
			SharedSessionContractImplementor session) {
		if ( collectionElementExistsSelector == null ) {
			collectionElementExistsSelector = new CollectionElementExistsSelector(
					this,
					dmlTargetTable,
					sqlWhereString,
					sessionFactory
			);
		}
		return collectionElementExistsSelector.elementExists( loadedKey, element, collection, session );
	}

	@Override
	public Object getElementByIndex(Object loadedKey, Object index, SharedSessionContractImplementor session, Object owner) {
		if ( collectionRowByIndexSelector == null ) {
			collectionRowByIndexSelector = generateRowByIndexSelector();
		}
		return collectionRowByIndexSelector.execute( loadedKey, incrementIndexByBase( index ), session );
	}

	CollectionRowByIndexSelector generateRowByIndexSelector() {
		if ( isOneToMany() ) {
			throw new NotYetImplementedFor6Exception();
		}

		if ( !hasIndex() ) {
			return CollectionRowByIndexSelector.NO_OP;
		}

		return new JoinTableCollectionRowByIndexSelector( this, dmlTargetTable, sqlWhereString, sessionFactory );
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		return cacheEntryStructure;
	}

	@Override
	public CollectionDataAccess getCacheAccess() {
		return cacheAccess;
	}

	@Override
	public String getMappedByProperty() {
		return mappedBy;
	}

	@Override
	public CollectionJavaDescriptor<C> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public boolean isAffectedByEnabledFilters(SharedSessionContractImplementor session) {
		// todo (6.0) : implement this
		// for now, return false
		return false;
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {

		if ( separateCollectionTable != null ) {
			/*
			  For CollectionElementEntityImpl the previous call to getElementDescriptor().applyTableReferenceJoins(....) has already
			  added a PrimaryReference to the joinCollector so now we need to add a secondaryReference
			*/
			// todo (6.0) : is there a better way to manage this?
			if ( joinCollector.getPrimaryTableReference() != null ) {
				TableReference joinedTableReference = new TableReference(
						separateCollectionTable,
						sqlAliasBase.generateNewAlias(),
						false
				);
				joinCollector.addSecondaryReference(
						new TableReferenceJoin(
								JoinType.INNER,
								joinedTableReference,
								makePredicate(
										joinedTableReference,
										joinCollector.getPrimaryTableReference()
								)
						) );
			}
			else {
				joinCollector.addPrimaryReference( new TableReference(
						separateCollectionTable,
						sqlAliasBase.generateNewAlias(),
						false
				) );
			}
		}

		if ( getIndexDescriptor() != null ) {
			getIndexDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
		}

		getElementDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
	}

	/*
	 todo (6.0) : the quite same logic of this method is also in {@link FetchedTableReferenceCollectorImpl},
	 {@link ToOneJoinCollectorImpl}, {@link AbstractEntityTypeDescriptor}, {@link CollectionElementEntityImpl},
	 {@link CollectionIndexEntityImpl}
	  */
	private Predicate makePredicate(ColumnReferenceQualifier lhs, TableReference rhs) {
		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );
		for ( ForeignKey foreignKey : foreignKeyDescriptor.getJoinForeignKey().getReferringTable().getForeignKeys() ) {
			if ( foreignKey.getTargetTable().equals( rhs.getTable() ) ) {
				for(ForeignKey.ColumnMappings.ColumnMapping columnMapping : foreignKey.getColumnMappings().getColumnMappings()) {
					final ColumnReference referringColumnReference = lhs.resolveColumnReference( columnMapping.getReferringColumn() );
					final ColumnReference targetColumnReference = rhs.resolveColumnReference( columnMapping.getTargetColumn() );

					// todo (6.0) : we need some kind of validation here that the column references are properly defined

					// todo (6.0) : we could also handle this using SQL row-value syntax, e.g.:
					//		`... where ... [ (rCol1, rCol2, ...) = (tCol1, tCol2, ...) ] ...`

					conjunction.add(
							new ComparisonPredicate(
									referringColumnReference, ComparisonOperator.EQUAL,
									targetColumnReference
							)
					);
				}
				break;
			}
		}
		return conjunction;
	}

	@Override
	public void deleteRows(PersistentCollection collection, Object key, SharedSessionContractImplementor session) {
		if ( collectionRowsDeletionExecutor == null ) {
			collectionRowsDeletionExecutor = generateCollectionRowsDeletionExecutor();
		}
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Deleting rows of collection: %s",
					MessageHelper.collectionInfoString( this, collection, key, session )
			);
		}
		collectionRowsDeletionExecutor.execute( collection, key, session );
	}

	private CollectionRowsDeletionExecutor generateCollectionRowsDeletionExecutor() {
		if ( isInverse() || !isRowDeleteEnabled() ) {
			return CollectionRowsDeletionExecutor.NO_OP;
		}
		else if ( isOneToMany() ) {
			return new OneToManyRowsDeletionExecutor( this, sessionFactory, dmlTargetTable, hasIndex(), indexContainsFormula() );
		}
		else {
			return new JoinTableRowsDeleletionExecutor( this, sessionFactory, hasIndex() && !indexContainsFormula() );
		}
	}

	@Override
	public void insertRows(PersistentCollection collection, Object key, SharedSessionContractImplementor session) {
		if ( collectionRowsInsertExecutor == null ) {
			collectionRowsInsertExecutor = generateCollectionRowsInsertExecutor();
		}
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Inserting rows of collection: %s",
					MessageHelper.collectionInfoString( this, collection, key, session )
			);
		}
		collectionRowsInsertExecutor.execute( collection, key, session );
		writeIndex( collection, key, false, true, session );
	}

	private CollectionCreationExecutor generateCollectionRowsInsertExecutor() {
		if ( isInverse() || !isRowInsertEnabled() ) {
			return AbstractCreationExecutor.NO_OP;
		}
		else if ( isOneToMany() ) {
			return new OneToManyRowsInsertExecutor( this, dmlTargetTable, getSessionFactory() );
		}
		else {
			return new JoinTableRowsInsertExecutor( this, dmlTargetTable, getSessionFactory() );
		}
	}

	@Override
	public void updateRows(PersistentCollection collection, Object key, SharedSessionContractImplementor session) {
		if ( !isInverse() && collection.isRowUpdatePossible() ) {
			if ( collectionRowsUpdateExecutor == null ) {
				collectionRowsUpdateExecutor = generateCollectionRowsUpdateExecutor();
			}
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Updating rows of collection: %s",
						MessageHelper.collectionInfoString( this, collection, key, session )
				);
			}
			collectionRowsUpdateExecutor.execute( collection, key, session );
		}
	}

	protected CollectionRowsUpdateExecutor generateCollectionRowsUpdateExecutor() {
		if ( isInverse() ) {
			return CollectionRowsUpdateExecutor.NO_OP;
		}
		else if ( isOneToMany() ) {
			return new OneToManyRowsUpdateExecutor(
					this,
					dmlTargetTable,
					isRowDeleteEnabled,
					isRowInsertEnabled,
					hasIndex(),
					indexContainsFormula(),
					sessionFactory
			);
		}
		else {
			return new JoinTableRowsUpdateExecutor(
					this,
					dmlTargetTable,
					hasIndex(),
					indexContainsFormula(),
					sessionFactory
			);
		}
	}

	private CollectionRowsIndexUpdateExecutor generateCollectionRowsIndexExecutor() {
		if ( !( isOneToMany() && isInverse() && hasIndex() && !indexContainsFormula() && isIndexSettable() ) ){
			return CollectionRowsIndexUpdateExecutor.NO_OP;
		}

		return new OneToManyRowsIndexUpdateExecutor( this, dmlTargetTable, sessionFactory );
	}

	protected boolean hasIndex() {
		return false;
	}

	protected boolean isIndexSettable() {
		return getIndexDescriptor() != null && getIndexDescriptor().isSettable();
	}

	protected boolean indexContainsFormula() {
		return false;
	}

	@Override
	public void recreate(
			PersistentCollection collection,
			Object key,
			SharedSessionContractImplementor session) {
		if ( collectionCreationExecutor == null ) {
			collectionCreationExecutor = generateCollectionCreationExecutor();
		}

		if ( log.isDebugEnabled() ) {
			log.debugf( "Inserting collection: %s", LoggingHelper.toLoggableString( getNavigableRole(), key ) );
		}

		collectionCreationExecutor.execute( collection, key, session );
		writeIndex( collection, key, false, true, session );
	}

	private CollectionCreationExecutor generateCollectionCreationExecutor() {
		if ( isInverse() || !isRowInsertEnabled() ) {
			return CollectionCreationExecutor.NO_OP;
		}
		else if ( isOneToMany() ) {
			return new OneToManyCreationExecutor( this, dmlTargetTable, getSessionFactory() );
		}
		else {
			return new JoinTableCreationExecutor( this, dmlTargetTable, getSessionFactory() );
		}
	}

	protected boolean isRowInsertEnabled() {
		return isRowInsertEnabled;
	}

	@Override
	public void remove(Object key, SharedSessionContractImplementor session) {
		log.tracef( "Starting #remove(%s)", key );

		if ( collectionRemovalExecutor == null ) {
			collectionRemovalExecutor = generateCollectionRemovalExecutor();
		}

		if ( log.isDebugEnabled() ) {
			log.debug( "Deleting collection: " + LoggingHelper.toLoggableString( getNavigableRole(), key ) );
		}

		collectionRemovalExecutor.execute( key, session );
	}

	private CollectionRemovalExecutor generateCollectionRemovalExecutor() {
		if ( isInverse() ) {
			return (key, session) -> log.tracef( "Skipping remove for inverse collection" );
		}
		if ( !isRowDeleteEnabled() ) {
			return (key, session) -> log.tracef( "Skipping remove for collection - row deletion disabled" );
		}

		if ( isOneToMany() ) {
			return new OneToManyRemovalExecutor( this, dmlTargetTable, sessionFactory );

		}
		else {
			return new JoinTableRemovalExecutor( this, sessionFactory );
		}
	}

	protected boolean isRowDeleteEnabled() {
		return isRowDeleteEnabled;
	}

	@Override
	public Object getKeyOfOwner(Object owner, SessionImplementor session) {
		if ( getForeignKeyTargetNavigable() instanceof EntityIdentifier ) {
			return getContainer().findFirstEntityDescriptor().getIdentifier( owner, session );
		}
		else {
			return ( (SingularPersistentAttribute) getForeignKeyTargetNavigable() ).getPropertyAccess().getGetter().get( owner );
		}
	}

	@Override
	public void processQueuedOps(PersistentCollection collection, Object key, SharedSessionContractImplementor session) {
		if ( collection.hasQueuedOperations() && isOneToMany() ) {
			doProcessQueuedOps( collection, key, session );
			writeIndex( collection, key, true, false, session );
		}
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.TO_PARENT;
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	private void writeIndex(
			PersistentCollection collection,
			Object key,
			boolean queuedOperations,
			boolean resetIndex,
			SharedSessionContractImplementor session) {
		if ( collectionRowsIndexUpdateExecutor == null ) {
			collectionRowsIndexUpdateExecutor = generateCollectionRowsIndexExecutor();
		}

		collectionRowsIndexUpdateExecutor.execute( collection, key, queuedOperations, resetIndex, session );
	}

	protected abstract void doProcessQueuedOps(
			PersistentCollection collection,
			Object id,
			SharedSessionContractImplementor session);
}
