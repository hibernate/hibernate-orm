/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

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
 * Unless a customer {@link org.hibernate.persister.spi.PersisterFactory} is used, it is expected
 * that implementations of CollectionDefinition define a constructor accepting the following arguments:<ol>
 *     <li>
 *         {@link org.hibernate.mapping.Collection} - The metadata about the collection to be handled
 *         by the persister
 *     </li>
 *     <li>
 *         {@link CollectionDataAccess} - the second level caching strategy for this collection
 *     </li>
 *     <li>
 *         {@link org.hibernate.persister.spi.PersisterCreationContext} - access to additional
 *         information useful while constructing the persister.
 *     </li>
 * </ol>
 *
 * @see QueryableCollection
 * @see org.hibernate.collection.spi.PersistentCollection
 * @author Gavin King
 */
public interface CollectionPersister extends CollectionDefinition {
	/**
	 * Initialize the given collection with the given key
	 * TODO: add owner argument!!
	 */
	void initialize(Serializable key, SharedSessionContractImplementor session) throws HibernateException;
	/**
	 * Is this collection role cacheable
	 */
	boolean hasCache();
	/**
	 * Get the cache
	 */
	CollectionDataAccess getCacheAccessStrategy();

	NavigableRole getNavigableRole();

	/**
	 * Get the cache structure
	 */
	CacheEntryStructure getCacheEntryStructure();
	/**
	 * Get the associated <tt>Type</tt>
	 */
	CollectionType getCollectionType();
	/**
	 * Get the "key" type (the type of the foreign key)
	 */
	Type getKeyType();
	/**
	 * Get the "index" type for a list or map (optional operation)
	 */
	Type getIndexType();
	/**
	 * Get the "element" type
	 */
	Type getElementType();
	/**
	 * Return the element class of an array, or null otherwise
	 */
	Class getElementClass();
	/**
	 * Read the key from a row of the JDBC <tt>ResultSet</tt>
	 */
	Object readKey(ResultSet rs, String[] keyAliases, SharedSessionContractImplementor session)
		throws HibernateException, SQLException;
	/**
	 * Read the element from a row of the JDBC <tt>ResultSet</tt>
	 */
	Object readElement(
			ResultSet rs,
			Object owner,
			String[] columnAliases,
			SharedSessionContractImplementor session)
		throws HibernateException, SQLException;
	/**
	 * Read the index from a row of the JDBC <tt>ResultSet</tt>
	 */
	Object readIndex(ResultSet rs, String[] columnAliases, SharedSessionContractImplementor session)
		throws HibernateException, SQLException;
	/**
	 * Read the identifier from a row of the JDBC <tt>ResultSet</tt>
	 */
	Object readIdentifier(
			ResultSet rs,
			String columnAlias,
			SharedSessionContractImplementor session)
		throws HibernateException, SQLException;
	/**
	 * Is this an array or primitive values?
	 */
	boolean isPrimitiveArray();
	/**
	 * Is this an array?
	 */
	boolean isArray();
	/**
	 * Is this a one-to-many association?
	 */
	boolean isOneToMany();
	/**
	 * Is this a many-to-many association?  Note that this is mainly
	 * a convenience feature as the single persister does not
	 * contain all the information needed to handle a many-to-many
	 * itself, as internally it is looked at as two many-to-ones.
	 */
	boolean isManyToMany();

	String getManyToManyFilterFragment(String alias, Map enabledFilters);

