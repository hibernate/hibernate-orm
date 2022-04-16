/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.Restrictable;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * A strategy for persisting a mapped collection role. A
 * {@code CollectionPersister} orchestrates rendering of SQL statements
 * corresponding to basic lifecycle events, including {@code insert},
 * {@code update}, and {@code delete} statements, and their execution via
 * JDBC.
 * <p>
 * Concrete implementations of this interface handle
 * {@linkplain OneToManyPersister one-to-many},
 * {@linkplain BasicCollectionPersister many-to-many}, association
 * cardinalities, and to a certain extent abstract the details of those
 * mappings from collaborators.
 * <p>
 * Note that an instance of {@link PersistentCollection} may change roles.
 * This object implements persistence of a collection instance while the
 * instance is referenced in a particular role.
 * <p>
 * This interface defines a contract between the persistence strategy and
 * the actual {@linkplain PersistentCollection persistent collection framework}
 * and {@link org.hibernate.engine.spi.SessionImplementor session}. It does
 * not define operations that are required for querying collections, nor
 * for loading by outer join. Implementations are highly coupled to the
 * {@link PersistentCollection} hierarchy, since double dispatch is used to
 * load and update collection elements.
 * <p>
 * Unless a custom {@link org.hibernate.persister.spi.PersisterFactory} is
 * used, it is expected that implementations of {@link CollectionDefinition}
 * define a constructor accepting the following arguments:
 * <ol>
 *     <li>
 *         {@link org.hibernate.mapping.Collection} - The metadata about
 *         the collection to be handled by the persister,
 *     </li>
 *     <li>
 *         {@link CollectionDataAccess} - the second level caching strategy
 *         for this collection, and
 *     </li>
 *     <li>
 *         {@link org.hibernate.persister.spi.PersisterCreationContext} -
 *         access to additional information useful while constructing the
 *         persister.
 *     </li>
 * </ol>
 *
 * @see QueryableCollection
 * @see PersistentCollection
 *
 * @author Gavin King
 */
public interface CollectionPersister extends Restrictable {
	NavigableRole getNavigableRole();

	/**
	 * Initialize the given collection with the given key
	 */
	void initialize(Object key, SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Is this collection role cacheable
	 */
	boolean hasCache();

	/**
	 * Get the cache
	 */
	CollectionDataAccess getCacheAccessStrategy();

	/**
	 * Get the cache structure
	 */
	CacheEntryStructure getCacheEntryStructure();
	/**
	 * Get the associated {@code Type}
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
	Class<?> getElementClass();

	/**
	 * The value converter for the element values of this collection
	 */
	default BasicValueConverter getElementConverter() {
		return null;
	}

	/**
	 * The value converter for index values of this collection (effectively map keys only)
	 */
	default BasicValueConverter getIndexConverter() {
		return null;
	}

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

	String getManyToManyFilterFragment(TableGroup tableGroup, Map<String, Filter> enabledFilters);

	/**
	 * Is this an "indexed" collection? (list or map)
	 */
	boolean hasIndex();
	/**
	 * Is this collection lazily initialized?
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
	void remove(Object id, SharedSessionContractImplementor session);

	/**
	 * (Re)create the collection's persistent state
	 */
	void recreate(
			PersistentCollection<?> collection,
			Object key,
			SharedSessionContractImplementor session);

	/**
	 * Delete the persistent state of any elements that were removed from
	 * the collection
	 */
	void deleteRows(
			PersistentCollection<?> collection,
			Object key,
			SharedSessionContractImplementor session);

	/**
	 * Update the persistent state of any elements that were modified
	 */
	void updateRows(
			PersistentCollection<?> collection,
			Object key,
			SharedSessionContractImplementor session);

	/**
	 * Insert the persistent state of any new collection elements
	 */
	void insertRows(
			PersistentCollection<?> collection,
			Object key,
			SharedSessionContractImplementor session);
	
	/**
	 * Process queued operations within the PersistentCollection.
	 */
	void processQueuedOps(
			PersistentCollection<?> collection,
			Object key,
			SharedSessionContractImplementor session);
	
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

	/**
	 * Get the user-visible metadata for the collection (optional operation)
	 *
	 * @deprecated This operation is no longer called by Hibernate.
	 */
	@Deprecated(since = "6.0")
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

	default PluralAttributeMapping getAttributeMapping() {
		throw new UnsupportedOperationException( "CollectionPersister used for [" + getRole() + "] does not support SQL AST" );
	}

	default boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers) {
		throw new UnsupportedOperationException( "CollectionPersister used for [" + getRole() + "] does not support SQL AST" );
	}

	default boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers) {
		throw new UnsupportedOperationException( "CollectionPersister used for [" + getRole() + "] does not support SQL AST" );
	}

	default boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers) {
		throw new UnsupportedOperationException( "CollectionPersister used for [" + getRole() + "] does not support SQL AST" );
	}

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
	int getSize(Object key, SharedSessionContractImplementor session);
	boolean indexExists(Object key, Object index, SharedSessionContractImplementor session);
	boolean elementExists(Object key, Object element, SharedSessionContractImplementor session);
	Object getElementByIndex(Object key, Object index, SharedSessionContractImplementor session, Object owner);
	int getBatchSize();

	/**
	 * @return the name of the property this collection is mapped by
	 */
	String getMappedByProperty();

	/**
	 * For sorted collections, the comparator to use.  Non-parameterized
	 * because for SORTED_SET the elements are compared but for SORTED_MAP the
	 * keys are compared
	 *
	 * @see CollectionClassification#SORTED_MAP
	 * @see CollectionClassification#SORTED_SET
	 */
	Comparator<?> getSortingComparator();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// mapping model

	CollectionSemantics<?,?> getCollectionSemantics();


	void applyBaseManyToManyRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			Set<String> treatAsDeclarations,
			SqlAstCreationState creationState);
}
