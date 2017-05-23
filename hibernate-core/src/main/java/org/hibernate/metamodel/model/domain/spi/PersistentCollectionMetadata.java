/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.io.Serializable;
import javax.persistence.metamodel.PluralAttribute;

import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.collection.spi.PersistentCollectionTuplizer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.CollectionLoader;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelNodeFactory;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.produce.spi.RootTableGroupProducer;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.produce.spi.TableReferenceContributor;

/**
 * A strategy for persisting a collection role. Defines a contract between
 * the persistence strategy and the actual persistent collection framework
 * and session. Does not define operations that are required for querying
 * collections, or loading by outer join.<br>
 * <br>
 * Implements persistence of a collection instance while the instance is
 * referenced in a particular role.<br>
 * <br>
 * This class is highly coupled to the <tt>PersistentCollection</tt>
 * hierarchy, since double dispatch is used to load and update collection
 * elements.<br>
 * <br>
 * May be considered an immutable view of the mapping object
 * <p/>
 * Unless a customer {@link RuntimeModelNodeFactory} is used, it is expected
 * that implementations of CollectionDefinition define a constructor accepting the following arguments:<ol>
 *     <li>
 *         {@link Collection} - The metadata about the collection to be handled
 *         by the persister
 *     </li>
 *     <li>
 *         {@link ManagedTypeImplementor} - Describes the thing the declares the collection
 *     </li>
 *     <li>
 *         String - The name of the collection's attribute relative to AttributeContainer
 *     </li>
 *     <li>
 *         {@link CollectionRegionAccessStrategy} - the second level caching strategy for this collection
 *     </li>
 *     <li>
 *         {@link RuntimeModelCreationContext} - access to additional
 *         information useful while constructing the persister.
 *     </li>
 * </ol>
 *
 * @see org.hibernate.collection.spi.PersistentCollection
 * @author Gavin King
 */