	/**
	 * Is this an "indexed" collection? (list or map)
	 */
	boolean hasIndex();
	/**
	 * Is this collection lazyily initialized?
	 */
	boolean isLazy();
	/**
	 * Is this collection "inverse", so state changes are not
	 * propagated to the database.
	 */
	boolean isInverse();
	/**
	 * Completely remove the persistent state of the collection
	 */
	void remove(Serializable id, SharedSessionContractImplementor session)
		throws HibernateException;
	/**
	 * (Re)create the collection's persistent state
	 */
	void recreate(
			PersistentCollection collection,
			Serializable key,
			SharedSessionContractImplementor session)
		throws HibernateException;
	/**
	 * Delete the persistent state of any elements that were removed from
	 * the collection
	 */
	void deleteRows(
			PersistentCollection collection,
			Serializable key,
			SharedSessionContractImplementor session)
		throws HibernateException;
	/**
	 * Update the persistent state of any elements that were modified
	 */
	void updateRows(
			PersistentCollection collection,
			Serializable key,
			SharedSessionContractImplementor session)
		throws HibernateException;
	/**
	 * Insert the persistent state of any new collection elements
	 */
	void insertRows(
			PersistentCollection collection,
			Serializable key,
			SharedSessionContractImplementor session)
		throws HibernateException;

	/**
	 * Process queued operations within the PersistentCollection.
	 */
	void processQueuedOps(
			PersistentCollection collection,
			Serializable key,
			SharedSessionContractImplementor session)
			throws HibernateException;

	/**
	 * Get the name of this collection role (the fully qualified class name,
	 * extended by a "property path")
	 */
	String getRole();
	/**
	 * Get the persister of the entity that "owns" this collection
	 */
	EntityPersister getOwnerEntityPersister();
	/**
	 * Get the surrogate key generation strategy (optional operation)
	 */
	IdentifierGenerator getIdentifierGenerator();
	/**
	 * Get the type of the surrogate key
	 */
	Type getIdentifierType();
	/**
	 * Does this collection implement "orphan delete"?
	 */
	boolean hasOrphanDelete();
	/**
	 * Is this an ordered collection? (An ordered collection is
	 * ordered by the initialization operation, not by sorting
	 * that happens in memory, as in the case of a sorted collection.)
	 */
	boolean hasOrdering();

	boolean hasManyToManyOrdering();

	/**
	 * Get the "space" that holds the persistent state
	 */
	Serializable[] getCollectionSpaces();

	CollectionMetadata getCollectionMetadata();

	/**
	 * Is cascade delete handled by the database-level
	 * foreign key constraint definition?
	 */
	boolean isCascadeDeleteEnabled();

	/**
	 * Does this collection cause version increment of the
	 * owning entity?
	 */
	boolean isVersioned();

	/**
	 * Can the elements of this collection change?
	 */
	boolean isMutable();

	//public boolean isSubselectLoadable();

	void postInstantiate() throws MappingException;

	SessionFactoryImplementor getFactory();

	boolean isAffectedByEnabledFilters(SharedSessionContractImplementor session);

	/**
	 * Generates the collection's key column aliases, based on the given
	 * suffix.
	 *
	 * @param suffix The suffix to use in the key column alias generation.
	 * @return The key column aliases.
	 */
	String[] getKeyColumnAliases(String suffix);

	/**
	 * Generates the collection's index column aliases, based on the given
	 * suffix.
	 *
	 * @param suffix The suffix to use in the index column alias generation.
	 * @return The key column aliases, or null if not indexed.
	 */
	String[] getIndexColumnAliases(String suffix);

	/**
	 * Generates the collection's element column aliases, based on the given
	 * suffix.
	 *
	 * @param suffix The suffix to use in the element column alias generation.
	 * @return The key column aliases.
	 */
	String[] getElementColumnAliases(String suffix);

	/**
	 * Generates the collection's identifier column aliases, based on the given
	 * suffix.
	 *
	 * @param suffix The suffix to use in the key column alias generation.
	 * @return The key column aliases.
	 */
	String getIdentifierColumnAlias(String suffix);

	boolean isExtraLazy();
	int getSize(Serializable key, SharedSessionContractImplementor session);
	boolean indexExists(Serializable key, Object index, SharedSessionContractImplementor session);
	boolean elementExists(Serializable key, Object element, SharedSessionContractImplementor session);
	Object getElementByIndex(Serializable key, Object index, SharedSessionContractImplementor session, Object owner);
	int getBatchSize();

	/**
	 * @return the name of the property this collection is mapped by
	 */
	String getMappedByProperty();
}
