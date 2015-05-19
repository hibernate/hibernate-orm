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
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metadata.CollectionMetadata;
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
 *         {@link CollectionRegionAccessStrategy} - the second level caching strategy for this collection
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
	 */
	public void initialize(Serializable key, SessionImplementor session) //TODO: add owner argument!!
	throws HibernateException;
	/**
	 * Is this collection role cacheable
	 */
	public boolean hasCache();
	/**
	 * Get the cache
	 */
	public CollectionRegionAccessStrategy getCacheAccessStrategy();
	/**
	 * Get the cache structure
	 */
	public CacheEntryStructure getCacheEntryStructure();
	/**
	 * Get the associated <tt>Type</tt>
	 */
	public CollectionType getCollectionType();
	/**
	 * Get the "key" type (the type of the foreign key)
	 */
	public Type getKeyType();
	/**
	 * Get the "index" type for a list or map (optional operation)
	 */
	public Type getIndexType();
	/**
	 * Get the "element" type
	 */
	public Type getElementType();
	/**
	 * Return the element class of an array, or null otherwise
	 */
	public Class getElementClass();
	/**
	 * Read the key from a row of the JDBC <tt>ResultSet</tt>
	 */
	public Object readKey(ResultSet rs, String[] keyAliases, SessionImplementor session)
		throws HibernateException, SQLException;
	/**
	 * Read the element from a row of the JDBC <tt>ResultSet</tt>
	 */
	public Object readElement(
		ResultSet rs,
		Object owner,
		String[] columnAliases,
		SessionImplementor session)
		throws HibernateException, SQLException;
	/**
	 * Read the index from a row of the JDBC <tt>ResultSet</tt>
	 */
	public Object readIndex(ResultSet rs, String[] columnAliases, SessionImplementor session)
		throws HibernateException, SQLException;
	/**
	 * Read the identifier from a row of the JDBC <tt>ResultSet</tt>
	 */
	public Object readIdentifier(
		ResultSet rs,
		String columnAlias,
		SessionImplementor session)
		throws HibernateException, SQLException;
	/**
	 * Is this an array or primitive values?
	 */
	public boolean isPrimitiveArray();
	/**
	 * Is this an array?
	 */
	public boolean isArray();
	/**
	 * Is this a one-to-many association?
	 */
	public boolean isOneToMany();
	/**
	 * Is this a many-to-many association?  Note that this is mainly
	 * a convenience feature as the single persister does not
	 * conatin all the information needed to handle a many-to-many
	 * itself, as internally it is looked at as two many-to-ones.
	 */
	public boolean isManyToMany();

	public String getManyToManyFilterFragment(String alias, Map enabledFilters);

	/**
	 * Is this an "indexed" collection? (list or map)
	 */
	public boolean hasIndex();
	/**
	 * Is this collection lazyily initialized?
	 */
	public boolean isLazy();
	/**
	 * Is this collection "inverse", so state changes are not
	 * propogated to the database.
	 */
	public boolean isInverse();
	/**
	 * Completely remove the persistent state of the collection
	 */
	public void remove(Serializable id, SessionImplementor session)
		throws HibernateException;
	/**
	 * (Re)create the collection's persistent state
	 */
	public void recreate(
		PersistentCollection collection,
		Serializable key,
		SessionImplementor session)
		throws HibernateException;
	/**
	 * Delete the persistent state of any elements that were removed from
	 * the collection
	 */
	public void deleteRows(
		PersistentCollection collection,
		Serializable key,
		SessionImplementor session)
		throws HibernateException;
	/**
	 * Update the persistent state of any elements that were modified
	 */
	public void updateRows(
		PersistentCollection collection,
		Serializable key,
		SessionImplementor session)
		throws HibernateException;
	/**
	 * Insert the persistent state of any new collection elements
	 */
	public void insertRows(
		PersistentCollection collection,
		Serializable key,
		SessionImplementor session)
		throws HibernateException;
	
	/**
	 * Process queued operations within the PersistentCollection.
	 */
	public void processQueuedOps(
			PersistentCollection collection,
			Serializable key,
			SessionImplementor session)
			throws HibernateException;
	
	/**
	 * Get the name of this collection role (the fully qualified class name,
	 * extended by a "property path")
	 */
	public String getRole();
	/**
	 * Get the persister of the entity that "owns" this collection
	 */
	public EntityPersister getOwnerEntityPersister();
	/**
	 * Get the surrogate key generation strategy (optional operation)
	 */
	public IdentifierGenerator getIdentifierGenerator();
	/**
	 * Get the type of the surrogate key
	 */
	public Type getIdentifierType();
	/**
	 * Does this collection implement "orphan delete"?
	 */
	public boolean hasOrphanDelete();
	/**
	 * Is this an ordered collection? (An ordered collection is
	 * ordered by the initialization operation, not by sorting
	 * that happens in memory, as in the case of a sorted collection.)
	 */
	public boolean hasOrdering();

	public boolean hasManyToManyOrdering();

	/**
	 * Get the "space" that holds the persistent state
	 */
	public Serializable[] getCollectionSpaces();

	public CollectionMetadata getCollectionMetadata();

	/**
	 * Is cascade delete handled by the database-level
	 * foreign key constraint definition?
	 */
	public abstract boolean isCascadeDeleteEnabled();
	
	/**
	 * Does this collection cause version increment of the 
	 * owning entity?
	 */
	public boolean isVersioned();
	
	/**
	 * Can the elements of this collection change?
	 */
	public boolean isMutable();
	
	//public boolean isSubselectLoadable();
	
	public String getNodeName();
	
	public String getElementNodeName();
	
	public String getIndexNodeName();

	public void postInstantiate() throws MappingException;
	
	public SessionFactoryImplementor getFactory();

	public boolean isAffectedByEnabledFilters(SessionImplementor session);

	/**
	 * Generates the collection's key column aliases, based on the given
	 * suffix.
	 *
	 * @param suffix The suffix to use in the key column alias generation.
	 * @return The key column aliases.
	 */
	public String[] getKeyColumnAliases(String suffix);

	/**
	 * Generates the collection's index column aliases, based on the given
	 * suffix.
	 *
	 * @param suffix The suffix to use in the index column alias generation.
	 * @return The key column aliases, or null if not indexed.
	 */
	public String[] getIndexColumnAliases(String suffix);

	/**
	 * Generates the collection's element column aliases, based on the given
	 * suffix.
	 *
	 * @param suffix The suffix to use in the element column alias generation.
	 * @return The key column aliases.
	 */
	public String[] getElementColumnAliases(String suffix);

	/**
	 * Generates the collection's identifier column aliases, based on the given
	 * suffix.
	 *
	 * @param suffix The suffix to use in the key column alias generation.
	 * @return The key column aliases.
	 */
	public String getIdentifierColumnAlias(String suffix);
	
	public boolean isExtraLazy();
	public int getSize(Serializable key, SessionImplementor session);
	public boolean indexExists(Serializable key, Object index, SessionImplementor session);
	public boolean elementExists(Serializable key, Object element, SessionImplementor session);
	public Object getElementByIndex(Serializable key, Object index, SessionImplementor session, Object owner);
	public int getBatchSize();

	/**
	 * @return the name of the property this collection is mapped by
	 */
	public String getMappedByProperty();
}
