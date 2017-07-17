/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.loader.spi.MultiLoadOptions;
import org.hibernate.loader.spi.NaturalIdLoader;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class SingleTableEntityDescriptor<T> extends AbstractEntityDescriptor<T> {


	public SingleTableEntityDescriptor(
			EntityMapping bootMapping,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super( bootMapping, creationContext );
	}

	@Override
	public boolean isVersioned() {
		return false;
	}

	@Override
	public Comparator getVersionComparator() {
		return null;
	}

//	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return null;
	}

	@Override
	public String asLoggableText() {
		return null;
	}

	@Override
	public void finishInstantiation(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeDescriptor<? super T> superType,
			IdentifiableTypeMapping bootMapping,
			RuntimeModelCreationContext creationContext) {

	}

	@Override
	public void completeInitialization(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeDescriptor<? super T> superType,
			IdentifiableTypeMappingImplementor bootMapping,
			RuntimeModelCreationContext creationContext) {

	}

	@Override
	public void postInstantiate() {

	}

	@Override
	public EntityIdentifier getIdentifierDescriptor() {
		return null;
	}

	@Override
	public EntityEntryFactory getEntityEntryFactory() {
		return null;
	}

	@Override
	public NaturalIdLoader getNaturalIdLoader(LockOptions lockOptions) {
		return null;
	}

	@Override
	public Table getPrimaryTable() {
		return null;
	}

	@Override
	public List<JoinedTableBinding> getSecondaryTableBindings() {
		return null;
	}

	@Override
	public String[] getAffectedTableNames() {
		return new String[0];
	}

	@Override
	public boolean isMultiTable() {
		return false;
	}

	@Override
	public List<EntityNameResolver> getEntityNameResolvers() {
		return null;
	}

	@Override
	public boolean hasProxy() {
		return false;
	}

	@Override
	public int[] findDirty(
			Object[] currentState, Object[] previousState, Object owner, SharedSessionContractImplementor session) {
		return new int[0];
	}

	@Override
	public int[] findModified(
			Object[] old, Object[] current, Object object, SharedSessionContractImplementor session) {
		return new int[0];
	}

	@Override
	public Serializable loadEntityIdByNaturalId(
			Object[] naturalIdValues, LockOptions lockOptions, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public Object load(
			Serializable id, Object optionalObject, LockMode lockMode, SharedSessionContractImplementor session)
			throws HibernateException {
		return null;
	}

	@Override
	public Object load(
			Serializable id, Object optionalObject, LockOptions lockOptions, SharedSessionContractImplementor session)
			throws HibernateException {
		return null;
	}

	@Override
	public List multiLoad(
			Serializable[] ids, SharedSessionContractImplementor session, MultiLoadOptions loadOptions) {
		return null;
	}

	@Override
	public void lock(
			Serializable id, Object version, Object object, LockMode lockMode, SharedSessionContractImplementor session)
			throws HibernateException {

	}

	@Override
	public void lock(
			Serializable id,
			Object version,
			Object object,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) throws HibernateException {

	}

	@Override
	public void insert(
			Serializable id, Object[] fields, Object object, SharedSessionContractImplementor session)
			throws HibernateException {

	}

	@Override
	public Serializable insert(
			Object[] fields, Object object, SharedSessionContractImplementor session) throws HibernateException {
		return null;
	}

	@Override
	public void delete(
			Serializable id, Object version, Object object, SharedSessionContractImplementor session)
			throws HibernateException {

	}

	@Override
	public void update(
			Serializable id,
			Object[] fields,
			int[] dirtyFields,
			boolean hasDirtyCollection,
			Object[] oldFields,
			Object oldVersion,
			Object object,
			Object rowId,
			SharedSessionContractImplementor session) throws HibernateException {

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
	public CacheEntryStructure getCacheEntryStructure() {
		return null;
	}

	@Override
	public CacheEntry buildCacheEntry(
			Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
		return null;
	}

//	@Override
	public boolean hasNaturalIdCache() {
		return false;
	}

//	@Override
//	public ClassMetadata getClassMetadata() {
//		return null;
//	}

	@Override
	public boolean isBatchLoadable() {
		return false;
	}

	@Override
	public boolean isSelectBeforeUpdateRequired() {
		return false;
	}

	@Override
	public Object[] getDatabaseSnapshot(Serializable id, SharedSessionContractImplementor session)
			throws HibernateException {
		return new Object[0];
	}

	@Override
	public Serializable getIdByUniqueKey(
			Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public Object getCurrentVersion(Serializable id, SharedSessionContractImplementor session)
			throws HibernateException {
		return null;
	}

	@Override
	public Object forceVersionIncrement(
			Serializable id, Object currentVersion, SharedSessionContractImplementor session)
			throws HibernateException {
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
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {

	}

	@Override
	public void afterReassociate(Object entity, SharedSessionContractImplementor session) {

	}

	@Override
	public Object createProxy(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
		return null;
	}

	@Override
	public Boolean isTransient(Object object, SharedSessionContractImplementor session) throws HibernateException {
		return null;
	}

	@Override
	public Object[] getPropertyValuesToInsert(
			Object object, Map mergeMap, SharedSessionContractImplementor session) throws HibernateException {
		return new Object[0];
	}

	@Override
	public void processInsertGeneratedProperties(
			Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session) {

	}

	@Override
	public void processUpdateGeneratedProperties(
			Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session) {

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
	public Object getPropertyValue(Object object, int i) throws HibernateException {
		return null;
	}

	@Override
	public Object getPropertyValue(Object object, String propertyName) {
		return null;
	}

	@Override
	public Serializable getIdentifier(Object object) throws HibernateException {
		return null;
	}

	@Override
	public Serializable getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public void setIdentifier(
			Object entity, Serializable id, SharedSessionContractImplementor session) {

	}

	@Override
	public Object getVersion(Object object) throws HibernateException {
		return null;
	}

	@Override
	public Object instantiate(Serializable id, SharedSessionContractImplementor session) {
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
	public void resetIdentifier(
			Object entity, Serializable currentId, Object currentVersion, SharedSessionContractImplementor session) {

	}

	@Override
	public EntityDescriptor getSubclassEntityPersister(
			Object instance, SessionFactoryImplementor factory) {
		return null;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return null;
	}

	@Override
	public int[] resolveAttributeIndexes(String[] attributeNames) {
		return new int[0];
	}

	@Override
	public boolean canUseReferenceCacheEntries() {
		return false;
	}

	@Override
	public void registerAffectingFetchProfile(String fetchProfileName) {

	}
}
