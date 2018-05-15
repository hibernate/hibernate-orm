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
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.internal.SqlAliasStemHelper;
import org.hibernate.metamodel.model.domain.internal.collection.BasicCollectionElementImpl;
import org.hibernate.metamodel.model.domain.internal.collection.BasicCollectionIndexImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionCreationExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionElementEmbeddedImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionElementEntityImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionIndexEmbeddedImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionIndexEntityImpl;
import org.hibernate.metamodel.model.domain.internal.collection.CollectionRemovalExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.FetchedTableReferenceCollectorImpl;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableCreationExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.JoinTableRemovalExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyCreationExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.OneToManyRemovalExecutor;
import org.hibernate.metamodel.model.domain.internal.collection.RootTableReferenceCollectorImpl;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.naming.Identifier;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.results.internal.domain.collection.CollectionFetchImpl;
import org.hibernate.sql.results.internal.domain.collection.CollectionInitializerProducer;
import org.hibernate.sql.results.internal.domain.collection.CollectionResultImpl;
import org.hibernate.sql.results.internal.domain.collection.DelayedCollectionFetch;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;

import org.jboss.logging.Logger;

import static org.hibernate.metamodel.model.domain.spi.CollectionElement.ElementClassification.ONE_TO_MANY;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPersistentCollectionDescriptor<O,C,E> implements PersistentCollectionDescriptor<O,C,E> {
	private static final Logger log = Logger.getLogger( AbstractPersistentCollectionDescriptor.class );

	private final SessionFactoryImplementor sessionFactory;

	private final ManagedTypeDescriptor container;
	private final PluralPersistentAttribute attribute;
	private final NavigableRole navigableRole;
	private final CollectionKey foreignKeyDescriptor;

	private Navigable foreignKeyTargetNavigable;

	private CollectionJavaDescriptor<C> javaTypeDescriptor;
	private CollectionMutabilityPlan mutabilityPlan;
	private CollectionIdentifier idDescriptor;
	private CollectionElement elementDescriptor;
	private CollectionIndex indexDescriptor;

	private CollectionDataAccess cacheAccess;

	private CollectionLoader collectionLoader;
	private CollectionCreationExecutor collectionCreationExecutor;
	private CollectionRemovalExecutor collectionRemovalExecutor;


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

		this.keyJavaTypeDescriptor = collectionBinding.getKey().getJavaTypeMapping().getJavaTypeDescriptor();

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( pluralProperty.getName() );

		int batch = collectionBinding.getBatchSize();
		if ( batch == -1 ) {
			batch = sessionFactory.getSessionFactoryOptions().getDefaultBatchFetchSize();
		}
		this.batchSize = batch;

		this.separateCollectionTable = resolveCollectionTable( collectionBinding, creationContext );

		this.extraLazy = collectionBinding.isExtraLazy();
		this.hasOrphanDeletes = collectionBinding.hasOrphanDelete();
		this.inverse = collectionBinding.isInverse();
	}

	protected static CollectionJavaDescriptor findCollectionJtd(
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

	@SuppressWarnings("WeakerAccess")
	protected void tryFinishInitialization(
			Collection bootCollectionDescriptor,
			RuntimeModelCreationContext creationContext) {
		final String referencedPropertyName = bootCollectionDescriptor.getReferencedPropertyName();
		if ( referencedPropertyName == null ) {
			foreignKeyTargetNavigable = getContainer().findNavigable( EntityIdentifier.NAVIGABLE_ID );
		}
		else {
			foreignKeyTargetNavigable = getContainer().findPersistentAttribute( referencedPropertyName );
		}

		// todo (6.0) : this is technically not the `separateCollectionTable` as for one-to-many it returns the element entity's table.
		//		need to decide how we want to model tables for collections.
		//
		//	^^ the better option seems to be exposing through `#createRootTableGroup` and `#createTableGroupJoin`

//		separateCollectionTable = resolveCollectionTable( bootCollectionDescriptor, creationContext );

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
					( (BasicValueMapping) identifierCollection.getIdentifier() ).resolveType(),
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

		this.elementDescriptor = resolveElementDescriptor( this, bootCollectionDescriptor, separateCollectionTable, creationContext );

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
		if ( fetchTiming == FetchTiming.DELAYED ) {
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
					selected,
					lockMode,
					creationState,
					creationContext
			);
		}
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
	public CollectionElement getElementDescriptor() {
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
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public int getSize(Object loadedKey, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Boolean indexExists(
			Object loadedKey, Object index, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Boolean elementExists(
			Object loadedKey, Object element, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Object getElementByIndex(
			Object loadedKey, Object index, SharedSessionContractImplementor session, Object owner) {
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
		throw new NotYetImplementedFor6Exception();
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
			joinCollector.addPrimaryReference( new TableReference(
					separateCollectionTable,
					sqlAliasBase.generateNewAlias(),
					false
			) );
		}

		if ( getIndexDescriptor() != null ) {
			getIndexDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
		}

		getElementDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
	}


	@Override
	public void recreate(
			PersistentCollection collection,
			Object key,
			SharedSessionContractImplementor session) {
//
//		if ( isInverse() ) {
//			// EARLY EXIT!!
//			return;
//		}
//
//		if ( !isRowInsertEnabled() ) {
//			// EARLY EXIT!!
//			return;
//		}
//
		if ( collectionCreationExecutor == null ) {
			collectionCreationExecutor = generateCollectionCreationExecutor();
		}

		if ( log.isDebugEnabled() ) {
			log.debugf( "Inserting collection: %s", LoggingHelper.toLoggableString( getNavigableRole(), key ) );
		}

		collectionCreationExecutor.create( collection, key, session );
	}

	private CollectionCreationExecutor generateCollectionCreationExecutor() {
		if ( isInverse() || ! isRowInsertEnabled() ) {
			return CollectionCreationExecutor.NO_OP;
		}
		else if ( getSeparateCollectionTable() != null ) {
			return new JoinTableCreationExecutor( this, dmlTargetTable, getSessionFactory() );
		}
		else {
			assert getElementDescriptor().getClassification() == ONE_TO_MANY;
			return new OneToManyCreationExecutor( this, dmlTargetTable, getSessionFactory() );
		}
	}

	protected boolean isRowInsertEnabled() {
		return true;
	}

	@Override
	public void remove(Object key, SharedSessionContractImplementor session) {
		log.tracef( "Starting #remove(%s)", key );

//		if ( isInverse() ) {
//			// EARLY EXIT!!
//			log.tracef( "Skipping remove for inverse collection" );
//			return;
//		}
//
//		if ( ! isRowDeleteEnabled() ) {
//			// EARLY EXIT!!
//			log.tracef( "Skipping remove for collection - row deletion disabled" );
//			return;
//		}

		if ( collectionRemovalExecutor == null ) {
			collectionRemovalExecutor = generateCollectionRemovalExecutor();
		}

		if ( log.isDebugEnabled() ) {
			log.debug( "Deleting collection: " + LoggingHelper.toLoggableString( getNavigableRole(), key ) );
		}

		collectionRemovalExecutor.remove( key, session );

	}

	private CollectionRemovalExecutor generateCollectionRemovalExecutor() {
		if ( isInverse() || ! isRowDeleteEnabled() ) {
			return CollectionRemovalExecutor.NO_OP;
		}
		else if ( getSeparateCollectionTable() != null ) {
			return new JoinTableRemovalExecutor( this, sessionFactory );
		}
		else {
			assert getElementDescriptor().getClassification() == ONE_TO_MANY;
			return new OneToManyRemovalExecutor( this, sessionFactory );
		}
	}

	protected boolean isRowDeleteEnabled() {
		return true;
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
}
