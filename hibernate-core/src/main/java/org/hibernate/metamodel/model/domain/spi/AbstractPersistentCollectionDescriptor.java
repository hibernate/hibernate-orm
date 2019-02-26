/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.hibernate.internal.util.collections.Stack;
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
import org.hibernate.metamodel.model.domain.internal.collection.BasicCollectionRowsUpdateExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionCreationExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionElementEmbeddedImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionElementEntityImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionIndexEmbeddedImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionIndexEntityImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionRemovalExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionRowsDeletionExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionRowsIndexUpdateExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionRowsUpdateExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionSizeExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.FetchedTableReferenceCollectorImpl;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableCreationExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableRemovalExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableRowsDeleletionExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableRowsInsertExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyCreationExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyRowsInsertExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyRemovalExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyRowsDeletionExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyRowsIndexUpdateExecutor;
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
import org.hibernate.query.sqm.produce.internal.UniqueIdGenerator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.internal.domain.collection.CollectionFetchImpl;
import org.hibernate.sql.results.internal.domain.collection.CollectionInitializerProducer;
import org.hibernate.sql.results.internal.domain.collection.CollectionResultImpl;
import org.hibernate.sql.results.internal.domain.collection.DelayedCollectionFetch;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.IntegerJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;

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
	private CollectionSizeExecutor collectionSizeExecutor;

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
			TableGroupInfo tableGroupInfo,
			RootTableGroupContext tableGroupContext) {
		final SqlAliasBase sqlAliasBase = tableGroupContext.getSqlAliasBaseGenerator().createSqlAliasBase(
				getSqlAliasStem()
		);

		RootTableReferenceCollectorImpl collector = new RootTableReferenceCollectorImpl(
				tableGroupContext.getTableSpace(),
				this,
				tableGroupInfo.getNavigablePath(),
				tableGroupInfo.getUniqueIdentifier(),
				tableGroupContext.getLockOptions().getEffectiveLockMode( tableGroupInfo.getIdentificationVariable() )
		);

		applyTableReferenceJoins(
				null,
				tableGroupContext.getTableReferenceJoinType(),
				sqlAliasBase,
				collector
		);

		return collector.generateTableGroup();
	}

	@Override
	public PersistentCollectionDescriptor getCollectionDescriptor() {
		return this;
	}


	// ultimately, "inclusion" in a collection must defined through a single table whether
	// that be:
	//		1) a "separate" collection table (@JoinTable) - could be either:
	//			a) an @ElementCollection - element/index value are contained on this separate table
	//			b) @ManyToMany - the separate table is an association table with column(s) that define the
	//				FK to an entity table.  NOTE that this is true for element and/or index -
	//				The element must be defined via the FK.  In this model, the index could be:
	// 					1) column(s) on the collection table pointing to the tables for
	// 						the entity that defines the index - only valid for map-keys that
	// 						are entities
	//					2) a basic/embedded value on the collection table
	//					3) a basic/embedded value on the element entity table
	//			c) @OneToMany with join table - essentially the same as (b) but with
	//				UKs defined on link table restricting cardinality
	//		2) no separate collection table - @OneToMany w/o join table

	@Override
	public TableGroupJoin createTableGroupJoin(
			TableGroupInfo tableGroupInfoSource,
			JoinType joinType,
			JoinedTableGroupContext tableGroupJoinContext) {
		return createTableGroupJoin(
				tableGroupJoinContext.getSqlAliasBaseGenerator(),
				tableGroupJoinContext.getLhs(),
				tableGroupJoinContext.getSqlExpressionResolver(),
				tableGroupJoinContext.getNavigablePath(),
				joinType,
				tableGroupInfoSource.getIdentificationVariable(),
				tableGroupJoinContext.getLockOptions().getEffectiveLockMode( tableGroupInfoSource.getIdentificationVariable() ),
				tableGroupJoinContext.getTableSpace()
		);
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			NavigableContainerReference lhs,
			SqlExpressionResolver sqlExpressionResolver,
			NavigablePath navigablePath,
			JoinType joinType,
			String identificationVariable,
			LockMode lockMode,
			TableSpace tableSpace) {
		final SqlAliasBase sqlAliasBase = sqlAliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() );

		final FetchedTableReferenceCollectorImpl joinCollector = new FetchedTableReferenceCollectorImpl(
				this,
				tableSpace,
				lhs,
				sqlExpressionResolver,
				navigablePath,
				lockMode
		);

		applyTableReferenceJoins(
				lhs.getColumnReferenceQualifier(),
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

	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {

		final DomainResult keyCollectionResult = getCollectionKeyDescriptor().createCollectionResult(
				navigableReference.getColumnReferenceQualifier(),
				creationState,
				creationContext
		);

		final LockMode lockMode = creationState.determineLockMode( resultVariable );

		return new CollectionResultImpl(
				getDescribedAttribute(),
				navigableReference.getNavigablePath(),
				resultVariable,
				lockMode,
				keyCollectionResult,
				createInitializerProducer(
						null,
						true,
						resultVariable,
						lockMode,
						creationState,
						creationContext
				)
		);
	}

	protected abstract CollectionInitializerProducer createInitializerProducer(
			FetchParent fetchParent,
			boolean selected,
			String resultVariable,
			LockMode lockMode,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext);

	protected abstract AbstractPluralPersistentAttribute createAttribute(
			Property pluralProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext);

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return getDescribedAttribute().getMappedFetchStrategy();
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		if ( !isArray() && fetchTiming == FetchTiming.DELAYED ) {
			// for delayed fetching, use a specialized Fetch impl
			return generateDelayedFetch(
					fetchParent,
					resultVariable,
					creationState,
					creationContext
			);
		}
		else {
			return generateImmediateFetch(
					fetchParent,
					resultVariable,
					isArray() || selected,
					lockMode,
					creationState,
					creationContext
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
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new DelayedCollectionFetch(
				fetchParent,
				getDescribedAttribute(),
				resultVariable,
				getCollectionKeyDescriptor().createContainerResult(
						creationState.getColumnReferenceQualifierStack().getCurrent(),
						creationState,
						creationContext
				)
		);
	}

	private Fetch generateImmediateFetch(
			FetchParent fetchParent,
			String resultVariable,
			boolean selected,
			LockMode lockMode,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		final Stack<NavigableReference> navigableReferenceStack = creationState.getNavigableReferenceStack();
		final NavigableContainerReference parentReference = (NavigableContainerReference) navigableReferenceStack.getCurrent();

		// if there is an existing NavigableReference this fetch can use, use it.  otherwise create one
		NavigableReference navigableReference = parentReference.findNavigableReference( getNavigableName() );
		if ( navigableReference == null ) {
			if ( selected ) {
				// this creates the SQL AST join(s) in the from clause
				final TableGroupJoin tableGroupJoin = createTableGroupJoin(
						creationState.getSqlAliasBaseGenerator(),
						parentReference,
						creationState.getSqlExpressionResolver(),
						fetchParent.getNavigablePath().append( getNavigableName() ),
						getDescribedAttribute().isNullable() ? JoinType.LEFT : JoinType.INNER,
						resultVariable,
						lockMode,
						creationState.getCurrentTableSpace()
				);
				creationState.getCurrentTableSpace().addJoinedTableGroup( tableGroupJoin );
				navigableReference = tableGroupJoin.getJoinedGroup().getNavigableReference();
			}
		}

		if ( navigableReference != null ) {
			assert navigableReference.getNavigable() instanceof CollectionValuedNavigable;
			creationState.getNavigableReferenceStack().push( navigableReference );
			creationState.getColumnReferenceQualifierStack().push( navigableReference.getColumnReferenceQualifier() );
		}

		try {
			return CollectionFetchImpl.create(
					fetchParent,
					getDescribedAttribute(),
					selected,
					resultVariable,
					lockMode,
					createInitializerProducer(
							fetchParent,
							selected,
							resultVariable,
							lockMode,
							creationState,
							creationContext
					),
					creationState,
					creationContext
			);
		}
		finally {
			if ( navigableReference != null ) {
				creationState.getColumnReferenceQualifierStack().pop();
				creationState.getNavigableReferenceStack().pop();
			}
		}
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
	public boolean contains(Object collection, Object childObject) {
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
		if ( collectionSizeExecutor == null ) {
			final boolean isMap = getCollectionClassification() == CollectionClassification.MAP;
			collectionSizeExecutor = new CollectionSizeExecutor(
					this,
					dmlTargetTable,
					hasIndex() && !isMap,
					sqlWhereString,
					sessionFactory
			);
		}

		int baseIndex = getIndexDescriptor() != null ? getIndexDescriptor().getBaseIndex() : 0;
		return collectionSizeExecutor.execute( loadedKey, session ) - baseIndex;
	}

	@Override
	public Boolean indexExists(Object loadedKey, Object index, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Boolean elementExists(Object loadedKey, Object element, SharedSessionContractImplementor session) {
		final SqlAliasBaseManager aliasBaseManager = new SqlAliasBaseManager();
		final UniqueIdGenerator uidGenerator = new UniqueIdGenerator();
		final String uid = uidGenerator.generateUniqueId();

		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec );
		final SelectClause selectClause = selectStatement.getQuerySpec().getSelectClause();

		final TableSpace rootTableSpace = rootQuerySpec.getFromClause().makeTableSpace();

		final NavigablePath path = new NavigablePath( getNavigableName() );

		final TableGroup rootTableGroup = createRootTableGroup(
				new TableGroupInfo() {
					@Override
					public String getUniqueIdentifier() {
						return uid;
					}

					@Override
					public String getIdentificationVariable() {
						return "this";
					}

					@Override
					public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
						return null;
					}

					@Override
					public NavigablePath getNavigablePath() {
						return path;
					}
				},
				new RootTableGroupContext() {
					@Override
					public void addRestriction(Predicate predicate) {
						rootQuerySpec.addRestriction( predicate );
					}

					@Override
					public QuerySpec getQuerySpec() {
						return rootQuerySpec;
					}

					@Override
					public TableSpace getTableSpace() {
						return rootTableSpace;
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return aliasBaseManager;
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						return null;
					}

					@Override
					public LockOptions getLockOptions() {
						return LockOptions.NONE;
					}
				}
		);
		rootTableSpace.setRootTableGroup( rootTableGroup );

		final List<DomainResult> domainResults = new ArrayList<>();

		final List<ColumnReference> columnReferences = new ArrayList();

		final SqlExpressableType sqlExpressableType = IntegerSqlDescriptor.INSTANCE.getSqlExpressableType(
				IntegerJavaDescriptor.INSTANCE,
				sessionFactory.getTypeConfiguration()
		);
		SqlSelection sqlSelection = new SqlSelectionImpl(
				1,
				0,
				new QueryLiteral(
						1,
						sqlExpressableType,
						Clause.SELECT
				),
				sqlExpressableType
		);


		throw new NotYetImplementedFor6Exception();

	}

	public class Position {
		int jdbcPosition = 1;
		int valuesArrayPosition = 0;

		public void increase() {
			jdbcPosition++;
			valuesArrayPosition++;
		}

		public int getJdbcPosition() {
			return jdbcPosition;
		}

		public int getValuesArrayPosition() {
			return valuesArrayPosition;
		}
	}

	@Override
	public Object getElementByIndex(Object loadedKey, Object index, SharedSessionContractImplementor session, Object owner) {
		throw new NotYetImplementedFor6Exception();
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
		collectionRowsDeletionExecutor.execute( collection, key, session );
	}

	private CollectionRowsDeletionExecutor generateCollectionRowsDeletionExecutor() {
		if ( isInverse() || !isRowDeleteEnabled() ) {
			return CollectionRowsDeletionExecutor.NO_OP;
		}
		else if ( isOneToMany() ) {
			return new OneToManyRowsDeletionExecutor( this, sessionFactory, dmlTargetTable, hasIndex() && !indexContainsFormula() );
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
					sessionFactory
			);
		}
		else {
			return new BasicCollectionRowsUpdateExecutor(
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
