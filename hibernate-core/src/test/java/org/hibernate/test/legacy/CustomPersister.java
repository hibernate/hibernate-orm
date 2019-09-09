/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.MultiLoadOptions;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.tuple.entity.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

public class CustomPersister implements EntityPersister {

	private static final Hashtable INSTANCES = new Hashtable();
	private static final IdentifierGenerator GENERATOR = new UUIDHexGenerator();

	private SessionFactoryImplementor factory;
	private EntityMetamodel entityMetamodel;

	@SuppressWarnings("UnusedParameters")
	public CustomPersister(
			PersistentClass model,
			EntityDataAccess cacheAccessStrategy,
			NaturalIdDataAccess naturalIdRegionAccessStrategy,
			PersisterCreationContext creationContext) {
		this.factory = creationContext.getSessionFactory();
		this.entityMetamodel = new EntityMetamodel( model, this, factory );
	}

	public boolean hasLazyProperties() {
		return false;
	}

	public boolean isInherited() {
		return false;
	}

	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return new NavigableRole( getEntityName() );
	}

	@Override
	public EntityEntryFactory getEntityEntryFactory() {
		return MutableEntityEntryFactory.INSTANCE;
	}

	@Override
	public Class getMappedClass() {
		return Custom.class;
	}

	@Override
	public void generateEntityDefinition() {
	}

	public void postInstantiate() throws MappingException {}

	public String getEntityName() {
		return Custom.class.getName();
	}

	public boolean isSubclassEntityName(String entityName) {
		return Custom.class.getName().equals(entityName);
	}

	public boolean hasProxy() {
		return false;
	}

	public boolean hasCollections() {
		return false;
	}

	public boolean hasCascades() {
		return false;
	}

	public boolean isMutable() {
		return true;
	}

	public boolean isSelectBeforeUpdateRequired() {
		return false;
	}

	public boolean isIdentifierAssignedByInsert() {
		return false;
	}

	public Boolean isTransient(Object object, SharedSessionContractImplementor session) {
		return ( (Custom) object ).id==null;
	}

	@Override
	public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SharedSessionContractImplementor session) {
		return getPropertyValues( object );
	}

	public void processInsertGeneratedProperties(Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session) {
	}

	public void processUpdateGeneratedProperties(Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session) {
	}

	public void retrieveGeneratedProperties(Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean implementsLifecycle() {
		return false;
	}

	@Override
	public Class getConcreteProxyClass() {
		return Custom.class;
	}

	@Override
	public void setPropertyValues(Object object, Object[] values) {
		setPropertyValue( object, 0, values[0] );
	}

	@Override
	public void setPropertyValue(Object object, int i, Object value) {
		( (Custom) object ).setName( (String) value );
	}

	@Override
	public Object[] getPropertyValues(Object object) throws HibernateException {
		Custom c = (Custom) object;
		return new Object[] { c.getName() };
	}

	@Override
	public Object getPropertyValue(Object object, int i) throws HibernateException {
		return ( (Custom) object ).getName();
	}

	@Override
	public Object getPropertyValue(Object object, String propertyName) throws HibernateException {
		return ( (Custom) object ).getName();
	}

	@Override
	public Serializable getIdentifier(Object object) throws HibernateException {
		return ( (Custom) object ).id;
	}

	@Override
	public Serializable getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return ( (Custom) entity ).id;
	}

	@Override
	public void setIdentifier(Object entity, Serializable id, SharedSessionContractImplementor session) {
		( (Custom) entity ).id = (String) id;
	}

	@Override
	public Object getVersion(Object object) throws HibernateException {
		return null;
	}

	@Override
	public Object instantiate(Serializable id, SharedSessionContractImplementor session) {
		Custom c = new Custom();
		c.id = (String) id;
		return c;
	}

	@Override
	public boolean isInstance(Object object) {
		return object instanceof Custom;
	}

	@Override
	public boolean hasUninitializedLazyProperties(Object object) {
		return false;
	}

	@Override
	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SharedSessionContractImplementor session) {
		( ( Custom ) entity ).id = ( String ) currentId;
	}

	public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory) {
		return this;
	}

	public int[] findDirty(
		Object[] x,
		Object[] y,
		Object owner,
		SharedSessionContractImplementor session) throws HibernateException {
		if ( !Objects.equals( x[0], y[0] ) ) {
			return new int[] { 0 };
		}
		else {
			return null;
		}
	}

	public int[] findModified(
		Object[] x,
		Object[] y,
		Object owner,
		SharedSessionContractImplementor session) throws HibernateException {
		if ( !Objects.equals( x[0], y[0] ) ) {
			return new int[] { 0 };
		}
		else {
			return null;
		}
	}

	/**
	 * @see EntityPersister#hasIdentifierProperty()
	 */
	public boolean hasIdentifierProperty() {
		return true;
	}

	/**
	 * @see EntityPersister#isVersioned()
	 */
	public boolean isVersioned() {
		return false;
	}

	/**
	 * @see EntityPersister#getVersionType()
	 */
	public VersionType getVersionType() {
		return null;
	}

	/**
	 * @see EntityPersister#getVersionProperty()
	 */
	public int getVersionProperty() {
		return 0;
	}

	/**
	 * @see EntityPersister#getIdentifierGenerator()
	 */
	public IdentifierGenerator getIdentifierGenerator()
	throws HibernateException {
		return GENERATOR;
	}

	/**
	 * @see EntityPersister#load(Serializable, Object, org.hibernate.LockOptions , SharedSessionContractImplementor)
	 */
	public Object load(
		Serializable id,
		Object optionalObject,
		LockOptions lockOptions,
		SharedSessionContractImplementor session
	) throws HibernateException {
		return load(id, optionalObject, lockOptions.getLockMode(), session);
	}

	@Override
	public List multiLoad(Serializable[] ids, SharedSessionContractImplementor session, MultiLoadOptions loadOptions) {
		return Collections.emptyList();
	}

	/**
	 * @see EntityPersister#load(Serializable, Object, LockMode, SharedSessionContractImplementor)
	 */
	public Object load(
		Serializable id,
		Object optionalObject,
		LockMode lockMode,
		SharedSessionContractImplementor session
	) throws HibernateException {

		// fails when optional object is supplied

		Custom clone = null;
		Custom obj = (Custom) INSTANCES.get(id);
		if (obj!=null) {
			clone = (Custom) obj.clone();
			TwoPhaseLoad.addUninitializedEntity(
					session.generateEntityKey( id, this ),
					clone,
					this,
					LockMode.NONE,
					session
			);
			TwoPhaseLoad.postHydrate(
					this, id,
					new String[] { obj.getName() },
					null,
					clone,
					LockMode.NONE,
					session
			);
			TwoPhaseLoad.initializeEntity(
					clone,
					false,
					session,
					new PreLoadEvent( (EventSource) session )
			);
			TwoPhaseLoad.afterInitialize( clone, session );
			TwoPhaseLoad.postLoad( clone, session, new PostLoadEvent( (EventSource) session ) );
		}
		return clone;
	}

	/**
	 * @see EntityPersister#lock(Serializable, Object, Object, LockMode, SharedSessionContractImplementor)
	 */
	public void lock(
		Serializable id,
		Object version,
		Object object,
		LockOptions lockOptions,
		SharedSessionContractImplementor session
	) throws HibernateException {

		throw new UnsupportedOperationException();
	}

	/**
	 * @see EntityPersister#lock(Serializable, Object, Object, LockMode, SharedSessionContractImplementor)
	 */
	public void lock(
		Serializable id,
		Object version,
		Object object,
		LockMode lockMode,
		SharedSessionContractImplementor session
	) throws HibernateException {

		throw new UnsupportedOperationException();
	}

	public void insert(
		Serializable id,
		Object[] fields,
		Object object,
		SharedSessionContractImplementor session
	) throws HibernateException {

		INSTANCES.put(id, ( (Custom) object ).clone() );
	}

	public Serializable insert(Object[] fields, Object object, SharedSessionContractImplementor session)
	throws HibernateException {

		throw new UnsupportedOperationException();
	}

	public void delete(
		Serializable id,
		Object version,
		Object object,
		SharedSessionContractImplementor session
	) throws HibernateException {

		INSTANCES.remove(id);
	}

	/**
	 * @see EntityPersister
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
		SharedSessionContractImplementor session
	) throws HibernateException {

		INSTANCES.put( id, ( (Custom) object ).clone() );

	}

	private static final Type[] TYPES = new Type[] { StandardBasicTypes.STRING };
	private static final String[] NAMES = new String[] { "name" };
	private static final boolean[] MUTABILITY = new boolean[] { true };
	private static final boolean[] GENERATION = new boolean[] { false };

	/**
	 * @see EntityPersister#getPropertyTypes()
	 */
	public Type[] getPropertyTypes() {
		return TYPES;
	}

	/**
	 * @see EntityPersister#getPropertyNames()
	 */
	public String[] getPropertyNames() {
		return NAMES;
	}

	/**
	 * @see EntityPersister#getPropertyCascadeStyles()
	 */
	public CascadeStyle[] getPropertyCascadeStyles() {
		return null;
	}

	/**
	 * @see EntityPersister#getIdentifierType()
	 */
	public Type getIdentifierType() {
		return StandardBasicTypes.STRING;
	}

	/**
	 * @see EntityPersister#getIdentifierPropertyName()
	 */
	public String getIdentifierPropertyName() {
		return "id";
	}

	public boolean hasCache() {
		return false;
	}

	public EntityDataAccess getCacheAccessStrategy() {
		return null;
	}

	public boolean hasNaturalIdCache() {
		return false;
	}

	public NaturalIdDataAccess getNaturalIdCacheAccessStrategy() {
		return null;
	}

	public String getRootEntityName() {
		return "CUSTOMS";
	}

	public Serializable[] getPropertySpaces() {
		return new String[] { "CUSTOMS" };
	}

	public Serializable[] getQuerySpaces() {
		return new String[] { "CUSTOMS" };
	}

	/**
	 * @see EntityPersister#getClassMetadata()
	 */
	public ClassMetadata getClassMetadata() {
		return null;
	}

	public boolean[] getPropertyUpdateability() {
		return MUTABILITY;
	}

	public boolean[] getPropertyCheckability() {
		return MUTABILITY;
	}

	/**
	 * @see EntityPersister#getPropertyInsertability()
	 */
	public boolean[] getPropertyInsertability() {
		return MUTABILITY;
	}

	public ValueInclusion[] getPropertyInsertGenerationInclusions() {
		return new ValueInclusion[0];
	}

	public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
		return new ValueInclusion[0];
	}


	public boolean canExtractIdOutOfEntity() {
		return true;
	}

	public boolean isBatchLoadable() {
		return false;
	}

	public Type getPropertyType(String propertyName) {
		throw new UnsupportedOperationException();
	}

	public Object createProxy(Serializable id, SharedSessionContractImplementor session)
		throws HibernateException {
		throw new UnsupportedOperationException("no proxy for this class");
	}

	public Object getCurrentVersion(
		Serializable id,
		SharedSessionContractImplementor session)
		throws HibernateException {

		return INSTANCES.get(id);
	}

	@Override
	public Object forceVersionIncrement(Serializable id, Object currentVersion, SharedSessionContractImplementor session)
			throws HibernateException {
		return null;
	}

	@Override
	public boolean[] getPropertyNullability() {
		return MUTABILITY;
	}

	@Override
	public boolean isCacheInvalidationRequired() {
		return false;
	}

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
	}

	@Override
	public void afterReassociate(Object entity, SharedSessionContractImplementor session) {
	}

	@Override
	public Object[] getDatabaseSnapshot(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
		return null;
	}

	@Override
	public Serializable getIdByUniqueKey(Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "not supported" );
	}

	@Override
	public boolean[] getPropertyVersionability() {
		return MUTABILITY;
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		return UnstructuredCacheEntry.INSTANCE;
	}

	@Override
	public CacheEntry buildCacheEntry(
			Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
		return new StandardCacheEntryImpl(
				state,
				this,
				version,
				session,
				entity
		);
	}

	@Override
	public boolean hasSubselectLoadableCollections() {
		return false;
	}

	@Override
	public int[] getNaturalIdentifierProperties() {
		return null;
	}

	@Override
	public boolean hasNaturalIdentifier() {
		return false;
	}

	@Override
	public boolean hasMutableProperties() {
		return false;
	}

	@Override
	public boolean isInstrumented() {
		return false;
	}

	@Override
	public boolean hasInsertGeneratedProperties() {
		return false;
	}

	@Override
	public boolean hasUpdateGeneratedProperties() {
		return false;
	}

	@Override
	public boolean[] getPropertyLaziness() {
		return null;
	}

	@Override
	public boolean isLazyPropertiesCacheable() {
		return true;
	}

	@Override
	public boolean canReadFromCache() {
		return false;
	}

	@Override
	public boolean canWriteToCache() {
		return false;
	}

	@Override
	public boolean isVersionPropertyGenerated() {
		return false;
	}

	@Override
	public Object[] getNaturalIdentifierSnapshot(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
		return null;
	}

	@Override
	public Serializable loadEntityIdByNaturalId(Object[] naturalIdValues, LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		return null;
	}

	public Comparator getVersionComparator() {
		return null;
	}

	@Override
	public EntityMetamodel getEntityMetamodel() {
		return entityMetamodel;
	}

	@Override
	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	@Override
	public EntityTuplizer getEntityTuplizer() {
		return null;
	}

	@Override
	public BytecodeEnhancementMetadata getInstrumentationMetadata() {
		return new BytecodeEnhancementMetadataNonPojoImpl( getEntityName() );
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator(rootAlias);
	}

	@Override
	public EntityPersister getEntityPersister() {
		return this;
	}

	@Override
	public EntityIdentifierDefinition getEntityKeyDefinition() {
		throw new NotYetImplementedException();
	}

	@Override
	public Iterable<AttributeDefinition> getAttributes() {
		throw new NotYetImplementedException();
	}

    @Override
    public int[] resolveAttributeIndexes(String[] attributeNames) {
        return null;
    }

	@Override
	public boolean canUseReferenceCacheEntries() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
