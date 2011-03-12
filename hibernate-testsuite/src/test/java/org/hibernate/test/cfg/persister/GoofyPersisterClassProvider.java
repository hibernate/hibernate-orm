/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.cfg.persister;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.entry.CacheEntryStructure;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.Collection;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.PersisterClassProvider;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class GoofyPersisterClassProvider implements PersisterClassProvider {
	public Class<? extends EntityPersister> getEntityPersisterClass(String entityName) {
		return NoopEntityPersister.class;
	}

	public Class<? extends CollectionPersister> getCollectionPersisterClass(String collectionPersister) {
		return NoopCollectionPersister.class;
	}

	public static class NoopEntityPersister implements EntityPersister {

		public NoopEntityPersister(org.hibernate.mapping.PersistentClass persistentClass,
								   org.hibernate.cache.access.EntityRegionAccessStrategy strategy,
								   org.hibernate.engine.SessionFactoryImplementor sf,
								   org.hibernate.engine.Mapping mapping) {
			throw new GoofyException(NoopEntityPersister.class);
		}

		public void postInstantiate() throws MappingException {

		}

		public SessionFactoryImplementor getFactory() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String getRootEntityName() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String getEntityName() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public EntityMetamodel getEntityMetamodel() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isSubclassEntityName(String entityName) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Serializable[] getPropertySpaces() {
			return new Serializable[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Serializable[] getQuerySpaces() {
			return new Serializable[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasProxy() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasCollections() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasMutableProperties() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasSubselectLoadableCollections() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasCascades() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isMutable() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isInherited() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isIdentifierAssignedByInsert() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Type getPropertyType(String propertyName) throws MappingException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public int[] findDirty(Object[] currentState, Object[] previousState, Object owner, SessionImplementor session) {
			return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public int[] findModified(Object[] old, Object[] current, Object object, SessionImplementor session) {
			return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasIdentifierProperty() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean canExtractIdOutOfEntity() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isVersioned() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Comparator getVersionComparator() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public VersionType getVersionType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public int getVersionProperty() {
			return 0;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasNaturalIdentifier() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public int[] getNaturalIdentifierProperties() {
			return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object[] getNaturalIdentifierSnapshot(Serializable id, SessionImplementor session) {
			return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public IdentifierGenerator getIdentifierGenerator() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasLazyProperties() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object load(Serializable id, Object optionalObject, LockMode lockMode, SessionImplementor session)
				throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object load(Serializable id, Object optionalObject, LockOptions lockOptions, SessionImplementor session)
				throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void lock(Serializable id, Object version, Object object, LockMode lockMode, SessionImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void lock(Serializable id, Object version, Object object, LockOptions lockOptions, SessionImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void insert(Serializable id, Object[] fields, Object object, SessionImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public Serializable insert(Object[] fields, Object object, SessionImplementor session)
				throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void delete(Serializable id, Object version, Object object, SessionImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void update(Serializable id, Object[] fields, int[] dirtyFields, boolean hasDirtyCollection, Object[] oldFields, Object oldVersion, Object object, Object rowId, SessionImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public Type[] getPropertyTypes() {
			return new Type[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String[] getPropertyNames() {
			return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean[] getPropertyInsertability() {
			return new boolean[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public ValueInclusion[] getPropertyInsertGenerationInclusions() {
			return new ValueInclusion[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
			return new ValueInclusion[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean[] getPropertyUpdateability() {
			return new boolean[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean[] getPropertyCheckability() {
			return new boolean[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean[] getPropertyNullability() {
			return new boolean[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean[] getPropertyVersionability() {
			return new boolean[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean[] getPropertyLaziness() {
			return new boolean[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public CascadeStyle[] getPropertyCascadeStyles() {
			return new CascadeStyle[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Type getIdentifierType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String getIdentifierPropertyName() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isCacheInvalidationRequired() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isLazyPropertiesCacheable() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasCache() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public EntityRegionAccessStrategy getCacheAccessStrategy() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public CacheEntryStructure getCacheEntryStructure() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public ClassMetadata getClassMetadata() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isBatchLoadable() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isSelectBeforeUpdateRequired() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session) throws HibernateException {
			return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object getCurrentVersion(Serializable id, SessionImplementor session) throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object forceVersionIncrement(Serializable id, Object currentVersion, SessionImplementor session)
				throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public EntityMode guessEntityMode(Object object) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isInstrumented(EntityMode entityMode) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasInsertGeneratedProperties() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasUpdateGeneratedProperties() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isVersionPropertyGenerated() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void afterReassociate(Object entity, SessionImplementor session) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public Object createProxy(Serializable id, SessionImplementor session) throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Boolean isTransient(Object object, SessionImplementor session) throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SessionImplementor session)
				throws HibernateException {
			return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void processInsertGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void processUpdateGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public Class getMappedClass(EntityMode entityMode) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean implementsLifecycle(EntityMode entityMode) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean implementsValidatable(EntityMode entityMode) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Class getConcreteProxyClass(EntityMode entityMode) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void setPropertyValues(Object object, Object[] values, EntityMode entityMode) throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void setPropertyValue(Object object, int i, Object value, EntityMode entityMode)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public Object[] getPropertyValues(Object object, EntityMode entityMode) throws HibernateException {
			return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object getPropertyValue(Object object, int i, EntityMode entityMode) throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object getPropertyValue(Object object, String propertyName, EntityMode entityMode)
				throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Serializable getIdentifier(Object object, EntityMode entityMode) throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Serializable getIdentifier(Object entity, SessionImplementor session) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void setIdentifier(Object entity, Serializable id, EntityMode entityMode) throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void setIdentifier(Object entity, Serializable id, SessionImplementor session) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public Object getVersion(Object object, EntityMode entityMode) throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object instantiate(Serializable id, EntityMode entityMode) throws HibernateException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object instantiate(Serializable id, SessionImplementor session) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isInstance(Object object, EntityMode entityMode) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasUninitializedLazyProperties(Object object, EntityMode entityMode) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, EntityMode entityMode) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SessionImplementor session) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory, EntityMode entityMode) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}
	}

	public static class NoopCollectionPersister implements CollectionPersister {

		public NoopCollectionPersister(org.hibernate.mapping.Collection collection,
									   org.hibernate.cache.access.CollectionRegionAccessStrategy strategy,
									   org.hibernate.cfg.Configuration configuration,
									   org.hibernate.engine.SessionFactoryImplementor sf) {
			throw new GoofyException(NoopCollectionPersister.class);
		}

		public void initialize(Serializable key, SessionImplementor session) throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasCache() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public CollectionRegionAccessStrategy getCacheAccessStrategy() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public CacheEntryStructure getCacheEntryStructure() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public CollectionType getCollectionType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Type getKeyType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Type getIndexType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Type getElementType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Class getElementClass() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object readKey(ResultSet rs, String[] keyAliases, SessionImplementor session)
				throws HibernateException, SQLException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object readElement(ResultSet rs, Object owner, String[] columnAliases, SessionImplementor session)
				throws HibernateException, SQLException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object readIndex(ResultSet rs, String[] columnAliases, SessionImplementor session)
				throws HibernateException, SQLException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object readIdentifier(ResultSet rs, String columnAlias, SessionImplementor session)
				throws HibernateException, SQLException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isPrimitiveArray() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isArray() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isOneToMany() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isManyToMany() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String getManyToManyFilterFragment(String alias, Map enabledFilters) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasIndex() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isLazy() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isInverse() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void remove(Serializable id, SessionImplementor session) throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void recreate(PersistentCollection collection, Serializable key, SessionImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void deleteRows(PersistentCollection collection, Serializable key, SessionImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void updateRows(PersistentCollection collection, Serializable key, SessionImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void insertRows(PersistentCollection collection, Serializable key, SessionImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public String getRole() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public EntityPersister getOwnerEntityPersister() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public IdentifierGenerator getIdentifierGenerator() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Type getIdentifierType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasOrphanDelete() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasOrdering() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasManyToManyOrdering() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Serializable[] getCollectionSpaces() {
			return new Serializable[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public CollectionMetadata getCollectionMetadata() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isCascadeDeleteEnabled() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isVersioned() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isMutable() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String getNodeName() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String getElementNodeName() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String getIndexNodeName() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void postInstantiate() throws MappingException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public SessionFactoryImplementor getFactory() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isAffectedByEnabledFilters(SessionImplementor session) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String[] getKeyColumnAliases(String suffix) {
			return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String[] getIndexColumnAliases(String suffix) {
			return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String[] getElementColumnAliases(String suffix) {
			return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String getIdentifierColumnAlias(String suffix) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isExtraLazy() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public int getSize(Serializable key, SessionImplementor session) {
			return 0;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean indexExists(Serializable key, Object index, SessionImplementor session) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean elementExists(Serializable key, Object element, SessionImplementor session) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object getElementByIndex(Serializable key, Object index, SessionImplementor session, Object owner) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}
	}
}
