/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.bytecode.spi.EntityInstrumentationMetadata;
import org.hibernate.cache.spi.OptimisticCacheSource;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * Implementors define mapping and persistence logic for a particular
 * strategy of entity mapping.  An instance of entity persisters corresponds
 * to a given mapped entity.
 * <p/>
 * Implementors must be threadsafe (preferrably immutable) and must provide a constructor
 * matching the signature of: {@link org.hibernate.mapping.PersistentClass}, {@link org.hibernate.engine.spi.SessionFactoryImplementor}
 *
 * @author Gavin King
 */
public interface EntityPersister extends OptimisticCacheSource {

	/**
	 * The property name of the "special" identifier property in HQL
	 */
	public static final String ENTITY_ID = "id";

	/**
	 * Finish the initialization of this object.
	 * <p/>
	 * Called only once per {@link org.hibernate.SessionFactory} lifecycle,
	 * after all entity persisters have been instantiated.
	 *
	 * @throws org.hibernate.MappingException Indicates an issue in the metadata.
	 */
	public void postInstantiate() throws MappingException;

	/**
	 * Return the SessionFactory to which this persister "belongs".
	 *
	 * @return The owning SessionFactory.
	 */
	public SessionFactoryImplementor getFactory();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // stuff that is persister-centric and/or EntityInfo-centric ~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns an object that identifies the space in which identifiers of
	 * this entity hierarchy are unique.  Might be a table name, a JNDI URL, etc.
	 *
	 * @return The root entity name.
	 */
	public String getRootEntityName();

	/**
	 * The entity name which this persister maps.
	 *
	 * @return The name of the entity which this persister maps.
	 */
	public String getEntityName();

	/**
	 * Retrieve the underlying entity metamodel instance...
	 *
	 *@return The metamodel
	 */
	public EntityMetamodel getEntityMetamodel();

	/**
	 * Determine whether the given name represents a subclass entity
	 * (or this entity itself) of the entity mapped by this persister.
	 *
	 * @param entityName The entity name to be checked.
	 * @return True if the given entity name represents either the entity
	 * mapped by this persister or one of its subclass entities; false
	 * otherwise.
	 */
	public boolean isSubclassEntityName(String entityName);

	/**
	 * Returns an array of objects that identify spaces in which properties of
	 * this entity are persisted, for instances of this class only.
	 * <p/>
	 * For most implementations, this returns the complete set of table names
	 * to which instances of the mapped entity are persisted (not accounting
	 * for superclass entity mappings).
	 *
	 * @return The property spaces.
	 */
	public Serializable[] getPropertySpaces();

	/**
	 * Returns an array of objects that identify spaces in which properties of
	 * this entity are persisted, for instances of this class and its subclasses.
	 * <p/>
	 * Much like {@link #getPropertySpaces()}, except that here we include subclass
	 * entity spaces.
	 *
	 * @return The query spaces.
	 */
	public Serializable[] getQuerySpaces();

	/**
	 * Determine whether this entity supports dynamic proxies.
	 *
	 * @return True if the entity has dynamic proxy support; false otherwise.
	 */
	public boolean hasProxy();

	/**
	 * Determine whether this entity contains references to persistent collections.
	 *
	 * @return True if the entity does contain persistent collections; false otherwise.
	 */
	public boolean hasCollections();

	/**
	 * Determine whether any properties of this entity are considered mutable.
	 *
	 * @return True if any properties of the entity are mutable; false otherwise (meaning none are).
	 */
	public boolean hasMutableProperties();

	/**
	 * Determine whether this entity contains references to persistent collections
	 * which are fetchable by subselect?
	 *
	 * @return True if the entity contains collections fetchable by subselect; false otherwise.
	 */
	public boolean hasSubselectLoadableCollections();

	/**
	 * Determine whether this entity has any non-none cascading.
	 *
	 * @return True if the entity has any properties with a cascade other than NONE;
	 * false otherwise (aka, no cascading).
	 */
	public boolean hasCascades();

