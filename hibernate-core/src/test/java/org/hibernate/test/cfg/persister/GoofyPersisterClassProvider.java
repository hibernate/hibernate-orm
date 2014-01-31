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
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.bytecode.spi.EntityInstrumentationMetadata;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.tuple.entity.NonPojoInstrumentationMetadata;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class GoofyPersisterClassProvider implements PersisterClassResolver {
	@Override
	public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata) {
		return NoopEntityPersister.class;
	}

	@Override
	public Class<? extends EntityPersister> getEntityPersisterClass(EntityBinding metadata) {
		return NoopEntityPersister.class;
	}

	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
		return NoopCollectionPersister.class;
	}

	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(PluralAttributeBinding metadata) {
		return NoopCollectionPersister.class;
	}

	public static class NoopEntityPersister implements EntityPersister {
		public NoopEntityPersister(
				EntityBinding entityBinding,
				EntityRegionAccessStrategy entityRegionAccessStrategy,
				NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
				SessionFactoryImplementor sf,
				Mapping mapping) {
			throw new GoofyException(NoopEntityPersister.class);
		}

		public NoopEntityPersister(org.hibernate.mapping.PersistentClass persistentClass,
								   org.hibernate.cache.spi.access.EntityRegionAccessStrategy strategy,
								   NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
								   SessionFactoryImplementor sf,
								   Mapping mapping) {
			throw new GoofyException(NoopEntityPersister.class);
		}

		@Override
		public EntityMode getEntityMode() {
			return null;
		}

		@Override
		public EntityTuplizer getEntityTuplizer() {
			return null;
		}

		@Override
		public EntityInstrumentationMetadata getInstrumentationMetadata() {
			return new NonPojoInstrumentationMetadata( null );
		}

		@Override
		public void generateEntityDefinition() {
		}

		@Override
		public void postInstantiate() throws MappingException {

		}

		@Override
		public SessionFactoryImplementor getFactory() {
			return null;
		}

		@Override
		public String getRootEntityName() {
			return null;
		}

		@Override
		public String getEntityName() {
			return null;
		}

		@Override
		public EntityMetamodel getEntityMetamodel() {
			return null;
		}

		@Override
		public boolean isSubclassEntityName(String entityName) {
			return false;
		}

		@Override
		public Serializable[] getPropertySpaces() {
			return new Serializable[0];
		}

		@Override
		public Serializable[] getQuerySpaces() {
			return new Serializable[0];
		}

		@Override
		public boolean hasProxy() {
			return false;
		}

		@Override
		public boolean hasCollections() {
			return false;
		}

		@Override
		public boolean hasMutableProperties() {
			return false;
		}

		@Override
		public boolean hasSubselectLoadableCollections() {
			return false;
		}

		@Override
		public boolean hasCascades() {
			return false;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public boolean isInherited() {
			return false;
		}

		@Override
		public boolean isIdentifierAssignedByInsert() {
			return false;
		}

		@Override
		public Type getPropertyType(String propertyName) throws MappingException {
			return null;
		}

		@Override
		public int[] findDirty(Object[] currentState, Object[] previousState, Object owner, SessionImplementor session) {
			return new int[0];
		}

		@Override
		public int[] findModified(Object[] old, Object[] current, Object object, SessionImplementor session) {
			return new int[0];
		}

		@Override
		public boolean hasIdentifierProperty() {
			return false;
		}

		@Override
		public boolean canExtractIdOutOfEntity() {
			return false;
		}

		@Override
		public boolean isVersioned() {
			return false;
		}

		@Override
		public Comparator getVersionComparator() {
			return null;
		}

		@Override
		public VersionType getVersionType() {
			return null;
		}

		@Override
		public int getVersionProperty() {
			return 0;
		}

		@Override
		public boolean hasNaturalIdentifier() {
			return false;
		}

		@Override
		public int[] getNaturalIdentifierProperties() {
			return new int[0];
		}

		@Override
		public Object[] getNaturalIdentifierSnapshot(Serializable id, SessionImplementor session) {
			return new Object[0];
		}

		@Override
		public Serializable loadEntityIdByNaturalId(Object[] naturalIdValues, LockOptions lockOptions,
				SessionImplementor session) {
			return null;
		}

		@Override
		public IdentifierGenerator getIdentifierGenerator() {
			return null;
		}

		@Override
		public boolean hasLazyProperties() {
			return false;
		}

		@Override
		public Object load(Serializable id, Object optionalObject, LockMode lockMode, SessionImplementor session) {
			return null;
		}

		@Override
		public Object load(Serializable id, Object optionalObject, LockOptions lockOptions, SessionImplementor session) {
			return null;
		}

		@Override
		public void lock(Serializable id, Object version, Object object, LockMode lockMode, SessionImplementor session) {
		}

		@Override
		public void lock(Serializable id, Object version, Object object, LockOptions lockOptions, SessionImplementor session) {
		}

		@Override
		public void insert(Serializable id, Object[] fields, Object object, SessionImplementor session) {
		}

		@Override
		public Serializable insert(Object[] fields, Object object, SessionImplementor session) {
			return null;
		}

		@Override
		public void delete(Serializable id, Object version, Object object, SessionImplementor session) {
		}

		@Override
		public void update(Serializable id, Object[] fields, int[] dirtyFields, boolean hasDirtyCollection, Object[] oldFields, Object oldVersion, Object object, Object rowId, SessionImplementor session) {
		}

		@Override
		public Type[] getPropertyTypes() {
			return new Type[0];
		}

		@Override
		public String[] getPropertyNames() {
			return new String[0];
		}

		@Override
		public boolean[] getPropertyInsertability() {
			return new boolean[0];
		}

		@Override
		public ValueInclusion[] getPropertyInsertGenerationInclusions() {
			return new ValueInclusion[0];
		}

		@Override
		public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
			return new ValueInclusion[0];
		}

		@Override
		public boolean[] getPropertyUpdateability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyCheckability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyNullability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyVersionability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyLaziness() {
			return new boolean[0];
		}

		@Override
		public CascadeStyle[] getPropertyCascadeStyles() {
			return new CascadeStyle[0];
		}

		@Override
		public Type getIdentifierType() {
			return null;
		}

		@Override
		public String getIdentifierPropertyName() {
			return null;
		}

		@Override
		public boolean isCacheInvalidationRequired() {
			return false;
		}

		@Override
		public boolean isLazyPropertiesCacheable() {
			return false;
		}

		@Override
		public boolean hasCache() {
			return false;
		}

		@Override
		public EntityRegionAccessStrategy getCacheAccessStrategy() {
			return null;
		}
		
		@Override
		public boolean hasNaturalIdCache() {
			return false;
		}

		@Override
		public NaturalIdRegionAccessStrategy getNaturalIdCacheAccessStrategy() {
			return null;
		}

		@Override
		public CacheEntryStructure getCacheEntryStructure() {
			return null;
		}

		@Override
		public CacheEntry buildCacheEntry(
				Object entity, Object[] state, Object version, SessionImplementor session) {
			return null;
		}

		@Override
		public ClassMetadata getClassMetadata() {
			return null;
		}

		@Override
		public boolean isBatchLoadable() {
			return false;
		}

		@Override
		public boolean isSelectBeforeUpdateRequired() {
			return false;
		}

		@Override
		public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session) throws HibernateException {
			return new Object[0];
		}

		@Override
		public Serializable getIdByUniqueKey(Serializable key, String uniquePropertyName, SessionImplementor session) {
			throw new UnsupportedOperationException( "not supported" );
		}

		@Override
		public Object getCurrentVersion(Serializable id, SessionImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object forceVersionIncrement(Serializable id, Object currentVersion, SessionImplementor session) {
			return null;
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
		public boolean isVersionPropertyGenerated() {
			return false;
		}

		@Override
		public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session) {
		}

		@Override
		public void afterReassociate(Object entity, SessionImplementor session) {
		}

		@Override
		public Object createProxy(Serializable id, SessionImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Boolean isTransient(Object object, SessionImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SessionImplementor session) {
			return new Object[0];
		}

		@Override
		public void processInsertGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
		}

		@Override
		public void processUpdateGeneratedProperties(Serializable id, Object entity, Object[] state, SessionImplementor session) {
		}

		@Override
		public Class getMappedClass() {
			return null;
		}

		@Override
		public boolean implementsLifecycle() {
			return false;
		}

		@Override
		public Class getConcreteProxyClass() {
			return null;
		}

		@Override
		public void setPropertyValues(Object object, Object[] values) {
		}

		@Override
		public void setPropertyValue(Object object, int i, Object value) {
		}

		@Override
		public Object[] getPropertyValues(Object object) {
			return new Object[0];
		}

		@Override
		public Object getPropertyValue(Object object, int i) {
			return null;
		}

		@Override
		public Object getPropertyValue(Object object, String propertyName) {
			return null;
		}

		@Override
		public Serializable getIdentifier(Object object) {
			return null;
		}

		@Override
		public Serializable getIdentifier(Object entity, SessionImplementor session) {
			return null;
		}

		@Override
		public void setIdentifier(Object entity, Serializable id, SessionImplementor session) {
		}

		@Override
		public Object getVersion(Object object) {
			return null;
		}

		@Override
		public Object instantiate(Serializable id, SessionImplementor session) {
			return null;
		}

		@Override
		public boolean isInstance(Object object) {
			return false;
		}

		@Override
		public boolean hasUninitializedLazyProperties(Object object) {
			return false;
		}

		@Override
		public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SessionImplementor session) {
		}

		@Override
		public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory) {
			return null;
		}

		@Override
		public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EntityPersister getEntityPersister() {
			return this;
		}

		@Override
		public EntityIdentifierDefinition getEntityKeyDefinition() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
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
			return false;
		}
	}

	public static class NoopCollectionPersister implements CollectionPersister {
		public NoopCollectionPersister(
				AbstractPluralAttributeBinding collectionBinding,
				CollectionRegionAccessStrategy regionAccessStrategy,
				MetadataImplementor metadata,
				SessionFactoryImplementor sf) {
			throw new GoofyException(NoopCollectionPersister.class);
		}

		public NoopCollectionPersister(org.hibernate.mapping.Collection collection,
									   org.hibernate.cache.spi.access.CollectionRegionAccessStrategy strategy,
									   org.hibernate.cfg.Configuration configuration,
									   SessionFactoryImplementor sf) {
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

		@Override
		public CollectionPersister getCollectionPersister() {
			return this;
		}

		public CollectionType getCollectionType() {
			throw new NotYetImplementedException();
		}

		@Override
		public CollectionIndexDefinition getIndexDefinition() {
			throw new NotYetImplementedException();
		}

		@Override
		public CollectionElementDefinition getElementDefinition() {
			throw new NotYetImplementedException();
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

		@Override
		public int getBatchSize() {
			return 0;
		}

		@Override
		public String getMappedByProperty() {
			return null;
		}

		@Override
		public void processQueuedOps(PersistentCollection collection, Serializable key, SessionImplementor session)
				throws HibernateException {
		}
	}
}
