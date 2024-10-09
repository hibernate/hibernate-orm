/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.internal.VersionGeneration;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.TableGroupFilterAliasGenerator;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.InFlightEntityMappingType;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.tree.from.RootTableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.VersionJavaType;

/**
 * A strategy for persisting a mapped {@linkplain jakarta.persistence.Entity
 * entity class}. An {@code EntityPersister} orchestrates rendering of the
 * SQL statements corresponding to basic lifecycle events, including
 * {@code insert}, {@code update}, and {@code delete} statements, and their
 * execution via JDBC.
 * <p>
 * Concrete implementations of this interface handle the
 * {@linkplain SingleTableEntityPersister single table},
 * {@linkplain JoinedSubclassEntityPersister joined}, and
 * {@linkplain UnionSubclassEntityPersister union} inheritance mapping
 * strategies, and to a certain extent abstract the details of those
 * mappings from collaborators.
 * <p>
 * This interface defines a contract between the persistence strategy and
 * the {@link org.hibernate.engine.spi.SessionImplementor session}. It does
 * not define operations that are required for querying, nor for loading by
 * outer join.
 * <p>
 * Unless a custom {@link org.hibernate.persister.spi.PersisterFactory} is
 * used, it is expected that implementations of {@code EntityPersister}
 * define a constructor accepting the following arguments:
 * <ol>
 *     <li>
 *         {@link org.hibernate.mapping.PersistentClass} - describes the
 *         metadata about the entity to be handled by the persister
 *     </li>
 *     <li>
 *         {@link EntityDataAccess} - the second level caching strategy for
 *         this entity
 *     </li>
 *     <li>
 *         {@link NaturalIdDataAccess} - the second level caching strategy
 *         for any natural id defined for this entity
 *     </li>
 *     <li>
 *         {@link org.hibernate.metamodel.spi.RuntimeModelCreationContext} -
 *         access to additional information useful while constructing the
 *         persister.
 *     </li>
 * </ol>
 * Implementations must be thread-safe (and preferably immutable).
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see org.hibernate.persister.spi.PersisterFactory
 * @see org.hibernate.persister.spi.PersisterClassResolver
 */
public interface EntityPersister extends EntityMappingType, EntityMutationTarget, RootTableGroupProducer, AttributeSource {

	/**
	 * Finish the initialization of this object.
	 * <p>
	 * The method {@link InFlightEntityMappingType#prepareMappingModel}
	 * must have been called for every entity persister before this method
	 * is invoked.
	 * <p>
	 * Called only once per {@link org.hibernate.SessionFactory} lifecycle,
	 * after all entity persisters have been instantiated.
	 *
	 * @throws MappingException Indicates an issue in the metadata.
	 */
	void postInstantiate() throws MappingException;

	/**
	 * Prepare loaders associated with the persister.  Distinct "phase"
	 * in building the persister after {@linkplain InFlightEntityMappingType#prepareMappingModel}
	 * and {@linkplain #postInstantiate()} have occurred.
	 * <p/>
	 * The distinct phase is used to ensure that all {@linkplain org.hibernate.metamodel.mapping.TableDetails}
	 * are available across the entire model
	 */
	default void prepareLoaders() {
	}

	/**
	 * Return the {@link org.hibernate.SessionFactory} to which this persister
	 * belongs.
	 *
	 * @return The owning {@code SessionFactory}.
	 */
	SessionFactoryImplementor getFactory();