	/**
	 * Determine whether instances of this entity are considered mutable.
	 *
	 * @return True if the entity is considered mutable; false otherwise.
	 */
	public boolean isMutable();

	/**
	 * Determine whether the entity is inherited one or more other entities.
	 * In other words, is this entity a subclass of other entities.
	 *
	 * @return True if other entities extend this entity; false otherwise.
	 */
	public boolean isInherited();

	/**
	 * Are identifiers of this entity assigned known before the insert execution?
	 * Or, are they generated (in the database) by the insert execution.
	 *
	 * @return True if identifiers for this entity are generated by the insert
	 * execution.
	 */
	public boolean isIdentifierAssignedByInsert();

	/**
	 * Get the type of a particular property by name.
	 *
	 * @param propertyName The name of the property for which to retrieve
	 * the type.
	 * @return The type.
	 * @throws org.hibernate.MappingException Typically indicates an unknown
	 * property name.
	 */
	public Type getPropertyType(String propertyName) throws MappingException;

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
	public int[] findDirty(Object[] currentState, Object[] previousState, Object owner, SessionImplementor session);

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
	public int[] findModified(Object[] old, Object[] current, Object object, SessionImplementor session);

	/**
	 * Determine whether the entity has a particular property holding
	 * the identifier value.
	 *
	 * @return True if the entity has a specific property holding identifier value.
	 */
	public boolean hasIdentifierProperty();

	/**
	 * Determine whether detached instances of this entity carry their own
	 * identifier value.
	 * <p/>
	 * The other option is the deprecated feature where users could supply
	 * the id during session calls.
	 *
	 * @return True if either (1) {@link #hasIdentifierProperty()} or
	 * (2) the identifier is an embedded composite identifier; false otherwise.
	 */
	public boolean canExtractIdOutOfEntity();

	/**
	 * Determine whether optimistic locking by column is enabled for this
	 * entity.
	 *
	 * @return True if optimistic locking by column (i.e., <version/> or
	 * <timestamp/>) is enabled; false otherwise.
	 */
	public boolean isVersioned();

	/**
	 * If {@link #isVersioned()}, then what is the type of the property
	 * holding the locking value.
	 *
	 * @return The type of the version property; or null, if not versioned.
	 */
	public VersionType getVersionType();

	/**
	 * If {@link #isVersioned()}, then what is the index of the property
	 * holding the locking value.
	 *
	 * @return The type of the version property; or -66, if not versioned.
	 */
	public int getVersionProperty();

	/**
	 * Determine whether this entity defines a natural identifier.
	 *
	 * @return True if the entity defines a natural id; false otherwise.
	 */
	public boolean hasNaturalIdentifier();

	/**
	 * If the entity defines a natural id ({@link #hasNaturalIdentifier()}), which
	 * properties make up the natural id.
	 *
	 * @return The indices of the properties making of the natural id; or
	 * null, if no natural id is defined.
	 */
	public int[] getNaturalIdentifierProperties();

	/**
	 * Retrieve the current state of the natural-id properties from the database.
	 *
	 * @param id The identifier of the entity for which to retrieve the natural-id values.
	 * @param session The session from which the request originated.
	 * @return The natural-id snapshot.
	 */
	public Object[] getNaturalIdentifierSnapshot(Serializable id, SessionImplementor session);

	/**
	 * Determine which identifier generation strategy is used for this entity.
	 *
	 * @return The identifier generation strategy.
	 */
	public IdentifierGenerator getIdentifierGenerator();

	/**
	 * Determine whether this entity defines any lazy properties (ala
	 * bytecode instrumentation).
	 *
	 * @return True if the entity has properties mapped as lazy; false otherwise.
	 */
	public boolean hasLazyProperties();

	/**
	 * Load the id for the entity based on the natural id.
	 */
	public Serializable loadEntityIdByNaturalId(Object[] naturalIdValues, LockOptions lockOptions,
			SessionImplementor session);