public interface PersistentCollectionMetadata<O,C,E>
		extends NavigableContainer<C>, RootTableGroupProducer, TableGroupJoinProducer,
		TableReferenceContributor, EmbeddedContainer<C> {

	Class[] CONSTRUCTOR_SIGNATURE = new Class[] {
			Collection.class,
			ManagedTypeImplementor.class,
			String.class,
			CollectionRegionAccessStrategy.class,
			RuntimeModelCreationContext.class
	};

	// todo : in terms of SqmNavigableSource.findNavigable() impl, be sure to only recognize:
	//			1) key
	//			2) index
	//			3) element
	//			4) value
	//			5) elements
	//			6) indices

	void finishInitialization(Collection collectionBinding, RuntimeModelCreationContext creationContext);


	/**
	 * Classifications of the plurality.  See {@link CollectionElement.ElementClassification}
	 * and {@link CollectionIndex.IndexClassification} for classification of the element and
	 * index/key, respectively.
	 */
	enum CollectionClassification {
		SET( PluralAttribute.CollectionType.SET ),
		LIST( PluralAttribute.CollectionType.LIST ),
		MAP( PluralAttribute.CollectionType.MAP ),
		BAG( PluralAttribute.CollectionType.COLLECTION );

		private final PluralAttribute.CollectionType jpaClassification;

		CollectionClassification(PluralAttribute.CollectionType jpaClassification) {
			this.jpaClassification = jpaClassification;
		}

		public PluralAttribute.CollectionType toJpaClassification() {
			return jpaClassification;
		}
	}

	CollectionClassification getCollectionClassification();

	NavigableRole getNavigableRole();

	/**
	 * @deprecated Use {@link #getNavigableRole()} instead.
	 */
	@Deprecated
	default String getRoleName() {
		return getNavigableRole().getFullPath();
	}

	/**
	 * Access information about the FK mapping to the "owner" of this collection
	 */
	CollectionKey getForeignKeyDescriptor();

	/**
	 * Access to the collection identifier, if it has one (idbag).  If not, will
	 * return {@code null}.
	 */
	CollectionIdentifier getIdDescriptor();

	/**
	 * Access to information about the collection's elements
	 */
	CollectionElement<E> getElementDescriptor();

	/**
	 * Access to information about the collection's index (list/array) or key (map).
	 * Will return {@code null} if the collection is not indexed (is not a map, list
	 * or array).
	 */
	CollectionIndex getIndexDescriptor();

	PersistentCollectionTuplizer getTuplizer();

	/**
	 * @todo (6.0) what args?
	 */
	CollectionLoader getLoader();

	Table getSeparateCollectionTable();

	boolean isInverse();

	boolean hasOrphanDelete();

	boolean isOneToMany();

	boolean isExtraLazy();

	boolean isDirty(Object old, Object value, SharedSessionContractImplementor session);


	// todo (6.0) : consider better alternatives
	// 		- delegates?  i.e.
	// 				* PersistentCollectionMetadata#getSizeExecutor()...
	//				* PersistentCollectionMetadata#getIndexDescriptor#getIndexExistsExecutor()...
	//				* PersistentCollectionMetadata#getElementDescriptor#getElementExistsExecutor()...
	//				* etc

	int getSize(Serializable loadedKey, SharedSessionContractImplementor session);

	Boolean indexExists(Serializable loadedKey, Object index, SharedSessionContractImplementor session);

	Boolean elementExists(Serializable loadedKey, Object element, SharedSessionContractImplementor session);

	Object getElementByIndex(Serializable loadedKey, Object index, SharedSessionContractImplementor session, Object owner);









	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// legacy stuff

	/**
	 * Is this collection role cacheable
	 */
	default boolean hasCache() {
		return getCacheAccessStrategy() != null;
	}

	/**
	 * Get the cache
	 */
	CollectionRegionAccessStrategy getCacheAccessStrategy();

	/**
	 * Get the cache structure
	 */
	CacheEntryStructure getCacheEntryStructure();




	// consider whether we want to keep any of this legacy stuff

//	/**
//	 * Initialize the given collection with the given key
//	 * TODO: add owner argument!!
//	 */
//	void initialize(Serializable key, SharedSessionContractImplementor session) throws HibernateException;
//	/**
//	 * Get the associated <tt>Type</tt>
//	 */
//	CollectionType getCollectionType();
//	/**
//	 * Get the "key" type (the type of the foreign key)
//	 */
//	Type getKeyType();
//	/**
//	 * Get the "index" type for a list or map (optional operation)
//	 */
//	Type getIndexType();
//	/**
//	 * Get the "element" type
//	 */
//	ExpressableType<E> getElementType();
//	/**
//	 * Return the element class of an array, or null otherwise
//	 */
//	Class getElementClass();
//	/**
//	 * Read the key from a row of the JDBC <tt>ResultSet</tt>
//	 */
//	Object readKey(ResultSet rs, String[] keyAliases, SharedSessionContractImplementor session)
//		throws HibernateException, SQLException;
//	/**
//	 * Read the element from a row of the JDBC <tt>ResultSet</tt>
//	 */
//	Object readElement(
//			ResultSet rs,
//			Object owner,
//			String[] columnAliases,
//			SharedSessionContractImplementor session)
//		throws HibernateException, SQLException;
//	/**
//	 * Read the index from a row of the JDBC <tt>ResultSet</tt>
//	 */
//	Object readIndex(ResultSet rs, String[] columnAliases, SharedSessionContractImplementor session)
//		throws HibernateException, SQLException;
//	/**
//	 * Read the identifier from a row of the JDBC <tt>ResultSet</tt>
//	 */
//	Object readIdentifier(
//			ResultSet rs,
//			String columnAlias,
//			SharedSessionContractImplementor session)
//		throws HibernateException, SQLException;
//	/**
//	 * Is this an array or primitive values?
//	 */
//	boolean isPrimitiveArray();
//
//	/**
//	 * Is this an array?
//	 */
//	boolean isArray();
//
//	/**
//	 * Is this a one-to-many association?
//	 */
//	boolean isOneToMany();
//	/**
//	 * Is this a many-to-many association?  Note that this is mainly
//	 * a convenience feature as the single persister does not
//	 * conatin all the information needed to handle a many-to-many
//	 * itself, as internally it is looked at as two many-to-ones.
//	 */
//	boolean isManyToMany();
//
//	String getManyToManyFilterFragment(String alias, Map enabledFilters);
//
//	/**
//	 * Is this an "indexed" collection? (list or map)
//	 */
//	boolean hasIndex();
//	/**
//	 * Is this collection lazyily initialized?
//	 */
//	boolean isLazy();
//	/**
//	 * Is this collection "inverse", so state changes are not
//	 * propogated to the database.
//	 */
//	boolean isInverse();
//	/**
//	 * Completely remove the persistent state of the collection
//	 */
//	void remove(Serializable id, SharedSessionContractImplementor session)
//		throws HibernateException;
//	/**
//	 * (Re)create the collection's persistent state
//	 */
//	void recreate(
//			PersistentCollection collection,
//			Serializable key,
//			SharedSessionContractImplementor session)
//		throws HibernateException;
//	/**
//	 * Delete the persistent state of any elements that were removed from
//	 * the collection
//	 */
//	void deleteRows(
//			PersistentCollection collection,
//			Serializable key,
//			SharedSessionContractImplementor session)
//		throws HibernateException;
//	/**
//	 * Update the persistent state of any elements that were modified
//	 */
//	void updateRows(
//			PersistentCollection collection,
//			Serializable key,
//			SharedSessionContractImplementor session)
//		throws HibernateException;
//	/**
//	 * Insert the persistent state of any new collection elements
//	 */
//	void insertRows(
//			PersistentCollection collection,
//			Serializable key,
//			SharedSessionContractImplementor session)
//		throws HibernateException;
//
//	/**
//	 * Process queued operations within the PersistentCollection.
//	 */
//	void processQueuedOps(
//			PersistentCollection collection,
//			Serializable key,
//			SharedSessionContractImplementor session)
//			throws HibernateException;
//
//	/**
//	 * Get the name of this collection role (the fully qualified class name,
//	 * extended by a "property path")
//	 */
//	String getRole();
//	/**
//	 * Get the persister of the entity that "owns" this collection
//	 */
//	EntityTypeImplementor getOwnerEntityPersister();
//	/**
//	 * Get the surrogate key generation strategy (optional operation)
//	 */
//	IdentifierGenerator getIdentifierGenerator();
//	/**
//	 * Get the type of the surrogate key
//	 */
//	Type getIdentifierType();
//	/**
//	 * Does this collection implement "orphan delete"?
//	 */
//	boolean hasOrphanDelete();
//	/**
//	 * Is this an ordered collection? (An ordered collection is
//	 * ordered by the initialization operation, not by sorting
//	 * that happens in memory, as in the case of a sorted collection.)
//	 */
//	boolean hasOrdering();
//
//	boolean hasManyToManyOrdering();
//
//	/**
//	 * Get the "space" that holds the persistent state
//	 */
//	Serializable[] getCollectionSpaces();
//
//	CollectionMetadata getCollectionMetadata();
//
//	/**
//	 * Is cascade delete handled by the database-level
//	 * foreign key constraint definition?
//	 */
//	boolean isCascadeDeleteEnabled();
//
//	/**
//	 * Does this collection cause version increment of the
//	 * owning entity?
//	 */
//	boolean isVersioned();
//
//	/**
//	 * Can the elements of this collection change?
//	 */
//	boolean isMutable();
//
//	//public boolean isSubselectLoadable();
//
//	void postInstantiate() throws MappingException;
//
//	SessionFactoryImplementor getFactory();
//
//	boolean isAffectedByEnabledFilters(SharedSessionContractImplementor session);
//
//	/**
//	 * Generates the collection's key column aliases, based on the given
//	 * suffix.
//	 *
//	 * @param suffix The suffix to use in the key column alias generation.
//	 * @return The key column aliases.
//	 */
//	String[] getKeyColumnAliases(String suffix);
//
//	/**
//	 * Generates the collection's index column aliases, based on the given
//	 * suffix.
//	 *
//	 * @param suffix The suffix to use in the index column alias generation.
//	 * @return The key column aliases, or null if not indexed.
//	 */
//	String[] getIndexColumnAliases(String suffix);
//
//	/**
//	 * Generates the collection's element column aliases, based on the given
//	 * suffix.
//	 *
//	 * @param suffix The suffix to use in the element column alias generation.
//	 * @return The key column aliases.
//	 */
//	String[] getElementColumnAliases(String suffix);
//
//	/**
//	 * Generates the collection's identifier column aliases, based on the given
//	 * suffix.
//	 *
//	 * @param suffix The suffix to use in the key column alias generation.
//	 * @return The key column aliases.
//	 */
//	String getIdentifierColumnAlias(String suffix);
//
//	boolean isExtraLazy();
//	int getSize(Serializable key, SharedSessionContractImplementor session);
//	boolean indexExists(Serializable key, Object index, SharedSessionContractImplementor session);
//	boolean elementExists(Serializable key, Object element, SharedSessionContractImplementor session);
//	Object getElementByIndex(Serializable key, Object index, SharedSessionContractImplementor session, Object owner);
//	int getBatchSize();
//
//	/**
//	 * @return the name of the property this collection is mapped by
//	 */
//	String getMappedByProperty();

}
