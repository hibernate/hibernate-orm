/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.spi.EntityMappingImplementor;
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
import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmNavigableReference;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class JoinedEntityTypeDescriptor<J> extends AbstractEntityTypeDescriptor<J> {

	public JoinedEntityTypeDescriptor(
			EntityMappingImplementor bootMapping,
			IdentifiableTypeDescriptor<? super J> superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super( bootMapping, superTypeDescriptor, creationContext );
	}

	@Override
	public String asLoggableText() {
		return String.format( "SingleTableEntityDescriptor<%s>", getEntityName() );

	}

	@Override
	public boolean finishInitialization(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		final boolean superDone = super.finishInitialization( bootDescriptor, creationContext );
		if ( !superDone ) {
			return false;
		}

		if ( bootDescriptor instanceof RootClass ) {
			// the hierarchy root
		}
		else if ( bootDescriptor instanceof JoinedSubclass ) {
			// branch/leaf
		}
		else {
			throw new IllegalStateException(
					"Expecting boot model descriptor to be RootClass or JoinedSubclass, but found : " + bootDescriptor );
		}

		return true;
	}


	@Override
	public SqmNavigableReference createSqmExpression(SqmPath lhs, SqmCreationState creationState) {
		return new SqmBasicValuedSimplePath(
				new NavigablePath( getNavigableName() + DiscriminatorDescriptor.NAVIGABLE_NAME ),
				this.getHierarchy().getDiscriminatorDescriptor(),
				null
		);
	}

	@Override
	public void lock(
			Object id, Object version, Object object, LockMode lockMode, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean hasProxy() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public int[] findDirty(
			Object[] currentState, Object[] previousState, Object owner, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public int[] findModified(
			Object[] old, Object[] current, Object object, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void insert(
			Object id, Object[] fields, Object object, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object insert(
			Object[] fields, Object object, SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void delete(
			Object id, Object version, Object object, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void update(
			Object id,
			Object[] fields,
			int[] dirtyFields,
			boolean hasDirtyCollection,
			Object[] oldFields,
			Object oldVersion,
			Object object,
			Object rowId,
			SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean hasCascades() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public String getIdentifierPropertyName() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean isCacheInvalidationRequired() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean isLazyPropertiesCacheable() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CacheEntry buildCacheEntry(
			Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Serializable getIdByUniqueKey(
			Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object getCurrentVersion(
			Object id, SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object forceVersionIncrement(
			Object id, Object currentVersion, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean isInstrumented() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void afterReassociate(Object entity, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object createProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Boolean isTransient(Object object, SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object[] getPropertyValuesToInsert(
			Object object,
			Map mergeMap,
			SharedSessionContractImplementor session) throws HibernateException {
		final Object[] stateArray = new Object[getStateArrayContributors().size()];
		visitStateArrayContributors(
				contributor -> {
					stateArray[contributor.getStateArrayPosition()] = contributor.getPropertyAccess()
							.getGetter()
							.getForInsert(
									object,
									mergeMap,
									session
							);
				}
		);

		return stateArray;
	}

	@Override
	public void processInsertGeneratedProperties(
			Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void processUpdateGeneratedProperties(
			Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Class getMappedClass() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean implementsLifecycle() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public EntityTypeDescriptor getSubclassEntityPersister(
			Object instance,
			SessionFactoryImplementor factory) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public int[] resolveAttributeIndexes(String[] attributeNames) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean canUseReferenceCacheEntries() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void registerAffectingFetchProfile(String fetchProfileName) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean hasCollections() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public JavaTypeDescriptor[] getPropertyJavaTypeDescriptors() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public String[] getPropertyNames() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public ValueInclusion[] getPropertyInsertGenerationInclusions() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean[] getPropertyUpdateability() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean[] getPropertyVersionability() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean[] getPropertyLaziness() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CascadeStyle[] getPropertyCascadeStyles() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean isAffectedByEnabledFilters(SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
