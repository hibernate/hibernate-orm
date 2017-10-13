/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.StructuredCollectionCacheEntry;
import org.hibernate.cache.spi.entry.StructuredMapCacheEntry;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.internal.BasicCollectionElementImpl;
import org.hibernate.metamodel.model.domain.internal.CollectionElementEmbeddedImpl;
import org.hibernate.metamodel.model.domain.internal.CollectionElementEntityImpl;
import org.hibernate.metamodel.model.domain.internal.BasicCollectionIndexImpl;
import org.hibernate.metamodel.model.domain.internal.CollectionIndexEmbeddedImpl;
import org.hibernate.metamodel.model.domain.internal.CollectionIndexEntityImpl;
import org.hibernate.metamodel.model.domain.internal.SqlAliasStemHelper;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.type.Type;

import static org.hibernate.metamodel.model.domain.internal.PersisterHelper.interpretCollectionClassification;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPersistentCollectionDescriptor<O,C,E> implements PersistentCollectionDescriptor<O,C,E> {
	private final SessionFactoryImplementor sessionFactory;
	private final ManagedTypeDescriptor source;

	private final NavigableRole navigableRole;
	private final CollectionClassification collectionClassification;

	private final CollectionKey foreignKeyDescriptor;

	// todo (6.0) - rework this (and friend) per todo item...
	//		* Redesign `org.hibernate.cache.spi.entry.CacheEntryStructure` and friends (with better names)
	// 			and make more efficient.  At the moment, to cache, we:
	//				.. Create a "cache entry" (object creation)
	//				.. "structure" the "cache entry" (object creation)
	//				.. add "structured data" to the cache.
	private final CacheEntryStructure cacheEntryStructure;


	private CollectionIdentifier idDescriptor;
	private CollectionElement elementDescriptor;
	private CollectionIndex indexDescriptor;

	private Table separateCollectionTable;

	private final String sqlAliasStem;

	private final org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor keyJavaTypeDescriptor;

	private final String[] spaces;

	private final int batchSize;

	public AbstractPersistentCollectionDescriptor(
			Collection collectionBinding,
			ManagedTypeDescriptor source,
			String navigableName,
			RuntimeModelCreationContext creationContext) throws MappingException, CacheException {
		this.sessionFactory = creationContext.getSessionFactory();
		this.source = source;
		this.navigableRole = source.getNavigableRole().append( navigableName );
		this.collectionClassification = interpretCollectionClassification( collectionBinding );
		this.foreignKeyDescriptor = new CollectionKey( this, collectionBinding );

		if ( sessionFactory.getSessionFactoryOptions().isStructuredCacheEntriesEnabled() ) {
			cacheEntryStructure = collectionBinding.isMap()
					? StructuredMapCacheEntry.INSTANCE
					: StructuredCollectionCacheEntry.INSTANCE;
		}
		else {
			cacheEntryStructure = UnstructuredCacheEntry.INSTANCE;
		}

		int spacesSize = 1 + collectionBinding.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = collectionBinding
				.getMappedTable()
				.getNameIdentifier()
				.render( sessionFactory.getServiceRegistry().getService( JdbcServices.class ).getDialect() );

		Iterator iter = collectionBinding.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = (String) iter.next();
		}

		this.keyJavaTypeDescriptor = collectionBinding.getKey().getJavaTypeDescriptor();

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( navigableName );

		int batch = collectionBinding.getBatchSize();
		if ( batch == -1 ) {
			batch = sessionFactory.getSessionFactoryOptions().getDefaultBatchFetchSize();
		}
		batchSize = batch;

	}

	@Override
	public void finishInitialization(
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext) {

		// todo (6.0) : this is technically not the `separateCollectionTable` as for one-to-many it returns the element entity's table.
		//		need to decide how we want to model tables for collections.
		separateCollectionTable = resolveCollectionTable( collectionBinding, creationContext );

		final Database database = creationContext.getMetadata().getDatabase();
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final MappedNamespace defaultNamespace = creationContext.getMetadata().getDatabase().getDefaultNamespace();
		final String defaultCatalogName = defaultNamespace == null ? null : defaultNamespace.getName().getCatalog().render( dialect );
		final String defaultSchemaName = defaultNamespace == null ? null : defaultNamespace.getName().getSchema().render( dialect );

		if ( collectionBinding instanceof IdentifierCollection ) {
			final IdentifierGenerator identifierGenerator = ( (IdentifierCollection) collectionBinding ).getIdentifier().createIdentifierGenerator(
					creationContext.getIdentifierGeneratorFactory(),
					dialect,
					defaultCatalogName,
					defaultSchemaName,
					null
			);
			this.idDescriptor = new CollectionIdentifier(
					( (BasicValueMapping) ( (IdentifierCollection) collectionBinding ).getIdentifier() ).resolveType(),
					identifierGenerator
			);
		}
		else {
			idDescriptor = null;
		}

		this.indexDescriptor = resolveIndexDescriptor( this, collectionBinding, creationContext );
		this.elementDescriptor = resolveElementDescriptor( this, collectionBinding, separateCollectionTable, creationContext );
	}

	@SuppressWarnings("unchecked")
	private static <J,T extends Type<J>> CollectionIndex<J> resolveIndexDescriptor(
			PersistentCollectionDescriptor persister,
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext) {
		if ( !IndexedCollection.class.isInstance( collectionBinding ) ) {
			return null;
		}

		final IndexedCollection indexedCollectionMapping = (IndexedCollection) collectionBinding;
		final Value indexValueMapping = indexedCollectionMapping.getIndex();

		if ( indexValueMapping instanceof Any ) {
			throw new NotYetImplementedException(  );
		}

		if ( indexValueMapping instanceof BasicValueMapping ) {
			return new BasicCollectionIndexImpl(
					persister,
					indexedCollectionMapping,
					creationContext
			);
		}

		if ( indexValueMapping instanceof EmbeddedValueMapping ) {
			return new CollectionIndexEmbeddedImpl(
					persister,
					indexedCollectionMapping,
					creationContext
			);
		}

		if ( indexValueMapping instanceof OneToMany || indexValueMapping instanceof ManyToOne ) {
			// NOTE : ManyToOne is used to signify the index is a many-to-many
			return new CollectionIndexEntityImpl(
					persister,
					indexedCollectionMapping,
					creationContext
			);
		}

		throw new IllegalArgumentException(
				"Could not determine proper CollectionIndex descriptor to generate.  Unrecognized ValueMapping : " +
						indexValueMapping
		);
	}

	protected abstract Table resolveCollectionTable(
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext);


	@SuppressWarnings("unchecked")
	private static CollectionElement resolveElementDescriptor(
			AbstractPersistentCollectionDescriptor collectionPersister,
			Collection bootCollectionDescriptor,
			Table separateCollectionTable,
			RuntimeModelCreationContext creationContext) {

		if ( bootCollectionDescriptor.getElement() instanceof Any ) {
			throw new NotYetImplementedException(  );
		}

		if ( bootCollectionDescriptor.getElement() instanceof BasicValueMapping ) {
			return new BasicCollectionElementImpl(
					collectionPersister,
					bootCollectionDescriptor,
					creationContext
			);
		}

		if ( bootCollectionDescriptor.getElement() instanceof EmbeddedValueMapping ) {
			return new CollectionElementEmbeddedImpl(
					collectionPersister,
					bootCollectionDescriptor,
					creationContext
			);
		}

		if ( bootCollectionDescriptor.getElement() instanceof ToOne ) {
			return new CollectionElementEntityImpl(
					collectionPersister,
					bootCollectionDescriptor,
					separateCollectionTable == null
							? CollectionElement.ElementClassification.MANY_TO_MANY
							: CollectionElement.ElementClassification.ONE_TO_MANY,
					creationContext
			);
		}

		throw new IllegalArgumentException(
				"Could not determine proper CollectionElement descriptor to generate.  Unrecognized ValueMapping : " +
						bootCollectionDescriptor.getElement()
		);
	}


	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor getKeyJavaTypeDescriptor() {
		return keyJavaTypeDescriptor;
	}

	@Override
	public String[] getCollectionSpaces() {
		return spaces;
	}

	@Override
	public void initialize(
			Serializable loadedKey,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public TableGroup createRootTableGroup(
			TableGroupInfo tableGroupInfo,
			RootTableGroupContext tableGroupContext) {
		throw new NotYetImplementedFor6Exception(  );
//		final SqlAliasBase sqlAliasBase = tableGroupContext.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );
//		final RootTableGroupTableReferenceCollector collector = new RootTableGroupTableReferenceCollector( this, sqlAliasBase );
//		applyTableReferenceJoins(
//				null,
//				JoinType.INNER,
//				sqlAliasBase,
//				collector,
//				tableGroupContext
//		);
//		return null;
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
	//			c) @OneToOne or @ManyToOne - essentially the same as (b) but with
	//				UKs defined on link table restricting cardinality
	//		2) no separate collection table - only valid for @OneToOne or @ManyToOne, although (1.c)
	//			for alternative mapping for @OneToOne or @ManyToOne.  Here the "collection table"
	//			is the primary table for the associated entity

//	private static class RootTableGroupTableReferenceCollector implements TableReferenceJoinCollector {
//		private final AbstractPersistentCollectionMetadata collectionMetadata;
//		private final TableReference collectionTableReference;
//		private TableReference primaryTableReference;
//
//		public RootTableGroupTableReferenceCollector(
//				AbstractPersistentCollectionMetadata collectionMetadata,
//				SqlAliasBase sqlAliasBase) {
//			this.collectionMetadata = collectionMetadata;
//
//			if ( collectionMetadata.separateCollectionTable != null ) {
//				this.collectionTableReference = new TableReference( collectionMetadata.separateCollectionTable, sqlAliasBase.generateNewAlias() );
//				primaryTableReference = collectionTableReference;
//			}
//			else {
//
//			}
//		}
//
//		@Override
//		public void addRoot(TableReference root) {
//
//		}
//
//		@Override
//		public void collectTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
//
//		}
//	}

//	@Override
//	public CollectionTableGroup createRootTableGroup(
//			NavigableReferenceInfo navigableReferenceInfo,
//			RootTableGroupContext tableGroupContext,
//			SqlAliasBaseGenerator sqlAliasBaseResolver) {
//		final SqlAliasBase sqlAliasBase = sqlAliasBaseResolver.getSqlAliasBase( navigableReferenceInfo );
//
//		final TableReference collectionTableReference;
//		if ( separateCollectionTable != null ) {
//			collectionTableReference = new TableReference( separateCollectionTable, sqlAliasBase.generateNewAlias() );
//		}
//		else {
//			collectionTableReference = null;
//		}
//
//		final ElementColumnReferenceSource elementTableGroup = new ElementColumnReferenceSource(
//				navigableReferenceInfo.getUniqueIdentifier()
//		);
//		getElementDescriptor().applyTableReferenceJoins(
//				JoinType.LEFT_OUTER_JOIN,
//				sqlAliasBase,
//				elementTableGroup
//		);
//
//		final IndexColumnReferenceSource indexTableGroup;
//		if ( getIndexDescriptor() != null ) {
//			indexTableGroup = new IndexColumnReferenceSource(
//					navigableReferenceInfo.getUniqueIdentifier()
//			);
//			getIndexDescriptor().applyTableReferenceJoins(
//					JoinType.LEFT_OUTER_JOIN,
//					sqlAliasBase,
//					elementTableGroup
//			);
//		}
//		else {
//			indexTableGroup = null;
//		}
//
//		return new CollectionTableGroup(
//				this,
//				tableGroupContext.getTableSpace(),
//				navigableReferenceInfo.getUniqueIdentifier(),
//				collectionTableReference,
//				elementTableGroup,
//				indexTableGroup
//		);
//	}


	@Override
	public TableGroupJoin createTableGroupJoin(
			TableGroupInfo tableGroupInfoSource,
			JoinType joinType,
			JoinedTableGroupContext tableGroupJoinContext) {
		throw new NotYetImplementedFor6Exception(  );
	}

}
