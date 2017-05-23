/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.StructuredCollectionCacheEntry;
import org.hibernate.cache.spi.entry.StructuredMapCacheEntry;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.internal.CollectionElementBasicImpl;
import org.hibernate.metamodel.model.domain.internal.CollectionElementEmbeddedImpl;
import org.hibernate.metamodel.model.domain.internal.CollectionElementEntityImpl;
import org.hibernate.metamodel.model.domain.internal.CollectionIndexBasicImpl;
import org.hibernate.metamodel.model.domain.internal.CollectionIndexEmbeddedImpl;
import org.hibernate.metamodel.model.domain.internal.CollectionIndexEntityImpl;
import org.hibernate.metamodel.model.domain.internal.PersisterHelper;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.ElementColumnReferenceSource;
import org.hibernate.sql.ast.produce.metamodel.spi.IndexColumnReferenceSource;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableReferenceInfo;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfoSource;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.CollectionTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPersistentCollectionMetadata<O,C,E> implements PersistentCollectionMetadata<O,C,E> {
	private final SessionFactoryImplementor sessionFactory;
	private final ManagedTypeImplementor source;
	private final NavigableRole navigableRole;
	private final CollectionClassification collectionClassification;

	private final CollectionKey foreignKeyDescriptor;

	// todo (6.0) - rework these per https://hibernate.atlassian.net/browse/HHH-11356
	private final CollectionRegionAccessStrategy cacheAccessStrategy;

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



	public AbstractPersistentCollectionMetadata(
			Collection collectionBinding,
			ManagedTypeImplementor source,
			String navigableName,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) throws MappingException, CacheException {
		this.sessionFactory = creationContext.getSessionFactory();
		this.source = source;
		this.navigableRole = source.getNavigableRole().append( navigableName );
		this.collectionClassification = PersisterHelper.interpretCollectionClassification( collectionBinding );
		this.foreignKeyDescriptor = new CollectionKey( this, collectionBinding );

		this.cacheAccessStrategy = cacheAccessStrategy;

		if ( sessionFactory.getSessionFactoryOptions().isStructuredCacheEntriesEnabled() ) {
			cacheEntryStructure = collectionBinding.isMap()
					? StructuredMapCacheEntry.INSTANCE
					: StructuredCollectionCacheEntry.INSTANCE;
		}
		else {
			cacheEntryStructure = UnstructuredCacheEntry.INSTANCE;
		}
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
					creationContext.getSessionFactory().getIdentifierGeneratorFactory(),
					dialect,
					defaultCatalogName,
					defaultSchemaName,
					null
			);
			this.idDescriptor = new CollectionIdentifier(
					(BasicType) ( (IdentifierCollection) collectionBinding ).getIdentifier().getType(),
					identifierGenerator
			);
		}
		else {
			idDescriptor = null;
		}

		this.indexDescriptor = resolveIndexDescriptor( this, collectionBinding, separateCollectionTable, creationContext );
		this.elementDescriptor = resolveElementDescriptor( this, collectionBinding, separateCollectionTable, creationContext );
	}

	@SuppressWarnings("unchecked")
	private static <J,T extends Type<J>> CollectionIndex<J> resolveIndexDescriptor(
			PersistentCollectionMetadata persister,
			Collection collectionBinding,
			Table separateCollectionTable,
			RuntimeModelCreationContext creationContext) {
		if ( !IndexedCollection.class.isInstance( collectionBinding ) ) {
			return null;
		}

		final IndexedCollection indexedCollectionMapping = (IndexedCollection) collectionBinding;
		final Value indexValueMapping = indexedCollectionMapping.getIndex();
		final Type<J> indexType = indexValueMapping.getType();
		// todo (6.0) : collect index columns
		final List<Column> columns = collectIndexColumns( persister, indexedCollectionMapping, separateCollectionTable, creationContext );

		if ( indexValueMapping instanceof OneToMany
				|| indexValueMapping instanceof ManyToOne ) {
			// NOTE : ManyToOne is used to signify the index is a many-to-many
			return new CollectionIndexEntityImpl(
					persister,
					indexedCollectionMapping,
					creationContext
			);
		}
		else if ( indexValueMapping instanceof EmbeddedValueMapping ) {
			return new CollectionIndexEmbeddedImpl(
					persister,
					indexedCollectionMapping,
					creationContext
			);
		}
		else if ( indexValueMapping instanceof SimpleValue ) {
			return new CollectionIndexBasicImpl(
					persister,
					indexedCollectionMapping,
					creationContext
			);
		}
		else {
			// should indicate an ANY index
			throw new NotYetImplementedException(  );
		}
	}

	private static List<Column> collectIndexColumns(
			PersistentCollectionMetadata persister,
			IndexedCollection indexedCollectionBinding,
			Table separateCollectionTable, RuntimeModelCreationContext creationContext) {
		return PersisterHelper.makeValues(
				creationContext.getSessionFactory(),
				indexedCollectionBinding.getIndex().getType(),
				indexedCollectionBinding.getIndex().getColumnIterator(),
				// todo (6.0) : `separateCollectionTable` works for many-to-many and element-collections - need to account for one-to-many (no separateCollectionTable)
				separateCollectionTable
		);
	}

	protected abstract Table resolveCollectionTable(
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext);


	@SuppressWarnings("unchecked")
	private static CollectionElement resolveElementDescriptor(
			AbstractPersistentCollectionMetadata collectionPersister,
			Collection collectionBinding,
			Table separateCollectionTable,
			RuntimeModelCreationContext creationContext) {
		final Type elementType = collectionBinding.getElement().getType();

		if ( elementType.isAnyType() ) {
			assert separateCollectionTable != null;

			throw new NotYetImplementedException(  );

//			final java.util.List<Column> columns = PersisterHelper.makeValues(
//					sessionFactory,
//					elementType,
//					getElementColumnNames(),
//					null,
//					this.separateCollectionTable
//			);
//
//			return new CollectionElementAny(
//					this,
//					(AnyType) elementType,
//					columns
//			);
		}
		else if ( elementType.isComponentType() ) {
			assert separateCollectionTable != null;

			return new CollectionElementEmbeddedImpl(
					collectionPersister,
					collectionBinding,
					creationContext
			);
		}
		else if ( elementType.isEntityType() ) {
			return new CollectionElementEntityImpl(
					collectionPersister,
					collectionBinding,
					separateCollectionTable == null
							? CollectionElement.ElementClassification.MANY_TO_MANY
							: CollectionElement.ElementClassification.ONE_TO_MANY,
					creationContext
			);
		}
		else {
			assert separateCollectionTable != null;
			return new CollectionElementBasicImpl(
					collectionPersister,
					collectionBinding,
					creationContext
			);
		}
	}

	@Override
	public TableGroup createRootTableGroup(
			TableGroupInfoSource tableGroupInfo,
			RootTableGroupContext tableGroupContext) {
		throw new org.hibernate.sql.NotYetImplementedException(  );
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
			TableGroupInfoSource tableGroupInfoSource,
			JoinType joinType,
			NavigableReference joinedReference,
			JoinedTableGroupContext tableGroupJoinContext) {
		throw new org.hibernate.sql.NotYetImplementedException(  );
	}

}