	@Override
	default String getSqlAliasStem() {
		return SqlAliasStemHelper.INSTANCE.generateStemFromEntityName( getEntityName() );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // stuff that is persister-centric and/or EntityInfo-centric ~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the EntityEntryFactory indicated for the entity mapped by this persister.
	 *
	 * @return The proper EntityEntryFactory.
	 */
	EntityEntryFactory getEntityEntryFactory();

	/**
	 * Returns an object that identifies the space in which identifiers of
	 * this entity hierarchy are unique.  Might be a table name, a JNDI URL, etc.
	 *
	 * @return The root entity name.
	 */
	String getRootEntityName();

	/**
	 * The entity name which this persister maps.
	 *
	 * @return The name of the entity which this persister maps.
	 */
	String getEntityName();

	/**
	 * The strategy to use for SQM mutation statements where the target entity
	 * has multiple tables. Returns {@code null} to indicate that the entity
	 * does not have multiple tables.
	 */
	SqmMultiTableMutationStrategy getSqmMultiTableMutationStrategy();

	SqmMultiTableInsertStrategy getSqmMultiTableInsertStrategy();

	/**
	 * Retrieve the underlying entity metamodel instance.
	 *
	 *@return The metamodel
	 */
	EntityMetamodel getEntityMetamodel();

	/**
	 * Called from {@link EnhancementAsProxyLazinessInterceptor} to trigger load of
	 * the entity's non-lazy state as well as the named attribute we are accessing
	 * if it is still uninitialized after fetching non-lazy state.
	 */
	default Object initializeEnhancedEntityUsedAsProxy(
			Object entity,
			String nameOfAttributeBeingAccessed,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(
				"Initialization of entity enhancement used to act like a proxy is not supported by this EntityPersister : "
						+ getClass().getName()
		);
	}

	/**
	 * Determine whether the given name represents a subclass entity
	 * (or this entity itself) of the entity mapped by this persister.
	 *
	 * @param entityName The entity name to be checked.
	 * @return True if the given entity name represents either the entity
	 * mapped by this persister or one of its subclass entities; false
	 * otherwise.
	 */
	boolean isSubclassEntityName(String entityName);

	/**
	 * Returns an array of objects that identify spaces in which properties of
	 * this entity are persisted, for instances of this class only.
	 * <p>
	 * For most implementations, this returns the complete set of table names
	 * to which instances of the mapped entity are persisted (not accounting
	 * for superclass entity mappings).
	 *
	 * @return The property spaces.
	 */
	String[] getPropertySpaces();

	/**
	 * Returns an array of objects that identify spaces in which properties of
	 * this entity are persisted, for instances of this class and its subclasses.
	 * <p>
	 * Much like {@link #getPropertySpaces()}, except that here we include subclass
	 * entity spaces.
	 *
	 * @return The query spaces.
	 */
	Serializable[] getQuerySpaces();

	/**
	 * The table names this entity needs to be synchronized against.
	 * <p>
	 * Much like {@link #getPropertySpaces()}, except that here we include subclass
	 * entity spaces.
	 *
	 * @return The synchronization spaces.
	 */
	default String[] getSynchronizationSpaces() {
		return (String[]) getQuerySpaces();
	}

	/**
	 * Returns an array of objects that identify spaces in which properties of
	 * this entity are persisted, for instances of this class and its subclasses.
	 * <p>
	 * Much like {@link #getPropertySpaces()}, except that here we include subclass
	 * entity spaces.
	 *
	 * @return The query spaces.
	 */
	default String[] getSynchronizedQuerySpaces() {
		return (String[]) getQuerySpaces();
	}

	default void visitQuerySpaces(Consumer<String> querySpaceConsumer) {
		final String[] spaces = getSynchronizedQuerySpaces();
		for (String space : spaces) {
			querySpaceConsumer.accept(space);
		}
	}

	/**
	 * Determine whether this entity supports dynamic proxies.
	 *
	 * @return True if the entity has dynamic proxy support; false otherwise.
	 */
	boolean hasProxy();

	/**
	 * Determine whether this entity contains references to persistent collections.
	 *
	 * @return True if the entity does contain persistent collections; false otherwise.
	 */
	boolean hasCollections();

	/**
	 * Determine whether any properties of this entity are considered mutable.
	 *
	 * @return True if any properties of the entity are mutable; false otherwise (meaning none are).
	 */
	boolean hasMutableProperties();

	/**
	 * Determine whether this entity contains references to persistent collections
	 * which are fetchable by subselect?
	 *
	 * @return True if the entity contains collections fetchable by subselect; false otherwise.
	 */
	boolean hasSubselectLoadableCollections();

	/**
	 * Determine whether this entity contains references to persistent collections
	 * not referencing the primary key.
	 *
	 * @return True if the entity contains a collection not referencing the primary key; false otherwise.
	 * @since 6.2
	 */
	boolean hasCollectionNotReferencingPK();

	/**
	 * Determine whether this entity has any
	 * (non-{@linkplain org.hibernate.engine.spi.CascadeStyles#NONE none}) cascading.
	 *
	 * @return True if the entity has any properties with a cascade other than NONE;
	 *         false otherwise (aka, no cascading).
	 */
	boolean hasCascades();

	/**
	 * Determine whether this entity has any
	 * {@linkplain org.hibernate.engine.spi.CascadeStyles#DELETE delete cascading}.
	 *
	 * @return True if the entity has any properties with a cascade other than NONE;
	 *         false otherwise.
	 */
	default boolean hasCascadeDelete() {
		//bad default implementation for compatibility
		return hasCascades();
	}

	/**
	 * Determine whether this entity has any owned collections.
	 *
	 * @return True if the entity has an owned collection;
	 * false otherwise.
	 */
	default boolean hasOwnedCollections() {
		//bad default implementation for compatibility
		return hasCollections();
	}

	/**
	 * Determine whether instances of this entity are considered mutable.
	 *
	 * @return True if the entity is considered mutable; false otherwise.
	 */
	boolean isMutable();

	/**
	 * Determine whether the entity is inherited one or more other entities.
	 * In other words, is this entity a subclass of other entities.
	 *
	 * @return True if other entities extend this entity; false otherwise.
	 */
	boolean isInherited();

	/**
	 * Are identifiers of this entity assigned known before the insert execution?
	 * Or, are they generated (in the database) by the insert execution.
	 *
	 * @return True if identifiers for this entity are generated by the insert
	 * execution.
	 */
	boolean isIdentifierAssignedByInsert();

	/**
	 * Get the type of a particular property by name.
	 *
	 * @param propertyName The name of the property for which to retrieve
	 * the type.
	 * @return The type.
	 * @throws MappingException Typically indicates an unknown
	 * property name.
	 *
	 * @deprecated See {@linkplain #findAttributeMapping(String)}
	 */
	@Deprecated( since = "6", forRemoval = true )
	Type getPropertyType(String propertyName) throws MappingException;

	/**
	 * Compare the two snapshots to determine if they represent dirty state.
	 *
	 * @param currentState The current snapshot
	 * @param previousState The baseline snapshot
	 * @param owner The entity containing the state
	 * @param session The originating session
	 * @return The indices of all dirty properties, or null if no properties
	 * were dirty.
	 */
	int[] findDirty(Object[] currentState, Object[] previousState, Object owner, SharedSessionContractImplementor session);

	/**
	 * Compare the two snapshots to determine if they represent modified state.
	 *
	 * @param old The baseline snapshot
	 * @param current The current snapshot
	 * @param object The entity containing the state
	 * @param session The originating session
	 * @return The indices of all modified properties, or null if no properties
	 * were modified.
	 */
	int[] findModified(Object[] old, Object[] current, Object object, SharedSessionContractImplementor session);

	/**
	 * Determine whether the entity has a particular property holding
	 * the identifier value.
	 *
	 * @return True if the entity has a specific property holding identifier value.
	 */
	boolean hasIdentifierProperty();

	/**
	 * Determine whether detached instances of this entity carry their own
	 * identifier value.
	 * <p>
	 * The other option is the deprecated feature where users could supply
	 * the id during session calls.
	 *
	 * @return True if either (1) {@link #hasIdentifierProperty()} or
	 * 		(2) the identifier is an embedded composite identifier; false otherwise.
	 *
	 * @deprecated This feature is no longer supported
	 */
	@Deprecated(since = "6")
	default boolean canExtractIdOutOfEntity() {
		return true;
	}

	/**
	 * Determine whether optimistic locking by column is enabled for this
	 * entity.
	 *
	 * @return True if optimistic locking by column (i.e., {@code <version/>} or
	 * {@code <timestamp/>}) is enabled; false otherwise.
	 */
	boolean isVersioned();

	/**
	 * If {@link #isVersioned()}, then what is the type of the property
	 * holding the locking value.
	 *
	 * @return The type of the version property; or null, if not versioned.
	 */
	BasicType<?> getVersionType();

	@SuppressWarnings("unchecked")
	default VersionJavaType<Object> getVersionJavaType() {
		return (VersionJavaType<Object>) getVersionType().getJavaTypeDescriptor();
	}

	/**
	 * If {@link #isVersioned()}, then what is the index of the property
	 * holding the locking value.
	 *
	 * @return The type of the version property; or -66, if not versioned.
	 */
	int getVersionProperty();

	/**
	 * Determine whether this entity defines a natural identifier.
	 *
	 * @return True if the entity defines a natural id; false otherwise.
	 */
	boolean hasNaturalIdentifier();

	/**
	 * If the entity defines a natural id, that is, if
	 * {@link #hasNaturalIdentifier()} returns {@code true}, the indices
	 * of the properties which make up the natural id.
	 *
	 * @return The indices of the properties making up the natural id;
	 *         or null, if no natural id is defined.
	 */
	int[] getNaturalIdentifierProperties();

	/**
	 * Retrieve the current state of the natural-id properties from the database.
	 *
	 * @param id The identifier of the entity for which to retrieve the natural-id values.
	 * @param session The session from which the request originated.
	 * @return The natural-id snapshot.
	 */
	Object getNaturalIdentifierSnapshot(Object id, SharedSessionContractImplementor session);

	/**
	 * Determine which identifier generation strategy is used for this entity.
	 *
	 * @return The identifier generation strategy.
	 *
	 * @deprecated use {@link #getGenerator()}
	 */
	@Deprecated
	IdentifierGenerator getIdentifierGenerator();

	default Generator getGenerator() {
		return getIdentifierGenerator();
	}

	default BeforeExecutionGenerator getVersionGenerator() {
		return new VersionGeneration( getVersionMapping() );
	}

	@Override
	default AttributeMapping getAttributeMapping(int position) {
		return getAttributeMappings().get( position );
	}

	@Override
	default <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;
		if ( domainValue instanceof Object[] ) {
			final Object[] values = (Object[]) domainValue;
			for ( int i = 0; i < getNumberOfAttributeMappings(); i++ ) {
				final AttributeMapping attributeMapping = getAttributeMapping( i );
				span += attributeMapping.breakDownJdbcValues( values[ i ], offset + span, x, y, valueConsumer, session );
			}
		}
		else {
			for ( int i = 0; i < getNumberOfAttributeMappings(); i++ ) {
				final AttributeMapping attributeMapping = getAttributeMapping( i );
				final Object attributeValue = attributeMapping.getValue( domainValue );
				span += attributeMapping.breakDownJdbcValues(
						attributeValue,
						offset + span,
						x,
						y,
						valueConsumer,
						session
				);
			}
		}
		return span;
	}

