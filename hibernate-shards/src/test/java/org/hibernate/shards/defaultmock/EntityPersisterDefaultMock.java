/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.defaultmock;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.entry.CacheEntryStructure;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

/**
 * @author Maulik Shah
 */
public class EntityPersisterDefaultMock implements EntityPersister {

  public void postInstantiate() throws MappingException {
    throw new UnsupportedOperationException();
  }

  public SessionFactoryImplementor getFactory() {
    throw new UnsupportedOperationException();
  }

  public String getRootEntityName() {
    throw new UnsupportedOperationException();
  }

  public String getEntityName() {
    throw new UnsupportedOperationException();
  }

  public boolean isSubclassEntityName(String entityName) {
    throw new UnsupportedOperationException();
  }

  public Serializable[] getPropertySpaces() {
    throw new UnsupportedOperationException();
  }

  public Serializable[] getQuerySpaces() {
    throw new UnsupportedOperationException();
  }

  public boolean hasProxy() {
    throw new UnsupportedOperationException();
  }

  public boolean hasCollections() {
    throw new UnsupportedOperationException();
  }

  public boolean hasMutableProperties() {
    throw new UnsupportedOperationException();
  }

  public boolean hasSubselectLoadableCollections() {
    throw new UnsupportedOperationException();
  }

  public boolean hasCascades() {
    throw new UnsupportedOperationException();
  }

  public boolean isMutable() {
    throw new UnsupportedOperationException();
  }

  public boolean isInherited() {
    throw new UnsupportedOperationException();
  }

  public boolean isIdentifierAssignedByInsert() {
    throw new UnsupportedOperationException();
  }

  public Type getPropertyType(String propertyName) throws MappingException {
    throw new UnsupportedOperationException();
  }

