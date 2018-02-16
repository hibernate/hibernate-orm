/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class JoinedEntityDescriptor<J> extends AbstractEntityDescriptor<J> {

	public JoinedEntityDescriptor(
			EntityMapping bootMapping,
			IdentifiableTypeDescriptor<? super J> superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super( bootMapping, superTypeDescriptor, creationContext );
	}

	@Override
	public String asLoggableText() {
		return String.format( "SingleTableEntityDescriptor<%s>", getEntityName() );

	}

	@Override
	public void finishInitialization(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		super.finishInitialization( bootDescriptor, creationContext );

		if ( bootDescriptor instanceof RootClass ) {
			// the hierarchy root
		}
		else if ( bootDescriptor instanceof JoinedSubclass ) {
			// branch/leaf
		}
		else {
			throw new IllegalStateException( "Expecting boot model descriptor to be RootClass or JoinedSubclass, but found : " + bootDescriptor );
		}
	}


	// `select ... from Person p order by p`
	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return sourceSqmFrom.getNavigableReference();
	}








	@Override
	public Serializable loadEntityIdByNaturalId(
			Object[] naturalIdValues, LockOptions lockOptions, SharedSessionContractImplementor session) {
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
	public String[] getAffectedTableNames() {
		return new String[0];
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
	public boolean hasCascades() {
		return false;
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
	public CacheEntryStructure getCacheEntryStructure() {
		return null;
	}

	@Override
	public CacheEntry buildCacheEntry(
			Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
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
	public Object[] getDatabaseSnapshot(
			Serializable id, SharedSessionContractImplementor session) throws HibernateException {
		return new Object[0];
	}

	@Override
	public Serializable getIdByUniqueKey(
			Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public Object getCurrentVersion(
			Serializable id, SharedSessionContractImplementor session) throws HibernateException {
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
	public boolean hasUninitializedLazyProperties(Object object) {
		return false;
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

	@Override
	public boolean hasCollections() {
		return false;
	}

	@Override
	public Type[] getPropertyTypes() {
		return new Type[0];
	}

	@Override
	public JavaTypeDescriptor[] getPropertyJavaTypeDescriptors() {
		return new JavaTypeDescriptor[0];
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
	public boolean isAffectedByEnabledFilters(SharedSessionContractImplementor session) {
		return false;
	}
}