	/**
	 * Determine whether this entity defines any lazy properties (when bytecode
	 * instrumentation is enabled).
	 *
	 * @return True if the entity has properties mapped as lazy; false otherwise.
	 */
	boolean hasLazyProperties();

	default NaturalIdLoader<?> getNaturalIdLoader() {
		throw new UnsupportedOperationException(
				"EntityPersister implementation '" + getClass().getName()
						+ "' does not support 'NaturalIdLoader'"
		);
	}

	default MultiNaturalIdLoader<?> getMultiNaturalIdLoader() {
		throw new UnsupportedOperationException(
				"EntityPersister implementation '" + getClass().getName()
						+ "' does not support 'MultiNaturalIdLoader'"
		);
	}

	/**
	 * Load the id for the entity based on the natural id.
	 */
	Object loadEntityIdByNaturalId(
			Object[] naturalIdValues,
			LockOptions lockOptions,
			SharedSessionContractImplementor session);

	/**
	 * Load an instance of the persistent class.
	 */
	Object load(Object id, Object optionalObject, LockMode lockMode, SharedSessionContractImplementor session);

	/**
	 * @deprecated Use {@link #load(Object, Object, LockMode, SharedSessionContractImplementor)}
	 */
	@Deprecated(since = "6.0")
	default Object load(
			Object id, Object optionalObject, LockMode lockMode, SharedSessionContractImplementor session,
			@SuppressWarnings("unused") Boolean readOnly)
			throws HibernateException {
		return load( id, optionalObject, lockMode, session );
	}