	/**
	 * Load an instance of the persistent class.
	 */
	public Object load(Serializable id, Object optionalObject, LockMode lockMode, SessionImplementor session)
	throws HibernateException;

	/**
	 * Load an instance of the persistent class.
	 */
	public Object load(Serializable id, Object optionalObject, LockOptions lockOptions, SessionImplementor session)
	throws HibernateException;

	/**
	 * Do a version check (optional operation)
	 */
	public void lock(Serializable id, Object version, Object object, LockMode lockMode, SessionImplementor session)
	throws HibernateException;

	/**
	 * Do a version check (optional operation)
	 */
	public void lock(Serializable id, Object version, Object object, LockOptions lockOptions, SessionImplementor session)
	throws HibernateException;

	/**
	 * Persist an instance
	 */
	public void insert(Serializable id, Object[] fields, Object object, SessionImplementor session)
	throws HibernateException;

	/**
	 * Persist an instance, using a natively generated identifier (optional operation)
	 */
	public Serializable insert(Object[] fields, Object object, SessionImplementor session)
	throws HibernateException;

	/**
	 * Delete a persistent instance
	 */
	public void delete(Serializable id, Object version, Object object, SessionImplementor session)
	throws HibernateException;

	/**
	 * Update a persistent instance
	 */
	public void update(
		Serializable id,
		Object[] fields,
		int[] dirtyFields,
		boolean hasDirtyCollection,
		Object[] oldFields,
		Object oldVersion,
		Object object,
		Object rowId,
		SessionImplementor session
	) throws HibernateException;

	/**
	 * Get the Hibernate types of the class properties
	 */
	public Type[] getPropertyTypes();

	/**
	 * Get the names of the class properties - doesn't have to be the names of the
	 * actual Java properties (used for XML generation only)
	 */
	public String[] getPropertyNames();

	/**
	 * Get the "insertability" of the properties of this class
	 * (does the property appear in an SQL INSERT)
	 */
	public boolean[] getPropertyInsertability();

	/**
	 * Which of the properties of this class are database generated values on insert?
	 */
	public ValueInclusion[] getPropertyInsertGenerationInclusions();

	/**
	 * Which of the properties of this class are database generated values on update?
	 */
	public ValueInclusion[] getPropertyUpdateGenerationInclusions();

	/**
	 * Get the "updateability" of the properties of this class
	 * (does the property appear in an SQL UPDATE)
	 */
	public boolean[] getPropertyUpdateability();

	/**
	 * Get the "checkability" of the properties of this class
	 * (is the property dirty checked, does the cache need
	 * to be updated)
	 */
	public boolean[] getPropertyCheckability();

	/**
	 * Get the nullability of the properties of this class
	 */
	public boolean[] getPropertyNullability();

	/**
	 * Get the "versionability" of the properties of this class
	 * (is the property optimistic-locked)
	 */
	public boolean[] getPropertyVersionability();
	public boolean[] getPropertyLaziness();
	/**
	 * Get the cascade styles of the properties (optional operation)
	 */
	public CascadeStyle[] getPropertyCascadeStyles();

	/**
	 * Get the identifier type
	 */
	public Type getIdentifierType();

	/**
	 * Get the name of the identifier property (or return null) - need not return the
	 * name of an actual Java property
	 */
	public String getIdentifierPropertyName();

	/**
	 * Should we always invalidate the cache instead of
	 * recaching updated state
	 */
	public boolean isCacheInvalidationRequired();
	/**
	 * Should lazy properties of this entity be cached?
	 */
	public boolean isLazyPropertiesCacheable();
	/**
	 * Does this class have a cache.
	 */
	public boolean hasCache();
	/**
	 * Get the cache (optional operation)
	 */
	public EntityRegionAccessStrategy getCacheAccessStrategy();
	/**
	 * Get the cache structure
	 */
	public CacheEntryStructure getCacheEntryStructure();
	
	/**
	 * Does this class have a natural id cache
	 */
	public boolean hasNaturalIdCache();
	
