//$Id: CustomPersister.java 11398 2007-04-10 14:54:07Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;
import java.util.Comparator;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.LockOptions;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.entry.CacheEntryStructure;
import org.hibernate.cache.entry.UnstructuredCacheEntry;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TwoPhaseLoad;
import org.hibernate.engine.ValueInclusion;
import org.hibernate.event.EventSource;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.QuerySelect;
import org.hibernate.sql.Select;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;
import org.hibernate.util.EqualsHelper;

public class CustomPersister implements EntityPersister {

	private static final Hashtable INSTANCES = new Hashtable();
	private static final IdentifierGenerator GENERATOR = new UUIDHexGenerator();
	
	private SessionFactoryImplementor factory;

	public CustomPersister(
			PersistentClass model, 
			EntityRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory, 
			Mapping mapping) {
		this.factory = factory;
	}

	public boolean hasLazyProperties() {
		return false;
	}

	private void checkEntityMode(EntityMode entityMode) {
		if ( EntityMode.POJO != entityMode ) {
			throw new IllegalArgumentException( "Unhandled EntityMode : " + entityMode );
		}
	}

	private void checkEntityMode(SessionImplementor session) {
		checkEntityMode( session.getEntityMode() );
	}

	public boolean isInherited() {
		return false;
	}
	
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	public Class getMappedClass() {
		return Custom.class;
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
		return new Boolean( ( (Custom) object ).id==null );
	}

	public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SessionImplementor session)
	throws HibernateException {
		return getPropertyValues( object, session.getEntityMode() );
	}

	public void processInsertGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
	}

	public void processUpdateGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
	}

	public void retrieveGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
		throw new UnsupportedOperationException();
	}

	public Class getMappedClass(EntityMode entityMode) {
		checkEntityMode( entityMode );
		return Custom.class;
	}

	public boolean implementsLifecycle(EntityMode entityMode) {
		checkEntityMode( entityMode );
		return false;
	}

	public boolean implementsValidatable(EntityMode entityMode) {
		checkEntityMode( entityMode );
		return false;
	}

	public Class getConcreteProxyClass(EntityMode entityMode) {
		checkEntityMode( entityMode );
		return Custom.class;
	}

	public void setPropertyValues(Object object, Object[] values, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		setPropertyValue( object, 0, values[0], entityMode );
	}

	public void setPropertyValue(Object object, int i, Object value, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		( (Custom) object ).setName( (String) value );
	}

	public Object[] getPropertyValues(Object object, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		Custom c = (Custom) object;
		return new Object[] { c.getName() };
	}

	public Object getPropertyValue(Object object, int i, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		return ( (Custom) object ).getName();
	}

	public Object getPropertyValue(Object object, String propertyName, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		return ( (Custom) object ).getName();
	}

	public Serializable getIdentifier(Object object, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		return ( (Custom) object ).id;
	}

	public Serializable getIdentifier(Object entity, SessionImplementor session) {
		checkEntityMode( session );
		return ( (Custom) entity ).id;
	}

	public void setIdentifier(Object object, Serializable id, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		( (Custom) object ).id = (String) id;
	}

	public void setIdentifier(Object entity, Serializable id, SessionImplementor session) {
		checkEntityMode( session );
		( (Custom) entity ).id = (String) id;
	}

	public Object getVersion(Object object, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		return null;
	}

	public Object instantiate(Serializable id, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		return instantiate( id );
	}

	private Object instantiate(Serializable id) {
		Custom c = new Custom();
		c.id = (String) id;
		return c;
	}

	public Object instantiate(Serializable id, SessionImplementor session) {
		checkEntityMode( session );
		return instantiate( id );
	}

	public boolean isInstance(Object object, EntityMode entityMode) {
		checkEntityMode( entityMode );
		return object instanceof Custom;
	}

	public boolean hasUninitializedLazyProperties(Object object, EntityMode entityMode) {
		checkEntityMode( entityMode );
		return false;
	}

	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, EntityMode entityMode) {
		checkEntityMode( entityMode );
		( ( Custom ) entity ).id = ( String ) currentId;
	}

	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SessionImplementor session) {
		checkEntityMode( session );
		( ( Custom ) entity ).id = ( String ) currentId;
	}

	public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory, EntityMode entityMode) {
		checkEntityMode( entityMode );
		return this;
	}

	public int[] findDirty(
		Object[] x,
		Object[] y,
		Object owner,
		SessionImplementor session
	) throws HibernateException {
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
		SessionImplementor session
	) throws HibernateException {
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
					new EntityKey( id, this, session.getEntityMode() ), 
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
					new PreLoadEvent( (EventSource) session ), 
					new PostLoadEvent( (EventSource) session ) 
				);
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

	private static final Type[] TYPES = new Type[] { Hibernate.STRING };
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
		return Hibernate.STRING;
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

	public Object getPropertyValue(Object object, String propertyName)
		throws HibernateException {
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

	public Object forceVersionIncrement(Serializable id, Object currentVersion, SessionImplementor session)
			throws HibernateException {
		return null;
	}

	public EntityMode guessEntityMode(Object object) {
		if ( !isInstance(object, EntityMode.POJO) ) {
			return null;
		}
		else {
			return EntityMode.POJO;
		}
	}

	public boolean[] getPropertyNullability() {
		return MUTABILITY;
	}

	public boolean isDynamic() {
		return false;
	}

	public boolean isCacheInvalidationRequired() {
		return false;
	}

	public void applyFilters(QuerySelect select, String alias, Map filters) {
	}

	public void applyFilters(Select select, String alias, Map filters) {
	}
	
	
	public void afterInitialize(Object entity, boolean fetched, SessionImplementor session) {
	}

	public void afterReassociate(Object entity, SessionImplementor session) {
	}

	public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session) 
	throws HibernateException {
		return null;
	}
	
	public boolean[] getPropertyVersionability() {
		return MUTABILITY;
	}

	public CacheEntryStructure getCacheEntryStructure() {
		return new UnstructuredCacheEntry();
	}

	public boolean hasSubselectLoadableCollections() {
		return false;
	}

	public int[] getNaturalIdentifierProperties() {
		return null;
	}

	public Type[] getNaturalIdentifierTypes() {
		return null;
	}

	public boolean hasNaturalIdentifier() {
		return false;
	}

	public boolean hasMutableProperties() {
		return false;
	}

	public boolean isInstrumented(EntityMode entityMode) {
		return false;
	}

	public boolean hasInsertGeneratedProperties() {
		return false;
	}

	public boolean hasUpdateGeneratedProperties() {
		return false;
	}

	public boolean[] getPropertyLaziness() {
		return null;
	}

	public boolean isLazyPropertiesCacheable() {
		return true;
	}

	public boolean hasGeneratedProperties() {
		return false;
	}

	public boolean isVersionPropertyGenerated() {
		return false;
	}

	public String[] getOrphanRemovalOneToOnePaths() {
		return null;
	}

	public Object[] getNaturalIdentifierSnapshot(Serializable id, SessionImplementor session) throws HibernateException {
		return null;
	}

	public Comparator getVersionComparator() {
		return null;
	}

	public EntityMetamodel getEntityMetamodel() {
		return null;
	}

}