  public int[] findDirty(Object[] x, Object[] y, Object owner,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public int[] findModified(Object[] old, Object[] current, Object object,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean hasIdentifierProperty() {
    throw new UnsupportedOperationException();
  }

  public boolean canExtractIdOutOfEntity() {
    throw new UnsupportedOperationException();
  }

  public boolean isVersioned() {
    throw new UnsupportedOperationException();
  }

  public Comparator getVersionComparator() {
    throw new UnsupportedOperationException();
  }

  public VersionType getVersionType() {
    throw new UnsupportedOperationException();
  }

  public int getVersionProperty() {
    throw new UnsupportedOperationException();
  }

  public boolean hasNaturalIdentifier() {
    throw new UnsupportedOperationException();
  }

  public int[] getNaturalIdentifierProperties() {
    throw new UnsupportedOperationException();
  }

  public Object[] getNaturalIdentifierSnapshot(Serializable id,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public IdentifierGenerator getIdentifierGenerator()
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean hasLazyProperties() {
    throw new UnsupportedOperationException();
  }

  public Object load(Serializable id, Object optionalObject, LockMode lockMode,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void lock(Serializable id, Object version, Object object,
      LockMode lockMode, SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void insert(Serializable id, Object[] fields, Object object,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Serializable insert(Object[] fields, Object object,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void delete(Serializable id, Object version, Object object,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void update(Serializable id, Object[] fields, int[] dirtyFields,
      boolean hasDirtyCollection, Object[] oldFields, Object oldVersion,
      Object object, Object rowId, SessionImplementor session)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Type[] getPropertyTypes() {
    throw new UnsupportedOperationException();
  }

  public String[] getPropertyNames() {
    throw new UnsupportedOperationException();
  }

  public boolean[] getPropertyInsertability() {
    throw new UnsupportedOperationException();
  }

  public boolean[] getPropertyInsertGeneration() {
    throw new UnsupportedOperationException();
  }

  public boolean[] getPropertyUpdateGeneration() {
    throw new UnsupportedOperationException();
  }

  public boolean[] getPropertyUpdateability() {
    throw new UnsupportedOperationException();
  }

  public boolean[] getPropertyCheckability() {
    throw new UnsupportedOperationException();
  }

  public boolean[] getPropertyNullability() {
    throw new UnsupportedOperationException();
  }

  public boolean[] getPropertyVersionability() {
    throw new UnsupportedOperationException();
  }

  public boolean[] getPropertyLaziness() {
    throw new UnsupportedOperationException();
  }

  public CascadeStyle[] getPropertyCascadeStyles() {
    throw new UnsupportedOperationException();
  }

  public Type getIdentifierType() {
    throw new UnsupportedOperationException();
  }

  public String getIdentifierPropertyName() {
    throw new UnsupportedOperationException();
  }

  public boolean isCacheInvalidationRequired() {
    throw new UnsupportedOperationException();
  }

  public boolean isLazyPropertiesCacheable() {
    throw new UnsupportedOperationException();
  }

  public boolean hasCache() {
    throw new UnsupportedOperationException();
  }

  public CacheConcurrencyStrategy getCache() {
    throw new UnsupportedOperationException();
  }

  public CacheEntryStructure getCacheEntryStructure() {
    throw new UnsupportedOperationException();
  }

  public ClassMetadata getClassMetadata() {
    throw new UnsupportedOperationException();
  }

  public boolean isBatchLoadable() {
    throw new UnsupportedOperationException();
  }

  public boolean isSelectBeforeUpdateRequired() {
    throw new UnsupportedOperationException();
  }

  public Object[] getDatabaseSnapshot(Serializable id,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object getCurrentVersion(Serializable id, SessionImplementor session)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object forceVersionIncrement(Serializable id, Object currentVersion,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public EntityMode guessEntityMode(Object object) {
    throw new UnsupportedOperationException();
  }

  public boolean isInstrumented(EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public boolean hasInsertGeneratedProperties() {
    throw new UnsupportedOperationException();
  }

  public boolean hasUpdateGeneratedProperties() {
    throw new UnsupportedOperationException();
  }

  public boolean isVersionPropertyGenerated() {
    throw new UnsupportedOperationException();
  }

  public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched,
      SessionImplementor session) {
    throw new UnsupportedOperationException();
  }

  public void afterReassociate(Object entity, SessionImplementor session) {
    throw new UnsupportedOperationException();
  }

  public Object createProxy(Serializable id, SessionImplementor session)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Boolean isTransient(Object object, SessionImplementor session)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object[] getPropertyValuesToInsert(Object object, Map mergeMap,
      SessionImplementor session) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void processInsertGeneratedProperties(Serializable id, Object entity,
      Object[] state, SessionImplementor session) {
    throw new UnsupportedOperationException();
  }

  public void processUpdateGeneratedProperties(Serializable id, Object entity,
      Object[] state, SessionImplementor session) {
    throw new UnsupportedOperationException();
  }

  public Class getMappedClass(EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public boolean implementsLifecycle(EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public boolean implementsValidatable(EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public Class getConcreteProxyClass(EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public void setPropertyValues(Object object, Object[] values,
      EntityMode entityMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void setPropertyValue(Object object, int i, Object value,
      EntityMode entityMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object[] getPropertyValues(Object object, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object getPropertyValue(Object object, int i, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object getPropertyValue(Object object, String propertyName,
      EntityMode entityMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Serializable getIdentifier(Object object, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void setIdentifier(Object object, Serializable id,
      EntityMode entityMode) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object getVersion(Object object, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object instantiate(Serializable id, EntityMode entityMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public boolean isInstance(Object object, EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public boolean hasUninitializedLazyProperties(Object object,
      EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public void resetIdentifier(Object entity, Serializable currentId,
      Object currentVersion, EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public EntityPersister getSubclassEntityPersister(Object instance,
      SessionFactoryImplementor factory, EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public EntityMetamodel getEntityMetamodel() {
    throw new UnsupportedOperationException();
  }

  public ValueInclusion[] getPropertyInsertGenerationInclusions() {
    throw new UnsupportedOperationException();
  }

  public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
    throw new UnsupportedOperationException();
  }
}
