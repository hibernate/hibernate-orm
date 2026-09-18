/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import jakarta.persistence.Entity;
import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.loader.ast.internal.TenantIdLoader;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadeStyles;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.internal.VersionGeneration;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.InFlightEntityMappingType;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.persister.filter.internal.TableGroupFilterAliasGenerator;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.spi.creation.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.query.from.RootTableGroupProducer;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.VersionJavaType;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.hibernate.internal.util.StringHelper.unqualifyEntityName;

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
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
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
	void postInstantiate(@Nonnull PersistentClass bootEntityDescriptor) throws MappingException;

	/**
	 * Prepare loaders associated with the persister.
	 * <p>
	 * Distinct "phase" in building the persister after
	 * {@linkplain InFlightEntityMappingType#prepareMappingModel} and
	 * {@linkplain #postInstantiate(PersistentClass)} have occurred.
	 * <p>
	 * The distinct phase is used to ensure that all
	 * {@linkplain org.hibernate.metamodel.mapping.TableDetails}
	 * are available across the entire model
	 */
	default void prepareLoaders() {
	}

	/**
	 * Build {@link org.hibernate.action.queue.spi.meta.TableDescriptor}s
	 * early, before loaders.
	 * <p>
	 * This is separated from {@link #prepareLoaders()} to ensure all
	 * table descriptors are available across the entire model hierarchy
	 * before any persister tries to access them (e.g., subclass persisters
	 * accessing root persister's table descriptors).
	 */
	default void buildTableDescriptorsEarly() {
	}

	/**
	 * The {@link org.hibernate.SessionFactory} to which this persister
	 * belongs.
	 */
	@Nonnull
	SessionFactoryImplementor getFactory();

	@Nonnull
	@Override
	default String getSqlAliasStem() {
		return SqlAliasStemHelper.INSTANCE.generateStemFromEntityName( getEntityName() );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is persister-centric and/or EntityInfo-centric ~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns an object that identifies the space in which identifiers of
	 * this entity hierarchy are unique.
	 * <p>
	 * Might be a table name, a JNDI URL, etc.
	 *
	 * @return The root entity name.
	 */
	@Nonnull
	String getRootEntityName();

	/**
	 * The entity name which this persister maps.
	 *
	 * @return The name of the entity which this persister maps.
	 */
	@Nonnull
	String getEntityName();

	/**
	 * The {@linkplain Entity#name() JPA entity name}, if one, associated with the entity.
	 */
	@Nullable
	String getJpaEntityName();

	@Nonnull
	default String getImportedName() {
		final String entityName = getJpaEntityName();
		return entityName == null
				? unqualifyEntityName( getEntityName() )
				: entityName;
	}

	/**
	 * The strategy to use for SQM mutation statements where the target
	 * entity has multiple tables. Returns {@code null} to indicate that
	 * the entity does not have multiple tables.
	 */
	@Nullable
	SqmMultiTableMutationStrategy getSqmMultiTableMutationStrategy();

	@Nullable
	SqmMultiTableInsertStrategy getSqmMultiTableInsertStrategy();

	boolean isPolymorphic();

	boolean isDynamicUpdate();

	boolean isDynamicInsert();

	@Nonnull
	OnDeleteAction[] getPropertyOnDeleteActions();

	@Nonnull
	Generator[] getGenerators();

	boolean hasImmutableNaturalId();

	boolean isNaturalIdentifierInsertGenerated();

	boolean isLazy();

	int getPropertySpan();

	boolean hasPreInsertGeneratedProperties();

	boolean hasPreUpdateGeneratedProperties();

	/**
	 * Called from {@link EnhancementAsProxyLazinessInterceptor} to trigger load of
	 * the entity's non-lazy state as well as the named attribute we are accessing
	 * if it is still uninitialized after fetching non-lazy state.
	 */
	@Nullable
	default Object initializeEnhancedEntityUsedAsProxy(
			@Nonnull Object entity,
			@Nullable String nameOfAttributeBeingAccessed,
			@Nonnull SharedSessionContractImplementor session) {
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
	boolean isSubclassEntityName(@Nonnull String entityName);

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
	@Nonnull
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
	@Nonnull
	Serializable[] getQuerySpaces();

	/**
	 * Returns an array of objects that identify spaces in which properties of
	 * this entity are persisted, for instances of this class and its subclasses.
	 * <p>
	 * Much like {@link #getPropertySpaces()}, except that here we include subclass
	 * entity spaces.
	 *
	 * @return The query spaces.
	 */
	@Nonnull
	default String[] getSynchronizedQuerySpaces() {
		return (String[]) getQuerySpaces();
	}

	default void visitQuerySpaces(@Nonnull Consumer<String> querySpaceConsumer) {
		for ( String space : getSynchronizedQuerySpaces() ) {
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
	 * Determine whether this entity contains references to attributes
	 * which are fetchable by subselect?
	 *
	 * @return True if the entity contains attributes fetchable by subselect; false otherwise.
	 */
	boolean hasSubselectLoadableAttributes();

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
	 * {@linkplain org.hibernate.cascade.spi.CascadeStyles#NONE cascading} operations.
	 *
	 * @return True if the entity has any properties with a cascade other than NONE;
	 *         false otherwise (aka, no cascading).
	 */
	boolean hasCascades();

	/**
	 * Determine whether this entity has any
	 * {@linkplain org.hibernate.cascade.spi.CascadeStyles#PERSIST persist cascading}.
	 *
	 * @return True if the entity has any properties with a cascade PERSIST or ALL;
	 *         false otherwise.
	 */
	boolean hasCascadePersist();

	/**
	 * Determine whether this entity has any
	 * {@linkplain org.hibernate.cascade.spi.CascadeStyles#DELETE delete cascading}.
	 *
	 * @return True if the entity has any properties with a cascade REMOVE or ALL;
	 *         false otherwise.
	 */
	boolean hasCascadeDelete();

	/**
	 * Determine whether this entity has any many-to-one or one-to-one associations.
	 *
	 * @return True if the entity has a many-to-one or one-to-one association;
	 * false otherwise.
	 *
	 * @since 7
	 */
	boolean hasToOnes();

	/**
	 * Determine whether this entity has any owned collections.
	 *
	 * @return True if the entity has an owned collection;
	 * false otherwise.
	 */
	boolean hasOwnedCollections();

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
	@Nonnull
	@Deprecated( since = "6", forRemoval = true )
	Type getPropertyType(@Nonnull String propertyName) throws MappingException;

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
	@Nullable
	int[] findDirty(@Nonnull Object[] currentState, @Nonnull Object[] previousState, @Nonnull Object owner, @Nonnull SharedSessionContractImplementor session);

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
	@Nullable
	int[] findModified(@Nonnull Object[] old, @Nonnull Object[] current, @Nonnull Object object, @Nonnull SharedSessionContractImplementor session);

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
	@Nullable
	BasicType<?> getVersionType();

	@Nullable
	@SuppressWarnings("unchecked")
	default VersionJavaType<Object> getVersionJavaType() {
		final var versionType = getVersionType();
		return versionType == null ? null : (VersionJavaType<Object>) versionType.getJavaTypeDescriptor();
	}

	/**
	 * If {@link #isVersioned()}, then what is the index of the property
	 * holding the locking value.
	 *
	 * @return The type of the version property; or -66, if not versioned.
	 */
	int getVersionPropertyIndex();

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
	@Nullable
	int[] getNaturalIdentifierProperties();

	/**
	 * Retrieve the current state of the natural-id properties from the database.
	 *
	 * @param id The identifier of the entity for which to retrieve the natural-id values.
	 * @param session The session from which the request originated.
	 * @return The natural-id snapshot.
	 */
	@Nullable
	Object getNaturalIdentifierSnapshot(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Determine which identifier generation strategy is used for this entity.
	 *
	 * @return The identifier generation strategy.
	 */
	@Nonnull
	Generator getGenerator();

	@Nullable
	default BeforeExecutionGenerator getVersionGenerator() {
		return new VersionGeneration( getVersionMapping() );
	}

	@Nonnull
	@Override
	default AttributeMapping getAttributeMapping(int position) {
		return getAttributeMappings().get( position );
	}

	@Override
	default <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		int span = 0;
		if ( domainValue instanceof Object[] values ) {
			for ( int i = 0; i < getNumberOfAttributeMappings(); i++ ) {
				final AttributeMapping attributeMapping = getAttributeMapping( i );
				span += attributeMapping.breakDownJdbcValues( values[ i ], offset + span, x, y, valueConsumer, session );
			}
		}
		else {
			for ( int i = 0; i < getNumberOfAttributeMappings(); i++ ) {
				final AttributeMapping attributeMapping = getAttributeMapping( i );
				final Object attributeValue = domainValue == null ? null : attributeMapping.getValue( domainValue );
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

	@Nonnull
	default NaturalIdLoader<?> getNaturalIdLoader() {
		throw new UnsupportedOperationException(
				"EntityPersister implementation '" + getClass().getName()
						+ "' does not support 'NaturalIdLoader'"
		);
	}

	@Nonnull
	default MultiNaturalIdLoader<?> getMultiNaturalIdLoader() {
		throw new UnsupportedOperationException(
				"EntityPersister implementation '" + getClass().getName()
						+ "' does not support 'MultiNaturalIdLoader'"
		);
	}

	/**
	 * Load an instance of the persistent class.
	 */
	@Nullable
	Object load(@Nonnull Object id, @Nullable Object optionalObject, @Nonnull LockMode lockMode, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Load an instance of the persistent class.
	 */
	@Nullable
	Object load(@Nonnull Object id, @Nullable Object optionalObject, @Nonnull LockOptions lockOptions, @Nonnull SharedSessionContractImplementor session);

	@Nullable
	default Object load(@Nonnull Object id, @Nullable Object optionalObject, @Nonnull LockOptions lockOptions, @Nonnull SharedSessionContractImplementor session, @Nullable Boolean readOnly)
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
	@Nonnull
	List<?> multiLoad(@Nonnull Object[] ids, @Nonnull SharedSessionContractImplementor session, @Nonnull MultiIdLoadOptions loadOptions);

	@Nullable
	@Override
	default Object loadByUniqueKey(@Nonnull String propertyName, @Nonnull Object uniqueKey, @Nonnull SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(
				"EntityPersister implementation '" + getClass().getName()
						+ "' does not support 'UniqueKeyLoadable'"
		);
	}

	/**
	 * Do a version check (optional operation)
	 */
	void lock(@Nonnull Object id, @Nullable Object version, @Nonnull Object object, @Nonnull LockMode lockMode, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Do a version check (optional operation)
	 */
	void lock(@Nonnull Object id, @Nullable Object version, @Nonnull Object object, @Nonnull LockOptions lockOptions, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Persist an instance
	 *
	 * @see #getInsertCoordinator()
	 * @deprecated Use {@link InsertCoordinator#insert(Object, Object, Object[], SharedSessionContractImplementor)} instead.
	 */
	@Deprecated( forRemoval = true, since = "6.5" )
	default void insert(@Nonnull Object id, @Nonnull Object[] fields, @Nonnull Object object, @Nonnull SharedSessionContractImplementor session) {
		getInsertCoordinator().insert( object, id, fields, session );
	}

	/**
	 * Persist an instance
	 *
	 * @see #getInsertCoordinator()
	 * @deprecated Use {@link InsertCoordinator#insert(Object, Object[], SharedSessionContractImplementor)} instead.
	 */
	@Nullable
	@Deprecated( forRemoval = true, since = "6.5" )
	default Object insert(@Nonnull Object[] fields, @Nonnull Object object, @Nonnull SharedSessionContractImplementor session) {
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
	default void delete(@Nonnull Object id, @Nullable Object version, @Nonnull Object object, @Nonnull SharedSessionContractImplementor session) {
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
			@Nonnull Object id,
			@Nonnull Object[] fields,
			@Nullable int[] dirtyFields,
			boolean hasDirtyCollection,
			@Nonnull Object[] oldFields,
			@Nullable Object oldVersion,
			@Nonnull Object object,
			@Nullable Object rowId,
			@Nonnull SharedSessionContractImplementor session) {
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
			@Nonnull Object id,
			@Nonnull Object[] fields,
			@Nullable int[] dirtyFields,
			boolean hasDirtyCollection,
			@Nonnull Object[] oldFields,
			@Nullable Object oldVersion,
			@Nonnull Object object,
			@Nullable Object rowId,
			@Nonnull SharedSessionContractImplementor session) {
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
	@Nonnull
	InsertCoordinator getInsertCoordinator();

	/**
	 * Get the update coordinator instance.
	 *
	 * @since 6.5
	 */
	@Nonnull
	UpdateCoordinator getUpdateCoordinator();

	/**
	 * Get the delete coordinator instance.
	 *
	 * @since 6.5
	 */
	@Nonnull
	DeleteCoordinator getDeleteCoordinator();

	/**
	 * Get the merge coordinator instance.
	 *
	 * @since 6.5
	 */
	@Nonnull
	default UpdateCoordinator getMergeCoordinator() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the Hibernate types of the class properties
	 */
	@Nonnull
	Type[] getPropertyTypes();

	/**
	 * Get the names of the class properties - doesn't have to be the names of the
	 * actual Java properties (used for XML generation only)
	 */
	@Nonnull
	String[] getPropertyNames();

	/**
	 * Get the "insertability" of the properties of this class
	 * (does the property appear in an SQL INSERT)
	 */
	@Nonnull
	boolean[] getPropertyInsertability();

	/**
	 * Get the "updateability" of the properties of this class
	 * (does the property appear in an SQL UPDATE)
	 */
	@Nonnull
	boolean[] getPropertyUpdateability();

	/**
	 * Is the property excluded from temporal versioning.
	 */
	default boolean isPropertyTemporalExcluded(int attributeIndex) {
		return false;
	}

	/**
	 * Is the property excluded from audit logging.
	 */
	default boolean isPropertyAuditedExcluded(int attributeIndex) {
		return false;
	}

	/**
	 * Get the "checkability" of the properties of this class
	 * (is the property dirty checked, does the cache need
	 * to be updated)
	 */
	@Nonnull
	boolean[] getPropertyCheckability();

	/**
	 * Get the nullability of the properties of this class
	 */
	@Nonnull
	boolean[] getPropertyNullability();

	/**
	 * Get the "versionability" of the properties of this class
	 * (is the property optimistic-locked)
	 */
	@Nonnull
	boolean[] getPropertyVersionability();

	@Nonnull
	boolean[] getPropertyLaziness();

	@Nonnull
	boolean[] getNonLazyPropertyUpdateability();

	/**
	 * Get the cascade styles of the properties (optional operation)
	 */
	@Nonnull
	CascadeStyle[] getPropertyCascadeStyles();

	/**
	 * Get the cascade style of the identifier property, or
	 * {@link CascadeStyles#NONE NONE}
	 * if the identifier has no cascading.
	 */
	@Nonnull
	default CascadeStyle getIdentifierCascadeStyle() {
		return CascadeStyles.NONE;
	}

	/**
	 * Get the identifier type
	 */
	@Nonnull
	Type getIdentifierType();

	/**
	 * Get the name of the identifier property (or return null) - need not return the
	 * name of an actual Java property
	 */
	@Nullable
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
	 * Does this class have a cache?
	 *
	 * @deprecated Use {@link #canReadFromCache()} and/or {@link #canWriteToCache()}
	 *             depending on need
	 */
	@Deprecated
	boolean hasCache();

	/**
	 * Get the cache (optional operation)
	 */
	@Nullable
	EntityDataAccess getCacheAccessStrategy();

	/**
	 * Get the cache structure
	 */
	@Nonnull
	CacheEntryStructure getCacheEntryStructure();

	@Nonnull
	CacheEntry buildCacheEntry(@Nonnull Object entity, @Nonnull Object[] state, @Nullable Object version, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Does this class have a natural id cache
	 */
	boolean hasNaturalIdCache();

	/**
	 * Get the NaturalId cache (optional operation)
	 */
	@Nullable
	NaturalIdDataAccess getNaturalIdCacheAccessStrategy();

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
	@Nullable
	Object[] getDatabaseSnapshot(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) throws HibernateException;

	@Nullable
	default TenantIdLoader getTenantIdLoader() {
		return null;
	}

	@Nullable
	Object getIdByUniqueKey(@Nonnull Object key, @Nonnull String uniquePropertyName, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Get the current version of the object, or return null if there is no
	 * row for the given identifier. In the case of unversioned data, return
	 * any object if the row exists.
	 */
	@Nullable
	Object getCurrentVersion(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) throws HibernateException;

	@Nonnull
	Object forceVersionIncrement(@Nonnull Object id, @Nullable Object currentVersion, @Nonnull SharedSessionContractImplementor session) throws HibernateException;

	@Nonnull
	default Object forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			boolean batching,
			@Nonnull SharedSessionContractImplementor session) throws HibernateException {
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
	void afterInitialize(@Nonnull Object entity, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Called just after the entity has been reassociated with the session
	 */
	void afterReassociate(@Nonnull Object entity, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Create a new proxy instance
	 */
	@Nonnull
	Object createProxy(@Nonnull Object id, @Nullable SharedSessionContractImplementor session);

	/**
	 * Is this a new transient instance?
	 */
	@Nullable
	Boolean isTransient(@Nonnull Object object, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Return the values of the insertable properties of the object (including backrefs)
	 */
	@Nonnull
	Object[] getPropertyValuesToInsert(@Nonnull Object object, @Nullable Map<Object,Object> mergeMap, @Nonnull SharedSessionContractImplementor session);

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
	default void processInsertGeneratedProperties(@Nonnull Object id, @Nonnull Object entity, @Nonnull Object[] state, @Nonnull SharedSessionContractImplementor session) {
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
			@Nonnull Object id,
			@Nonnull Object entity,
			@Nonnull Object[] state,
			@Nullable GeneratedValues generatedValues,
			@Nonnull SharedSessionContractImplementor session) {
	}

	@Nonnull
	default List<? extends ModelPart> getGeneratedProperties(@Nonnull EventType timing) {
		return timing == EventType.INSERT ? getInsertGeneratedProperties() : getUpdateGeneratedProperties();
	}

	@Nonnull
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
	default void processUpdateGeneratedProperties(@Nonnull Object id, @Nonnull Object entity, @Nonnull Object[] state, @Nonnull SharedSessionContractImplementor session) {
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
			@Nonnull Object id,
			@Nonnull Object entity,
			@Nonnull Object[] state,
			@Nullable GeneratedValues generatedValues,
			@Nonnull SharedSessionContractImplementor session);

	@Nonnull
	default List<? extends ModelPart> getUpdateGeneratedProperties() {
		return Collections.emptyList();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is Tuplizer-centric ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The persistent class, or null
	 */
	@Nullable
	Class<?> getMappedClass();

	/**
	 * Get the proxy interface that instances of <em>this</em> concrete class will be
	 * cast to (optional operation).
	 */
	@Nullable
	Class<?> getConcreteProxyClass();

	default void setValues(@Nonnull Object object, @Nonnull Object[] values) {
		setPropertyValues( object, values );
	}

	/**
	 * Set the given values to the mapped properties of the given object.
	 *
	 * @deprecated Use {@link #setValues} instead
	 */
	@Deprecated(since = "6.0")
	void setPropertyValues(@Nonnull Object object, @Nonnull Object[] values);

	default void setValue(@Nonnull Object object, int i, @Nullable Object value) {
		setPropertyValue( object, i, value );
	}

	/**
	 * Set the value of a particular property of the given instance.
	 *
	 * @deprecated Use {@link #setValue} instead
	 */
	@Deprecated(since = "6.0")
	void setPropertyValue(@Nonnull Object object, int i, @Nullable Object value);

	@Nonnull
	default Object[] getValues(@Nonnull Object object) {
		return getPropertyValues( object );
	}

	/**
	 * @deprecated Use {@link #getValues} instead
	 */
	@Nonnull
	@Deprecated(since  = "6.0")
	Object[] getPropertyValues(@Nonnull Object object);

	@Nullable
	default Object getValue(@Nonnull Object object, int i) {
		return getPropertyValue( object, i );
	}

	/**
	 * @deprecated Use {@link #getValue} instead
	 */
	@Nullable
	@Deprecated(since = "6.0")
	Object getPropertyValue(@Nonnull Object object, int i) throws HibernateException;

	/**
	 * Get the value of a particular property
	 */
	@Nullable
	Object getPropertyValue(@Nonnull Object object, @Nonnull String propertyName);

	/**
	 * Get the identifier of an instance from the object's identifier property.
	 * Throw an exception if it has no identifier property.
	 */
	@Nullable
	Object getIdentifier(@Nonnull Object entity, @Nullable SharedSessionContractImplementor session);

	/**
	 * Get the identifier of an instance from the object's identifier property.
	 * Throw an exception if it has no identifier property.
	 *
	 * It's supposed to be use during the merging process
	 */
	@Nullable
	default Object getIdentifier(@Nonnull Object entity, @Nullable MergeContext mergeContext) {
		return getIdentifier( entity, mergeContext == null ? null : mergeContext.getEventSource() );
	}

	/**
	 * Get the identifier of an instance from the object's identifier property.
	 * Throw an exception if it has no identifier property.
	 */
	@Nullable
	default Object getIdentifier(@Nonnull Object entity) {
		return getIdentifier( entity, (SharedSessionContractImplementor) null );
	}

	/**
	 * Inject the identifier value into the given entity.
	 */
	void setIdentifier(@Nonnull Object entity, @Nullable Object id, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Get the version number (or timestamp) from the object's version property.
	 * Return {@code null} if it is not versioned.
	 */
	@Nullable
	Object getVersion(@Nonnull Object object) throws HibernateException;

	/**
	 * Create a class instance initialized with the given identifier
	 *
	 * @param id The identifier value to use (may be null to represent no value)
	 * @param session The session from which the request originated.
	 *
	 * @return The instantiated entity.
	 */
	@Nonnull
	Object instantiate(@Nullable Object id, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Is the given object an instance of this entity?
	 */
	boolean isInstance(@Nonnull Object object);

	/**
	 * Does the given instance have any uninitialized lazy properties?
	 */
	boolean hasUninitializedLazyProperties(@Nonnull Object object);

	/**
	 * Set the identifier and version of the given instance back to its "unsaved"
	 * value, that is, the value it had before it was made persistent.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_IDENTIFIER_ROLLBACK
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#isIdentifierRollbackEnabled
	 */
	void resetIdentifier(@Nonnull Object entity, @Nonnull Object currentId, @Nullable Object currentVersion, @Nonnull SharedSessionContractImplementor session);

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
	@Nonnull
	EntityPersister getSubclassEntityPersister(@Nullable Object instance, @Nonnull SessionFactoryImplementor factory);

	@Nonnull
	EntityRepresentationStrategy getRepresentationStrategy();

	@Nonnull
	@Override
	default EntityPersister getEntityMappingType() {
		return this;
	}

	@Override
	default void addToCacheKey(
			@Nonnull MutableCacheKeyBuilder cacheKey,
			@Nullable Object value,
			@Nullable SharedSessionContractImplementor session) {
		getIdentifierMapping().addToCacheKey( cacheKey, value == null ? null : getIdentifier( value, session ), session );
	}

	@Nonnull
	BytecodeEnhancementMetadata getBytecodeEnhancementMetadata();

	@Nonnull
	FilterAliasGenerator getFilterAliasGenerator(@Nonnull final String rootAlias);

	@Nonnull
	default FilterAliasGenerator getFilterAliasGenerator(@Nonnull TableGroup rootTableGroup) {
		return new TableGroupFilterAliasGenerator( getTableName(), rootTableGroup );
	}

	/**
	 * The table to join to.
	 */
	@Nonnull
	String getTableName();

	/**
	 * Converts an array of attribute names to a set of indexes, according to the entity metamodel
	 *
	 * @param attributeNames Array of names to be resolved
	 *
	 * @return A set of unique indexes of the attribute names found in the metamodel
	 */
	@Nonnull
	int[] resolveAttributeIndexes(@Nullable String[] attributeNames);

	/**
	 * Like {@link #resolveAttributeIndexes(String[])} but also always returns mutable attributes
	 *
	 * @param attributeNames Array of names to be resolved
	 *
	 * @return A set of unique indexes of the attribute names found in the metamodel
	 */
	@Nonnull
	default int[] resolveDirtyAttributeIndexes(
			@Nonnull Object[] values,
			@Nonnull Object[] loadedState,
			@Nullable String[] attributeNames,
			@Nonnull SessionImplementor session) {
		return resolveAttributeIndexes( attributeNames );
	}

	boolean canUseReferenceCacheEntries();

	@Incubating(since = "6.5")
	boolean useShallowQueryCacheLayout();

	@Incubating(since = "6.5")
	boolean storeDiscriminatorInShallowQueryCacheLayout();

	boolean hasFilterForLoadByKey();

	/**
	 * @return Metadata for each unique key defined
	 */
	@Nonnull
	@Incubating(since = "6.2")
	Iterable<UniqueKeyEntry> uniqueKeyEntries();

	/**
	 * Get a SQL select string that performs a select based on a unique
	 * key determined by the given property name.
	 *
	 * @param propertyName The name of the property which maps to the
	 *           column(s) to use in the select statement restriction.
	 * @return The SQL select string
	 */
	@Nonnull
	String getSelectByUniqueKeyString(@Nonnull String propertyName);

	/**
	 * Get a SQL select string that performs a select based on a unique
	 * key determined by the given property names.
	 *
	 * @param propertyNames The names of the properties which maps to the
	 *               column(s) to use in the select statement restriction.
	 * @return The SQL select string
	 */
	@Nonnull
	default String getSelectByUniqueKeyString(@Nonnull String[] propertyNames) {
		// default impl only for backward compatibility
		if ( propertyNames.length > 1 ) {
			throw new IllegalArgumentException( "support for multiple properties not implemented" );
		}
		return getSelectByUniqueKeyString( propertyNames[0] );
	}

	@Nonnull
	String getSelectByUniqueKeyString(@Nonnull String[] propertyNames, @Nonnull String[] columnNames);


	/**
	 * The names of the primary key columns in the root table.
	 *
	 * @return The primary key column names.
	 */
	@Nonnull
	String[] getRootTableKeyColumnNames();

	/**
	 * Get the database-specific SQL command to retrieve the last
	 * generated IDENTITY value.
	 *
	 * @return The SQL command string
	 */
	@Nullable
	String getIdentitySelectString();

	/**
	 * Get the names of columns used to persist the identifier
	 */
	@Nonnull
	String[] getIdentifierColumnNames();

	/**
	 * Get the result set aliases used for the identifier columns, given a suffix
	 */
	@Nonnull
	String[] getIdentifierAliases(@Nonnull String suffix);

	/**
	 * Locks are always applied to the "root table".
	 *
	 * @return The root table name
	 */
	@Nonnull
	String getRootTableName();

	/**
	 * Get the names of columns on the root table used to persist the identifier.
	 *
	 * @return The root table identifier column names.
	 */
	@Nonnull
	String[] getRootTableIdentifierColumnNames();

	/**
	 * For versioned entities, get the name of the column (again, expected on the
	 * root table) used to store the version values.
	 *
	 * @return The version column name.
	 */
	@Nullable
	String getVersionColumnName();

	/**
	 * Get the result set aliases used for the property columns, given a suffix (properties of this class, only).
	 */
	@Nonnull
	String[] getPropertyAliases(@Nonnull String suffix, int i);

	/**
	 * Get the result set aliases used for the identifier columns, given a suffix
	 */
	@Nullable
	String getDiscriminatorAlias(@Nonnull String suffix);

	boolean hasMultipleTables();

	@Nonnull
	String[] getTableNames();

	/**
	 * @deprecated Only ever used from places where we really want to use<ul>
	 *     <li>{@link SelectStatement} (select generator)</li>
	 *     <li>{@link InsertSelectStatement}</li>
	 *     <li>{@link org.hibernate.sql.ast.spi.query.update.UpdateStatement}</li>
	 *     <li>{@link org.hibernate.sql.ast.spi.query.delete.DeleteStatement}</li>
	 * </ul>
	 */
	@Nonnull
	@Deprecated( since = "6.2" )
	String getTableName(int j);

	@Nonnull
	String[] getKeyColumns(int j);

	int getTableSpan();

	boolean isInverseTable(int j);

	boolean isNullableTable(int j);

	boolean hasDuplicateTables();

	int getSubclassTableSpan();

	@Nonnull
	String getSubclassTableName(int j);

	@Nonnull
	String getTableNameForColumn(@Nonnull String columnName);

	/**
	 * @return the column name for the discriminator as specified in the mapping.
	 *
	 * @deprecated Use {@link EntityDiscriminatorMapping#getSelectionExpression()} instead
	 */
	@Nullable
	@Deprecated
	String getDiscriminatorColumnName();

	/**
	 * Get the discriminator type
	 */
	@Nullable
	Type getDiscriminatorType();

	/**
	 * Does the result set contain rowids?
	 */
	boolean hasRowId();

	/**
	 * The indexes of {@linkplain org.hibernate.annotations.Immutable immutable} attributes of the entity.
	 */
	@Nonnull
	int[] getImmutablePropertyIndexes();

	@Nonnull
	String[] getSubclassPropertyColumnNames(int i);

	/**
	 * Return the column alias names used to persist/query the indexed property of the class or a subclass.
	 */
	@Nonnull
	String[] getSubclassPropertyColumnAliases(int i, @Nonnull String suffix);

	/**
	 * Return the column alias names used to persist/query the named property of the class or a subclass (optional operation).
	 */
	@Nullable
	String[] getSubclassPropertyColumnAliases(@Nonnull String propertyName, @Nonnull String suffix);

	int countSubclassProperties();

	/**
	 * Get the column names for the given property path
	 */
	@Nonnull
	String[] getPropertyColumnNames(@Nonnull String propertyPath);

	/**
	 * All columns to select, when loading.
	 */
	@Nonnull
	String selectFragment(@Nonnull String alias, @Nonnull String suffix);

	/**
	 * The type of the discriminator, or {@code null} if the entity does not have a discriminator.
	 *
	 * @return a {@link DiscriminatorType} or {@code null}
	 *
	 * @see #getDiscriminatorType()
	 *
	 * @since 7
	 */
	@Nullable
	DiscriminatorType<?> getDiscriminatorDomainType();

	/**
	 * Given a property path, return the corresponding column name(s).
	 *
	 * @deprecated No longer used in ORM core
	 */
	@Nonnull
	@Deprecated(since = "7.0", forRemoval = true)
	String[] toColumns(@Nonnull String propertyName);

	@Incubating(since = "7.4")
	boolean excludedFromTemporalVersioning(@Nullable int[] dirtyAttributeIndexes, boolean hasDirtyCollection);

	boolean isSharedColumn(@Nonnull String columnExpression);

	@Nonnull
	String[][] getConstraintOrderedTableKeyColumnClosure();

	@Internal
	boolean managesColumns(@Nonnull String[] columnNames);


	@Nonnull
	@Override
	default String getRolePath() {
		return getNavigableRole().getFullPath();
	}
}
