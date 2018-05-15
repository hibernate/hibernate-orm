/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.loader.spi.CollectionLoader;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelDescriptorFactory;
import org.hibernate.metamodel.model.domain.CollectionDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.RootTableGroupProducer;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.produce.spi.TableReferenceContributor;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Metadata and operations on a persistent collection (plural attribute).
 *
 * Works hand-in-hand with both {@link PersistentCollection} and
 * {@link CollectionSemantics} to define complete support
 * for persistent collections.
 *
 * Unless a customer {@link RuntimeModelDescriptorFactory} is used, it is expected
 * that implementations of CollectionDefinition define a constructor accepting the following arguments:<ol>
 *     <li>
 *         {@link Collection} - The metadata about the collection to be handled
 *         by the persister
 *     </li>
 *     <li>
 *         {@link ManagedTypeDescriptor} - Describes the thing the declares the collection
 *     </li>
 *     <li>
 *         String - The name of the collection's attribute relative to AttributeContainer
 *     <li>
 *         {@link RuntimeModelCreationContext} - access to additional
 *         information useful while constructing the persister.
 *     </li>
 * </ol>
 *
 * @see org.hibernate.collection.spi.PersistentCollection
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface PersistentCollectionDescriptor<O,C,E>
		extends CollectionDomainType<C,E>, CollectionValuedNavigable<C>, RootTableGroupProducer, TableGroupJoinProducer,
		TableReferenceContributor, EmbeddedContainer<C>, Filterable, Fetchable<C> {

	Object UNFETCHED_COLLECTION = new MarkerObject( "UNFETCHED COLLECTION" );

	Class[] CONSTRUCTOR_SIGNATURE = new Class[] {
			Property.class,
			ManagedTypeDescriptor.class,
			RuntimeModelCreationContext.class
	};

	boolean finishInitialization(Collection collectionBinding, RuntimeModelCreationContext creationContext);

	default CollectionClassification getCollectionClassification() {
		return getSemantics().getCollectionClassification();
	}

	CollectionSemantics<C> getSemantics();

	@Override
	CollectionJavaDescriptor<C> getJavaTypeDescriptor();

	@Override
	ManagedTypeDescriptor<O> getContainer();

	NavigableRole getNavigableRole();

	PluralPersistentAttribute getDescribedAttribute() ;

	CollectionMutabilityPlan<C> getMutabilityPlan();

	/**
	 * The Navigable that is the target of the FK in the
	 * Container.  Generally speaking this returns the
	 * container's
	 *
	 * todo (6.0) : this could really be part of `Fetchable`
	 * 		- note however that for a managed-type association
	 * 			(component, entity, etc) the implication is
	 * 			different.  there it is the target Navigable
	 * 			in the associated managed type
	 *		- I think the composite impls should always just
	 *			delegate this call to their container
	 */
	Navigable getForeignKeyTargetNavigable();

	/**
	 * Access information about the FK mapping to the "owner" of this collection
	 */
	CollectionKey getCollectionKeyDescriptor();

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
	<I> CollectionIndex<I> getIndexDescriptor();

	/**
	 * For sorted collections, the comparator to use.  Non-parameterized
	 * because for SORTED_SET the elements are compared but for SORTED_MAP the
	 * keys are compared
	 *
	 * @see CollectionClassification#SORTED_MAP
	 * @see CollectionClassification#SORTED_SET
	 */
	default Comparator<?> getSortingComparator() {
		// most impls have none
		return null;
	}


	// todo : in terms of SqmNavigableSource.findNavigable() impl, be sure to only recognize:
	//			1) {key}
	//			2) {keys}
	//			3) {index}
	//			4) {indices}
	//			5) {element}
	//			6) {elements}
	//			7) {value}
	//			8) {values}
	//
	//			9) {entry} ? - for Map.Entry
	//
	//		- with or without the braces.  Meaning both `c.{element}` and `c.element` are
	//			recognized


	@Override
	@SuppressWarnings("unchecked")
	default <N> Navigable<N> findNavigable(String navigableName) {
		if ( "key".equals( navigableName ) || "{key}".equals( navigableName )
				|| "keys".equals( navigableName ) || "{keys}".equals( navigableName )
				|| "index".equals( navigableName ) || "{index}".equals( navigableName )
				|| "indices".equals( navigableName ) || "{indices}".equals( navigableName ) ) {
			return getIndexDescriptor();
		}

		if ( "element".equals( navigableName ) || "{element}".equals( navigableName )
				|| "elements".equals( navigableName ) || "{elements}".equals( navigableName )
				|| "value".equals( navigableName ) || "{value}".equals( navigableName )
				|| "values".equals( navigableName ) || "{values}".equals( navigableName ) ) {
			return (Navigable<N>) getElementDescriptor();
		}

		return null;
	}

	@Override
	default void visitNavigables(NavigableVisitationStrategy visitor) {
		if ( getIndexDescriptor() != null ) {
			getIndexDescriptor().visitNavigable( visitor );
		}

		getElementDescriptor().visitNavigable( visitor );
	}

	@Override
	default String getNavigableName() {
		return getNavigableRole().getNavigableName();
	}

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitNavigables( visitor );
	}

	/**
	 * todo (6.0) : remove this method
	 *
	 * @deprecated Use {@link #getSemantics()} ()} instead
	 */
	@Deprecated
	CollectionSemantics getTuplizer();

	@Override
	default boolean canCompositeContainCollections() {
		return false;
	}


	/**
	 * @todo (6.0) what args?
	 */
	CollectionLoader getLoader();

	Table getSeparateCollectionTable();

	/**
	 * Get the "space" that holds the persistent state
	 */
	Set<String> getCollectionSpaces();


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
	//
	// better to maybe do this, but just internally.  IOW, leave these methods but
	// have the impls be like, e.g.: `getSizeExecutor.

	int getSize(Object loadedKey, SharedSessionContractImplementor session);

	Boolean indexExists(Object loadedKey, Object index, SharedSessionContractImplementor session);

	Boolean elementExists(Object loadedKey, Object element, SharedSessionContractImplementor session);

	Object getElementByIndex(Object loadedKey, Object index, SharedSessionContractImplementor session, Object owner);




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	default PersistenceType getPersistenceType() {
		switch ( getElementDescriptor().getClassification() ) {
			case BASIC: {
				return PersistenceType.BASIC;
			}
			case EMBEDDABLE: {
				return PersistenceType.EMBEDDABLE;
			}
			case MANY_TO_MANY:
			case ONE_TO_MANY: {
				return PersistenceType.ENTITY;
			}
			case ANY: {
				// todo (6.0) : check against setting controlling how to handle JPA methods for extension stuff
				return null;
			}
			default: {
				throw new IllegalStateException( "Unrecognized collection element classification : " + getElementDescriptor().getClassification() );
			}
		}
	}




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// legacy stuff


	/**
	 * @deprecated Use {@link #getNavigableRole()} instead.
	 */
	@Deprecated
	default String getRoleName() {
		return getNavigableRole().getFullPath();
	}

	/**
	 * Get the cache structure
	 */
	CacheEntryStructure getCacheEntryStructure();

	default JavaTypeDescriptor getKeyJavaTypeDescriptor() {
		return getIndexDescriptor() == null ? null : getIndexDescriptor().getJavaTypeDescriptor();
	}

	default SimpleTypeDescriptor getKeyDomainTypeDescriptor() {
		return getIndexDescriptor() == null ? null : getIndexDescriptor().getDomainTypeDescriptor();
	}

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

	/**
	 * Completely remove the persistent state of the collection
	 */
	default void remove(Object key, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * (Re)create the collection's persistent state
	 */
	default void recreate(
			PersistentCollection collection,
			Object key,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Delete the persistent state of any elements that were removed from
	 * the collection
	 */
	default void deleteRows(
			PersistentCollection collection,
			Object key,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Update the persistent state of any elements that were modified
	 */
	default void updateRows(
			PersistentCollection collection,
			Object key,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Insert the persistent state of any new collection elements
	 */
	default void insertRows(
			PersistentCollection collection,
			Object key,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Process queued operations within the PersistentCollection.
	 */
	default void processQueuedOps(
			PersistentCollection collection,
			Object key,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

//	/**
//	 * Get the name of this collection role (the fully qualified class name,
//	 * extended by a "property path")
//	 */
//	String getRole();
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

	default boolean hasCache() {
		return getCacheAccess() != null;
	}

	CollectionDataAccess getCacheAccess();

	void initialize(Object loadedKey, SharedSessionContractImplementor session);

	// todo (6.0) : re-eval the whole timing + style + batch-size
	int getBatchSize();
//
//	CollectionMetadata getCollectionDescriptor();
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// "owner"-related stuff
	//
	//		todo (6.0) : rethink all of these...

	EntityTypeDescriptor findEntityOwnerDescriptor();

	/**
	 * @return the name of the property this collection is mapped by
	 */
	String getMappedByProperty();

	/**
	 * Previously this was defined on CollectionType.
	 *
	 * As with all of these "owner"-related methods we need to come up with
	 * a better plan for handling that stuff.
	 */
	default Object getKeyOfOwner(Object owner, SessionImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default Iterator getElementsIterator(Object collection, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	default C instantiateRaw(int anticipatedSize) {
		return getSemantics().instantiateRaw( anticipatedSize, this );
	}

	default PersistentCollection<E> instantiateWrapper(
			SharedSessionContractImplementor session,
			Object key) {
		return getSemantics().instantiateWrapper( key, this, session );
	}

	default PersistentCollection<E> wrap(
			SharedSessionContractImplementor session,
			C rawCollection) {
		return getSemantics().wrap( rawCollection, this, session );
	}

	boolean contains(Object collection, Object childObject);

	default Object indexOf(Object collection, Object element) {
		throw new UnsupportedOperationException( "Collection type does not support indexes" );
	}
}