	/**
	 * Load an instance of the persistent class.
	 */
	Object load(Object id, Object optionalObject, LockOptions lockOptions, SharedSessionContractImplementor session);

	default Object load(Object id, Object optionalObject, LockOptions lockOptions, SharedSessionContractImplementor session, Boolean readOnly)
			throws HibernateException {
		return load( id, optionalObject, lockOptions, session );
	}

	/**
	 * Performs a load of multiple entities (of this type) by identifier simultaneously.
	 *
	 * @param ids The identifiers to load
	 * @param session The originating Session
	 * @param loadOptions The options for loading
	 *
	 * @return The loaded, matching entities
	 */
	List<?> multiLoad(Object[] ids, EventSource session, MultiIdLoadOptions loadOptions);

	@Override
	default Object loadByUniqueKey(String propertyName, Object uniqueKey, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(
				"EntityPersister implementation '" + getClass().getName()
						+ "' does not support 'UniqueKeyLoadable'"
		);
	}

	/**
	 * Do a version check (optional operation)
	 */
	void lock(Object id, Object version, Object object, LockMode lockMode, EventSource session);

	/**
	 * Do a version check (optional operation)
	 */
	void lock(Object id, Object version, Object object, LockOptions lockOptions, EventSource session);

	/**
	 * Persist an instance
	 *
	 * @see #getInsertCoordinator()
	 * @deprecated Use {@link InsertCoordinator#insert(Object, Object, Object[], SharedSessionContractImplementor)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	default void insert(Object id, Object[] fields, Object object, SharedSessionContractImplementor session) {
		getInsertCoordinator().insert( object, id, fields, session );
	}

	/**
	 * Persist an instance
	 *
	 * @see #getInsertCoordinator()
	 * @deprecated Use {@link InsertCoordinator#insert(Object, Object[], SharedSessionContractImplementor)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	default Object insert(Object[] fields, Object object, SharedSessionContractImplementor session) {
		final GeneratedValues generatedValues = getInsertCoordinator().insert( object, fields, session );
		return generatedValues == null ? null : generatedValues.getGeneratedValue( getIdentifierMapping() );
	}

	/**
	 * Delete a persistent instance
	 *
	 * @see #getDeleteCoordinator()
	 * @deprecated Use {@link DeleteCoordinator#delete} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	default void delete(Object id, Object version, Object object, SharedSessionContractImplementor session) {
		getDeleteCoordinator().delete( object, id, version, session );
	}

	/**
	 * Update a persistent instance
	 *
	 * @see #getUpdateCoordinator()
	 * @deprecated Use {@link UpdateCoordinator#update} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	default void update(
			Object id,
			Object[] fields,
			int[] dirtyFields,
			boolean hasDirtyCollection,
			Object[] oldFields,
			Object oldVersion,
			Object object,
			Object rowId,
			SharedSessionContractImplementor session) {
		getUpdateCoordinator().update(
				object,
				id,
				rowId,
				fields,
				oldVersion,
				oldFields,
				dirtyFields,
				hasDirtyCollection,
				session
		);
	}

	/**
	 * Merge a persistent instance
	 *
	 * @see #getMergeCoordinator()
	 * @deprecated Use {@link UpdateCoordinator#update} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	default void merge(
			Object id,
			Object[] fields,
			int[] dirtyFields,
			boolean hasDirtyCollection,
			Object[] oldFields,
			Object oldVersion,
			Object object,
			Object rowId,
			SharedSessionContractImplementor session) {
		getMergeCoordinator().update(
				object,
				id,
				rowId,
				fields,
				oldVersion,
				oldFields,
				dirtyFields,
				hasDirtyCollection,
				session
		);
	}

	/**
	 * Get the insert coordinator instance.
	 *
	 * @since 6.5
	 */
	InsertCoordinator getInsertCoordinator();

