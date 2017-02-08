/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.spi;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
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
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.persister.collection.internal.CollectionElementBasicImpl;
import org.hibernate.persister.collection.internal.CollectionElementEmbeddedImpl;
import org.hibernate.persister.collection.internal.CollectionElementEntityImpl;
import org.hibernate.persister.collection.internal.CollectionIndexBasicImpl;
import org.hibernate.persister.collection.internal.CollectionIndexEmbeddedImpl;
import org.hibernate.persister.collection.internal.CollectionIndexEntityImpl;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sqm.domain.SqmPluralAttributeElement.ElementClassification;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.EmbeddedType;
import org.hibernate.type.spi.EntityType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionPersister<O,C,E> implements CollectionPersister<O,C,E> {
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


	private CollectionId idDescriptor;
	private CollectionElement elementDescriptor;
	private CollectionIndex indexDescriptor;

	private Table separateCollectionTable;



	public AbstractCollectionPersister(
			Collection collectionBinding,
			ManagedTypeImplementor source,
			String navigableName,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			PersisterCreationContext creationContext) throws MappingException, CacheException {
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
			PersisterCreationContext creationContext) {

		// todo (6.0) : this is technically not the `separateCollectionTable` as for one-to-many it returns the element entity's table.
		//		need to decide how we want to model tables for collections.
		separateCollectionTable = resolveCollectionTable( collectionBinding, creationContext );

		final Database database = creationContext.getMetadata().getDatabase();
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final Namespace defaultNamespace = creationContext.getMetadata().getDatabase().getDefaultNamespace();
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
			this.idDescriptor = new CollectionId(
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
	private static <J,T extends Type<J>> CollectionIndex<J,T> resolveIndexDescriptor(
			CollectionPersister persister,
			Collection collectionBinding,
			Table separateCollectionTable,
			PersisterCreationContext creationContext) {
		if ( !IndexedCollection.class.isInstance( collectionBinding ) ) {
			return null;
		}

		final IndexedCollection indexedCollectionBinding = (IndexedCollection) collectionBinding;
		final Value indexValueMapping = indexedCollectionBinding.getIndex();
		final Type<J> indexType = indexValueMapping.getType();
		// todo (6.0) : collect index columns
		final List<Column> columns = collectIndexColumns( persister, indexedCollectionBinding, separateCollectionTable, creationContext );

		if ( indexValueMapping instanceof SimpleValue ) {
			return new CollectionIndexBasicImpl(
					persister,
					indexedCollectionBinding,
					(BasicType<J>) indexType,
					columns
			);
		}
		else if ( indexValueMapping instanceof Component ) {
			return new CollectionIndexEmbeddedImpl(
					persister,
					indexedCollectionBinding,
					(EmbeddedType) indexType,
					columns
			);
		}
		else if ( indexValueMapping instanceof OneToMany
				|| indexValueMapping instanceof ManyToOne ) {
			// NOTE : ManyToOne is used to signify the index is a many-to-many
			return new CollectionIndexEntityImpl(
					persister,
					indexedCollectionBinding,
					(EntityType) indexType,
					columns
			);
		}
		else {
			// should indicate an ANY index
			throw new NotYetImplementedException(  );
		}
	}

	private static List<Column> collectIndexColumns(
			CollectionPersister persister,
			IndexedCollection indexedCollectionBinding,
			Table separateCollectionTable, PersisterCreationContext creationContext) {
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
			PersisterCreationContext creationContext);


	@SuppressWarnings("unchecked")
	private static CollectionElement resolveElementDescriptor(
			AbstractCollectionPersister collectionPersister,
			Collection collectionBinding,
			Table separateCollectionTable,
			PersisterCreationContext creationContext) {
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

			final java.util.List<Column> columns = PersisterHelper.makeValues(
					creationContext.getSessionFactory(),
					elementType,
					collectionBinding.getColumnIterator(),
					separateCollectionTable
			);

			return new CollectionElementEmbeddedImpl(
					collectionPersister,
					collectionBinding,
					(EmbeddedType) elementType,
					columns
			);
		}
		else if ( elementType.isEntityType() ) {
			final EntityPersister elementPersister = ( (EntityType) elementType ).getEntityPersister();
			final Table table;
			if ( separateCollectionTable != null ) {
				// assume it is a many-to-many
				// todo (6.0) : should this allow multiple Tables to cater to collections?
				table = separateCollectionTable;
			}
			else {
				table = elementPersister.getRootTable();
			}

			final java.util.List<Column> columns = PersisterHelper.makeValues(
					creationContext.getSessionFactory(),
					elementType,
					collectionBinding.getElement().getColumnIterator(),
					table
			);

			return new CollectionElementEntityImpl(
					collectionPersister,
					collectionBinding,
					(EntityType) elementType,
					separateCollectionTable == null
							? ElementClassification.MANY_TO_MANY
							: ElementClassification.ONE_TO_MANY,
					columns
			);
		}
		else {
			assert separateCollectionTable != null;

			final java.util.List<Column> columns = PersisterHelper.makeValues(
					creationContext.getSessionFactory(),
					elementType,
					collectionBinding.getElement().getColumnIterator(),
					separateCollectionTable
			);

			return new CollectionElementBasicImpl(
					collectionPersister,
					collectionBinding,
					(BasicType) elementType,
					columns
			);
		}
	}

}