	/**
	 * Get the NaturalId cache (optional operation)
	 */
	public NaturalIdRegionAccessStrategy getNaturalIdCacheAccessStrategy();

	/**
	 * Get the user-visible metadata for the class (optional operation)
	 */
	public ClassMetadata getClassMetadata();

	/**
	 * Is batch loading enabled?
	 */
	public boolean isBatchLoadable();

	/**
	 * Is select snapshot before update enabled?
	 */
	public boolean isSelectBeforeUpdateRequired();

	/**
	 * Get the current database state of the object, in a "hydrated" form, without
	 * resolving identifiers
	 * @return null if there is no row in the database
	 */
	public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session)
	throws HibernateException;

	public Serializable getIdByUniqueKey(Serializable key, String uniquePropertyName, SessionImplementor session);

	/**
	 * Get the current version of the object, or return null if there is no row for
	 * the given identifier. In the case of unversioned data, return any object
	 * if the row exists.
	 */
	public Object getCurrentVersion(Serializable id, SessionImplementor session)
	throws HibernateException;

	public Object forceVersionIncrement(Serializable id, Object currentVersion, SessionImplementor session)
	throws HibernateException;

	/**
	 * Has the class actually been bytecode instrumented?
	 */
	public boolean isInstrumented();

	/**
	 * Does this entity define any properties as being database generated on insert?
	 *
	 * @return True if this entity contains at least one property defined
	 * as generated (including version property, but not identifier).
	 */
	public boolean hasInsertGeneratedProperties();

	/**
	 * Does this entity define any properties as being database generated on update?
	 *
	 * @return True if this entity contains at least one property defined
	 * as generated (including version property, but not identifier).
	 */
	public boolean hasUpdateGeneratedProperties();

	/**
	 * Does this entity contain a version property that is defined
	 * to be database generated?
	 *
	 * @return true if this entity contains a version property and that
	 * property has been marked as generated.
	 */
	public boolean isVersionPropertyGenerated();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is tuplizer-centric, but is passed a session ~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Called just after the entities properties have been initialized
	 */
	public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session);

	/**
	 * Called just after the entity has been reassociated with the session
	 */
	public void afterReassociate(Object entity, SessionImplementor session);

	/**
	 * Create a new proxy instance
	 */
	public Object createProxy(Serializable id, SessionImplementor session)
	throws HibernateException;

	/**
	 * Is this a new transient instance?
	 */
	public Boolean isTransient(Object object, SessionImplementor session) throws HibernateException;

	/**
	 * Return the values of the insertable properties of the object (including backrefs)
	 */
	public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SessionImplementor session) throws HibernateException;

	/**
	 * Perform a select to retrieve the values of any generated properties
	 * back from the database, injecting these generated values into the
	 * given entity as well as writing this state to the
	 * {@link org.hibernate.engine.spi.PersistenceContext}.
	 * <p/>
	 * Note, that because we update the PersistenceContext here, callers
	 * need to take care that they have already written the initial snapshot
	 * to the PersistenceContext before calling this method.
	 *
	 * @param id The entity's id value.
	 * @param entity The entity for which to get the state.
	 * @param state
	 * @param session The session
	 */
	public void processInsertGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session);
	/**
	 * Perform a select to retrieve the values of any generated properties
	 * back from the database, injecting these generated values into the
	 * given entity as well as writing this state to the
	 * {@link org.hibernate.engine.spi.PersistenceContext}.
	 * <p/>
	 * Note, that because we update the PersistenceContext here, callers
	 * need to take care that they have already written the initial snapshot
	 * to the PersistenceContext before calling this method.
	 *
	 * @param id The entity's id value.
	 * @param entity The entity for which to get the state.
	 * @param state
	 * @param session The session
	 */
	public void processUpdateGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is Tuplizer-centric ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The persistent class, or null
	 */
	public Class getMappedClass();

	/**
	 * Does the class implement the {@link org.hibernate.classic.Lifecycle} interface.
	 */
	public boolean implementsLifecycle();

	/**
	 * Get the proxy interface that instances of <em>this</em> concrete class will be
	 * cast to (optional operation).
	 */
	public Class getConcreteProxyClass();

	/**
	 * Set the given values to the mapped properties of the given object
	 */
	public void setPropertyValues(Object object, Object[] values);

	/**
	 * Set the value of a particular property
	 */
	public void setPropertyValue(Object object, int i, Object value);

	/**
	 * Return the (loaded) values of the mapped properties of the object (not including backrefs)
	 */
	public Object[] getPropertyValues(Object object);

	/**
	 * Get the value of a particular property
	 */
	public Object getPropertyValue(Object object, int i) throws HibernateException;

	/**
	 * Get the value of a particular property
	 */
	public Object getPropertyValue(Object object, String propertyName);

	/**
	 * Get the identifier of an instance (throw an exception if no identifier property)
	 *
	 * @deprecated Use {@link #getIdentifier(Object,SessionImplementor)} instead
	 */
	@SuppressWarnings( {"JavaDoc"})
	public Serializable getIdentifier(Object object) throws HibernateException;

	/**
	 * Get the identifier of an instance (throw an exception if no identifier property)
	 *
	 * @param entity The entity for which to get the identifier
	 * @param session The session from which the request originated
	 *
	 * @return The identifier
	 */
	public Serializable getIdentifier(Object entity, SessionImplementor session);

    /**
     * Inject the identifier value into the given entity.
     *
     * @param entity The entity to inject with the identifier value.
     * @param id The value to be injected as the identifier.
	 * @param session The session from which is requests originates
     */
	public void setIdentifier(Object entity, Serializable id, SessionImplementor session);

	/**
	 * Get the version number (or timestamp) from the object's version property (or return null if not versioned)
	 */
	public Object getVersion(Object object) throws HibernateException;

	/**
	 * Create a class instance initialized with the given identifier
	 *
	 * @param id The identifier value to use (may be null to represent no value)
	 * @param session The session from which the request originated.
	 *
	 * @return The instantiated entity.
	 */
	public Object instantiate(Serializable id, SessionImplementor session);

	/**
	 * Is the given object an instance of this entity?
	 */
	public boolean isInstance(Object object);

	/**
	 * Does the given instance have any uninitialized lazy properties?
	 */
	public boolean hasUninitializedLazyProperties(Object object);

	/**
	 * Set the identifier and version of the given instance back to its "unsaved" value.
	 *
	 * @param entity The entity instance
	 * @param currentId The currently assigned identifier value.
	 * @param currentVersion The currently assigned version value.
	 * @param session The session from which the request originated.
	 */
	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SessionImplementor session);

	/**
	 * A request has already identified the entity-name of this persister as the mapping for the given instance.
	 * However, we still need to account for possible subclassing and potentially re-route to the more appropriate
	 * persister.
	 * <p/>
	 * For example, a request names <tt>Animal</tt> as the entity-name which gets resolved to this persister.  But the
	 * actual instance is really an instance of <tt>Cat</tt> which is a subclass of <tt>Animal</tt>.  So, here the
	 * <tt>Animal</tt> persister is being asked to return the persister specific to <tt>Cat</tt>.
	 * <p/>
	 * It is also possible that the instance is actually an <tt>Animal</tt> instance in the above example in which
	 * case we would return <tt>this</tt> from this method.
	 *
	 * @param instance The entity instance
	 * @param factory Reference to the SessionFactory
	 *
	 * @return The appropriate persister
	 *
	 * @throws HibernateException Indicates that instance was deemed to not be a subclass of the entity mapped by
	 * this persister.
	 */
	public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory);

	public EntityMode getEntityMode();
	public EntityTuplizer getEntityTuplizer();

	public EntityInstrumentationMetadata getInstrumentationMetadata();
	
	public FilterAliasGenerator getFilterAliasGenerator(final String rootAlias);
}
