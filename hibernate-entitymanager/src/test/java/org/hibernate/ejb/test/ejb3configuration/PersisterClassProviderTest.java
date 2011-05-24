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
package org.hibernate.ejb.test.ejb3configuration;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PersisterClassProviderTest extends junit.framework.TestCase {
	public void testPersisterClassProvider() {
		Ejb3Configuration conf = new Ejb3Configuration();
		conf.getProperties().put( PersisterClassResolverInitiator.IMPL_NAME, GoofyPersisterClassProvider.class );
		conf.addAnnotatedClass( Bell.class );
		try {
			final EntityManagerFactory entityManagerFactory = conf.buildEntityManagerFactory();
			entityManagerFactory.close();
		}
		catch ( PersistenceException e ) {
			assertNotNull( e.getCause() );
			assertNotNull( e.getCause().getCause() );
			assertEquals( GoofyException.class, e.getCause().getCause().getClass() );

		}
	}

	public static class GoofyPersisterClassProvider implements PersisterClassResolver {
		@Override
		public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata) {
			return GoofyProvider.class;
		}

		@Override
		public Class<? extends EntityPersister> getEntityPersisterClass(EntityBinding metadata) {
			return GoofyProvider.class;
		}

		@Override
		public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
			return null;
		}

		@Override
		public Class<? extends CollectionPersister> getCollectionPersisterClass(PluralAttributeBinding metadata) {
			return null;
		}
	}

	public static class GoofyProvider implements EntityPersister {

		public GoofyProvider(org.hibernate.mapping.PersistentClass persistentClass,
								   org.hibernate.cache.spi.access.EntityRegionAccessStrategy strategy,
								   SessionFactoryImplementor sf,
								   Mapping mapping) {
			throw new GoofyException();
		}

		public void postInstantiate() throws MappingException {
			//To change body of implemented methods use File | Settings | File Templates.
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

	public static class GoofyException extends RuntimeException {

	}
}