	/**
	 * Get the update coordinator instance.
	 *
	 * @since 6.5
	 */
	UpdateCoordinator getUpdateCoordinator();

	/**
	 * Get the delete coordinator instance.
	 *
	 * @since 6.5
	 */
	DeleteCoordinator getDeleteCoordinator();

	/**
	 * Get the merge coordinator instance.
	 *
	 * @since 6.5
	 */
	default UpdateCoordinator getMergeCoordinator() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the Hibernate types of the class properties
	 */
	Type[] getPropertyTypes();

	/**
	 * Get the names of the class properties - doesn't have to be the names of the
	 * actual Java properties (used for XML generation only)
	 */
	String[] getPropertyNames();

	/**
	 * Get the "insertability" of the properties of this class
	 * (does the property appear in an SQL INSERT)
	 */
	boolean[] getPropertyInsertability();

	/**
	 * Get the "updateability" of the properties of this class
	 * (does the property appear in an SQL UPDATE)
	 */
	boolean[] getPropertyUpdateability();

	/**
	 * Get the "checkability" of the properties of this class
	 * (is the property dirty checked, does the cache need
	 * to be updated)
	 */
	boolean[] getPropertyCheckability();

	/**
	 * Get the nullability of the properties of this class
	 */
	boolean[] getPropertyNullability();

	/**
	 * Get the "versionability" of the properties of this class
	 * (is the property optimistic-locked)
	 */
	boolean[] getPropertyVersionability();
	boolean[] getPropertyLaziness();
	/**
	 * Get the cascade styles of the properties (optional operation)
	 */
	CascadeStyle[] getPropertyCascadeStyles();

	default boolean isPropertySelectable(int propertyNumber) {
		return true;
	}

	/**
	 * Get the identifier type
	 */
	Type getIdentifierType();

	/**
	 * Get the name of the identifier property (or return null) - need not return the
	 * name of an actual Java property
	 */
	String getIdentifierPropertyName();

	/**
	 * Should we always invalidate the cache instead of
	 * recaching updated state
	 */
	boolean isCacheInvalidationRequired();
	/**
	 * Should lazy properties of this entity be cached?
	 */
	boolean isLazyPropertiesCacheable();

	boolean canReadFromCache();
	boolean canWriteToCache();

	/**
	 * Does this class have a cache.
	 *
	 * @deprecated Use {@link #canReadFromCache()} and/or {@link #canWriteToCache()}
	 *             depending on need
	 */
	@Deprecated
	boolean hasCache();
	/**
	 * Get the cache (optional operation)
	 */
	EntityDataAccess getCacheAccessStrategy();
	/**
	 * Get the cache structure
	 */
	CacheEntryStructure getCacheEntryStructure();

	CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SharedSessionContractImplementor session);

	/**
	 * Does this class have a natural id cache
	 */
	boolean hasNaturalIdCache();

	/**
	 * Get the NaturalId cache (optional operation)
	 */
	NaturalIdDataAccess getNaturalIdCacheAccessStrategy();

	/**
	 * Get the user-visible metadata for the class (optional operation)
	 *
	 * @deprecated This operation is no longer called by Hibernate.
	 */
	@Deprecated(since = "6.0")
	ClassMetadata getClassMetadata();

	/**
	 * The batch size for batch loading.
	 *
	 * @see org.hibernate.engine.spi.LoadQueryInfluencers#effectiveBatchSize(EntityPersister)
	 */
	default int getBatchSize() {
		return -1;
	}

	/**
	 * Is batch loading enabled?
	 *
	 * @see org.hibernate.engine.spi.LoadQueryInfluencers#effectivelyBatchLoadable(EntityPersister)
	 */
	default boolean isBatchLoadable() {
		return getBatchSize() > 1;
	}

	/**
	 * Is select snapshot before update enabled?
	 */
	boolean isSelectBeforeUpdateRequired();

	/**
	 * Get the current database state of the object, in a "hydrated" form,
	 * without resolving identifiers.
	 *
	 * @return null if there is no row in the database
	 */
	Object[] getDatabaseSnapshot(Object id, SharedSessionContractImplementor session) throws HibernateException;

	Object getIdByUniqueKey(Object key, String uniquePropertyName, SharedSessionContractImplementor session);

	/**
	 * Get the current version of the object, or return null if there is no
	 * row for the given identifier. In the case of unversioned data, return
	 * any object if the row exists.
	 */
	Object getCurrentVersion(Object id, SharedSessionContractImplementor session) throws HibernateException;

	Object forceVersionIncrement(Object id, Object currentVersion, SharedSessionContractImplementor session) throws HibernateException;

	default Object forceVersionIncrement(
			Object id,
			Object currentVersion,
			boolean batching,
			SharedSessionContractImplementor session) throws HibernateException {
		return forceVersionIncrement( id, currentVersion, session );
	}

	/**
	 * Has the class actually been bytecode instrumented?
	 */
	boolean isInstrumented();

	/**
	 * Does this entity define any properties as being database generated on insert?
	 *
	 * @return True if this entity contains at least one property defined
	 * as generated (including version property, but not identifier).
	 */
	boolean hasInsertGeneratedProperties();

	/**
	 * Does this entity define any properties as being database generated on update?
	 *
	 * @return True if this entity contains at least one property defined
	 * as generated (including version property, but not identifier).
	 */
	boolean hasUpdateGeneratedProperties();

	/**
	 * Does this entity contain a version property that is defined
	 * to be database generated?
	 *
	 * @return true if this entity contains a version property and that
	 * property has been marked as generated.
	 */
	boolean isVersionPropertyGenerated();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is tuplizer-centric, but is passed a session ~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Called just after the entities properties have been initialized
	 */
	void afterInitialize(Object entity, SharedSessionContractImplementor session);

	/**
	 * Called just after the entity has been reassociated with the session
	 */
	void afterReassociate(Object entity, SharedSessionContractImplementor session);

	/**
	 * Create a new proxy instance
	 */
	Object createProxy(Object id, SharedSessionContractImplementor session);

	/**
	 * Is this a new transient instance?
	 */
	Boolean isTransient(Object object, SharedSessionContractImplementor session);

	/**
	 * Return the values of the insertable properties of the object (including backrefs)
	 */
	Object[] getPropertyValuesToInsert(Object object, Map<Object,Object> mergeMap, SharedSessionContractImplementor session);

	/**
	 * Perform a select to retrieve the values of any generated properties
	 * back from the database, injecting these generated values into the
	 * given entity as well as writing this state to the
	 * {@link org.hibernate.engine.spi.PersistenceContext}.
	 * <p>
	 * Note, that because we update the PersistenceContext here, callers
	 * need to take care that they have already written the initial snapshot
	 * to the PersistenceContext before calling this method.
	 * @deprecated Use {@link #processInsertGeneratedProperties(Object, Object, Object[], GeneratedValues, SharedSessionContractImplementor)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	default void processInsertGeneratedProperties(Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		processInsertGeneratedProperties( id, entity, state, null, session );
	}

	/**
	 * Retrieve the values of any insert generated properties through the provided
	 * {@link GeneratedValues} or, when that's not available, by selecting them
	 * back from the database, injecting these generated values into the
	 * given entity as well as writing this state to the
	 * {@link org.hibernate.engine.spi.PersistenceContext}.
	 * <p>
	 * Note, that because we update the PersistenceContext here, callers
	 * need to take care that they have already written the initial snapshot
	 * to the PersistenceContext before calling this method.
	 */
	default void processInsertGeneratedProperties(
			Object id,
			Object entity,
			Object[] state,
			GeneratedValues generatedValues,
			SharedSessionContractImplementor session) {
	}

	default List<? extends ModelPart> getGeneratedProperties(EventType timing) {
		return timing == EventType.INSERT ? getInsertGeneratedProperties() : getUpdateGeneratedProperties();
	}

	default List<? extends ModelPart> getInsertGeneratedProperties() {
		return Collections.emptyList();
	}

	/**
	 * Perform a select to retrieve the values of any generated properties
	 * back from the database, injecting these generated values into the
	 * given entity as well as writing this state to the
	 * {@link org.hibernate.engine.spi.PersistenceContext}.
	 * <p>
	 * Note, that because we update the PersistenceContext here, callers
	 * need to take care that they have already written the initial snapshot
	 * to the PersistenceContext before calling this method.
	 * @deprecated Use {@link #processUpdateGeneratedProperties(Object, Object, Object[], GeneratedValues, SharedSessionContractImplementor)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	default void processUpdateGeneratedProperties(Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		processUpdateGeneratedProperties( id, entity, state, null, session );
	}

	/**
	 * Retrieve the values of any update generated properties through the provided
	 * {@link GeneratedValues} or, when that's not available, by selecting them
	 * back from the database, injecting these generated values into the
	 * given entity as well as writing this state to the
	 * {@link org.hibernate.engine.spi.PersistenceContext}.
	 * <p>
	 * Note, that because we update the PersistenceContext here, callers
	 * need to take care that they have already written the initial snapshot
	 * to the PersistenceContext before calling this method.
	 */
	void processUpdateGeneratedProperties(
			Object id,
			Object entity,
			Object[] state,
			GeneratedValues generatedValues,
			SharedSessionContractImplementor session);

	default List<? extends ModelPart> getUpdateGeneratedProperties() {
		return Collections.emptyList();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is Tuplizer-centric ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The persistent class, or null
	 */
	Class<?> getMappedClass();

	/**
	 * Does the class implement the {@link org.hibernate.classic.Lifecycle} interface?
	 */
	boolean implementsLifecycle();

	/**
	 * Get the proxy interface that instances of <em>this</em> concrete class will be
	 * cast to (optional operation).
	 */
	Class<?> getConcreteProxyClass();

	default void setValues(Object object, Object[] values) {
		setPropertyValues( object, values );
	}

	/**
	 * Set the given values to the mapped properties of the given object.
	 *
	 * @deprecated Use {@link #setValues} instead
	 */
	@Deprecated(since = "6.0")
	void setPropertyValues(Object object, Object[] values);

	default void setValue(Object object, int i, Object value) {
		setPropertyValue( object, i, value );
	}

	/**
	 * Set the value of a particular property of the given instance.
	 *
	 * @deprecated Use {@link #setValue} instead
	 */
	@Deprecated(since = "6.0")
	void setPropertyValue(Object object, int i, Object value);

	default Object[] getValues(Object object) {
		return getPropertyValues( object );
	}

	/**
	 * @deprecated Use {@link #getValues} instead
	 */
	@Deprecated(since  = "6.0")
	Object[] getPropertyValues(Object object);

	default Object getValue(Object object, int i) {
		return getPropertyValue( object, i );
	}

	/**
	 * @deprecated Use {@link #getValue} instead
	 */
	@Deprecated(since = "6.0")
	Object getPropertyValue(Object object, int i) throws HibernateException;

	/**
	 * Get the value of a particular property
	 */
	Object getPropertyValue(Object object, String propertyName);

	/**
	 * Get the identifier of an instance from the object's identifier property.
	 * Throw an exception if it has no identifier property.
	 */
	Object getIdentifier(Object entity, SharedSessionContractImplementor session);

	/**
	 * Get the identifier of an instance from the object's identifier property.
	 * Throw an exception if it has no identifier property.
	 *
	 * It's supposed to be use during the merging process
	 */
	default Object getIdentifier(Object entity, MergeContext mergeContext) {
		return getIdentifier( entity, mergeContext.getEventSource() );
	}

	/**
	 * Inject the identifier value into the given entity.
	 */
	void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session);

	/**
	 * Get the version number (or timestamp) from the object's version property.
	 * Return {@code null} if it is not versioned.
	 */
	Object getVersion(Object object) throws HibernateException;

	/**
	 * Create a class instance initialized with the given identifier
	 *
	 * @param id The identifier value to use (may be null to represent no value)
	 * @param session The session from which the request originated.
	 *
	 * @return The instantiated entity.
	 */
	Object instantiate(Object id, SharedSessionContractImplementor session);

	/**
	 * Is the given object an instance of this entity?
	 */
	boolean isInstance(Object object);

	/**
	 * Does the given instance have any uninitialized lazy properties?
	 */
	boolean hasUninitializedLazyProperties(Object object);

	/**
	 * Set the identifier and version of the given instance back to its "unsaved"
	 * value, that is, the value it had before it was made persistent.
	 */
	void resetIdentifier(Object entity, Object currentId, Object currentVersion, SharedSessionContractImplementor session);

	/**
	 * Obtain the {@code EntityPersister} for the concrete class of the given
	 * entity instance which participates in a mapped inheritance hierarchy
	 * with this persister. The given instance must be an instance of a subclass
	 * of the persistent class managed by this persister.
	 * <p>
	 * A request has already identified the entity name of this persister as the
	 * mapping for the given instance. However, we still need to account for
	 * possible subclassing and potentially reroute to the more appropriate
	 * persister.
	 * <p>
	 * For example, a request names {@code Animal} as the entity name which gets
	 * resolved to this persister.  But the actual instance is really an instance
	 * of {@code Cat} which is a subclass of {@code Animal}. So, here the
	 * {@code Animal} persister is being asked to return the persister specific
	 * to {@code Cat}.
	 * <p>
	 * It's also possible that the instance is actually an {@code Animal} instance
	 * in the above example in which case we would return {@code this} from this
	 * method.
	 *
	 * @param instance The entity instance
	 * @param factory Reference to the SessionFactory
	 *
	 * @return The appropriate persister
	 *
	 * @throws HibernateException Indicates that instance was deemed to not be a
	 *                            subclass of the entity mapped by this persister.
	 */
	EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory);

	EntityRepresentationStrategy getRepresentationStrategy();

	@Override
	default EntityMappingType getEntityMappingType() {
		return this;
	}

	@Override
	default void addToCacheKey(
			MutableCacheKeyBuilder cacheKey,
			Object value,
			SharedSessionContractImplementor session) {
		getIdentifierMapping().addToCacheKey( cacheKey, getIdentifier( value, session ), session );
	}

	BytecodeEnhancementMetadata getInstrumentationMetadata();

	default BytecodeEnhancementMetadata getBytecodeEnhancementMetadata() {
		return getInstrumentationMetadata();
	}

	FilterAliasGenerator getFilterAliasGenerator(final String rootAlias);

	default FilterAliasGenerator getFilterAliasGenerator(TableGroup rootTableGroup) {
		assert this instanceof Joinable;
		return new TableGroupFilterAliasGenerator( ( (Joinable) this ).getTableName(), rootTableGroup );
	}

	/**
	 * Converts an array of attribute names to a set of indexes, according to the entity metamodel
	 *
	 * @param attributeNames Array of names to be resolved
	 *
	 * @return A set of unique indexes of the attribute names found in the metamodel
	 */
	int[] resolveAttributeIndexes(String[] attributeNames);

	/**
	 * Like {@link #resolveAttributeIndexes(String[])} but also always returns mutable attributes
	 *
	 * @param attributeNames Array of names to be resolved
	 *
	 * @return A set of unique indexes of the attribute names found in the metamodel
	 */
	default int[] resolveDirtyAttributeIndexes(
			Object[] values,
			Object[] loadedState,
			String[] attributeNames,
			SessionImplementor session) {
		return resolveAttributeIndexes( attributeNames );
	}

	boolean canUseReferenceCacheEntries();

	@Incubating
	boolean useShallowQueryCacheLayout();

	@Incubating
	boolean storeDiscriminatorInShallowQueryCacheLayout();

	boolean hasFilterForLoadByKey();

	/**
	 * The property name of the "special" identifier property in HQL
	 *
	 * @deprecated this feature of HQL is now deprecated
	 */
	@Deprecated(since = "6.2")
	String ENTITY_ID = "id";

	/**
	 * @return Metadata for each unique key defined
	 */
	@Incubating
	Iterable<UniqueKeyEntry> uniqueKeyEntries();

	/**
	 * Get a SQL select string that performs a select based on a unique
	 * key determined by the given property name.
	 *
	 * @param propertyName The name of the property which maps to the
	 *           column(s) to use in the select statement restriction.
	 * @return The SQL select string
	 */
	String getSelectByUniqueKeyString(String propertyName);

	/**
	 * Get a SQL select string that performs a select based on a unique
	 * key determined by the given property names.
	 *
	 * @param propertyNames The names of the properties which maps to the
	 *               column(s) to use in the select statement restriction.
	 * @return The SQL select string
	 */
	default String getSelectByUniqueKeyString(String[] propertyNames) {
		// default impl only for backward compatibility
		if ( propertyNames.length > 1 ) {
			throw new IllegalArgumentException( "support for multiple properties not implemented" );
		}
		return getSelectByUniqueKeyString( propertyNames[0] );
	}

	String getSelectByUniqueKeyString(String[] propertyNames, String[] columnNames);


	/**
	 * The names of the primary key columns in the root table.
	 *
	 * @return The primary key column names.
	 */
	String[] getRootTableKeyColumnNames();

	/**
	 * Get the database-specific SQL command to retrieve the last
	 * generated IDENTITY value.
	 *
	 * @return The SQL command string
	 */
	String getIdentitySelectString();

	String[] getIdentifierColumnNames();
}
