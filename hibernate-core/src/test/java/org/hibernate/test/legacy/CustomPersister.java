package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.bytecode.spi.EntityInstrumentationMetadata;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.tuple.entity.NonPojoInstrumentationMetadata;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

public class CustomPersister implements EntityPersister {

	private static final Hashtable INSTANCES = new Hashtable();
	private static final IdentifierGenerator GENERATOR = new UUIDHexGenerator();

	private SessionFactoryImplementor factory;

	public CustomPersister(
			PersistentClass model,
			EntityRegionAccessStrategy cacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			SessionFactoryImplementor factory,
			Mapping mapping) {
		this.factory = factory;
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

	public Boolean isTransient(Object object, SessionImplementor session) {
		return ( (Custom) object ).id==null;
	}

	@Override
	public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SessionImplementor session) {
		return getPropertyValues( object );
	}

	public void processInsertGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
	}

	public void processUpdateGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
	}

	public void retrieveGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
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
	public Serializable getIdentifier(Object entity, SessionImplementor session) {
		return ( (Custom) entity ).id;
	}

	@Override
	public void setIdentifier(Object entity, Serializable id, SessionImplementor session) {
		( (Custom) entity ).id = (String) id;
	}

	@Override
	public Object getVersion(Object object) throws HibernateException {
		return null;
	}

	@Override
	public Object instantiate(Serializable id, SessionImplementor session) {
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
	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SessionImplementor session) {
		( ( Custom ) entity ).id = ( String ) currentId;
	}

	public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory) {
		return this;
	}

	public int[] findDirty(
		Object[] x,
		Object[] y,
		Object owner,
		SessionImplementor session) throws HibernateException {
		if ( !EqualsHelper.equals( x[0], y[0] ) ) {
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
		SessionImplementor session) throws HibernateException {
		if ( !EqualsHelper.equals( x[0], y[0] ) ) {
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
	 * @see EntityPersister#load(Serializable, Object, org.hibernate.LockOptions , SessionImplementor)
	 */
	public Object load(
		Serializable id,
		Object optionalObject,
		LockOptions lockOptions,
		SessionImplementor session
	) throws HibernateException {
		return load(id, optionalObject, lockOptions.getLockMode(), session);
	}

	/**
	 * @see EntityPersister#load(Serializable, Object, LockMode, SessionImplementor)
	 */
	public Object load(
		Serializable id,
		Object optionalObject,
		LockMode lockMode,
		SessionImplementor session
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
					false,
					session
			);
			TwoPhaseLoad.postHydrate(
					this, id,
					new String[] { obj.getName() },
					null,
					clone,
					LockMode.NONE,
					false,
					session
			);
			TwoPhaseLoad.initializeEntity(
					clone,
					false,
					session,
					new PreLoadEvent( (EventSource) session )
			);
			TwoPhaseLoad.postLoad( clone, session, new PostLoadEvent( (EventSource) session ) );
		}
		return clone;
	}

	/**
	 * @see EntityPersister#lock(Serializable, Object, Object, LockMode, SessionImplementor)
	 */
	public void lock(
		Serializable id,
		Object version,
		Object object,
		LockOptions lockOptions,
		SessionImplementor session
	) throws HibernateException {

		throw new UnsupportedOperationException();
	}

	/**
	 * @see EntityPersister#lock(Serializable, Object, Object, LockMode, SessionImplementor)
	 */
	public void lock(
		Serializable id,
		Object version,
		Object object,
		LockMode lockMode,
		SessionImplementor session
	) throws HibernateException {

		throw new UnsupportedOperationException();
	}

	public void insert(
		Serializable id,
		Object[] fields,
		Object object,
		SessionImplementor session
	) throws HibernateException {

		INSTANCES.put(id, ( (Custom) object ).clone() );
	}

	public Serializable insert(Object[] fields, Object object, SessionImplementor session)
	throws HibernateException {

		throw new UnsupportedOperationException();
	}

	public void delete(
		Serializable id,
		Object version,
		Object object,
		SessionImplementor session
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
		SessionImplementor session
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

	public EntityRegionAccessStrategy getCacheAccessStrategy() {
		return null;
	}
	
	public boolean hasNaturalIdCache() {
		return false;
	}

	public NaturalIdRegionAccessStrategy getNaturalIdCacheAccessStrategy() {
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

	public Object createProxy(Serializable id, SessionImplementor session)
		throws HibernateException {
		throw new UnsupportedOperationException("no proxy for this class");
	}

	public Object getCurrentVersion(
		Serializable id,
		SessionImplementor session)
		throws HibernateException {

		return INSTANCES.get(id);
	}

	@Override
	public Object forceVersionIncrement(Serializable id, Object currentVersion, SessionImplementor session)
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
	public void afterInitialize(Object entity, boolean fetched, SessionImplementor session) {
	}

	@Override
	public void afterReassociate(Object entity, SessionImplementor session) {
	}

	@Override
	public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session) throws HibernateException {
		return null;
	}

	@Override
	public Serializable getIdByUniqueKey(Serializable key, String uniquePropertyName, SessionImplementor session) {
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
			Object entity, Object[] state, Object version, SessionImplementor session) {
		return new StandardCacheEntryImpl(
				state,
				this,
				this.hasUninitializedLazyProperties( entity ),
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
	public boolean isVersionPropertyGenerated() {
		return false;
	}

	@Override
	public Object[] getNaturalIdentifierSnapshot(Serializable id, SessionImplementor session) throws HibernateException {
		return null;
	}

	@Override
	public Serializable loadEntityIdByNaturalId(Object[] naturalIdValues, LockOptions lockOptions,
			SessionImplementor session) {
		return null;
	}

	@Override
	public Comparator getVersionComparator() {
		return null;
	}

	@Override
	public EntityMetamodel getEntityMetamodel() {
		return null;
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
	public EntityInstrumentationMetadata getInstrumentationMetadata() {
		return new NonPojoInstrumentationMetadata( getEntityName() );
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
    public int[] resolveAttributeIndexes(Set<String> attributes) {
        return null;
    }

	@Override
	public boolean canUseReferenceCacheEntries() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
