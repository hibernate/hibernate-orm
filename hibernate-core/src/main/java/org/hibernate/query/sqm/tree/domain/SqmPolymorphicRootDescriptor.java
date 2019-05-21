/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.loader.spi.EntityLocker;
import org.hibernate.loader.spi.MultiIdEntityLoader;
import org.hibernate.loader.spi.MultiIdLoaderSelectors;
import org.hibernate.loader.spi.NaturalIdLoader;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.loader.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.mapping.spi.EntityHierarchy;
import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.metamodel.model.mapping.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.mapping.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.mapping.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.mapping.spi.ManagedTypeRepresentationStrategy;
import org.hibernate.metamodel.model.mapping.spi.Navigable;
import org.hibernate.metamodel.model.mapping.spi.NavigableContainer;
import org.hibernate.metamodel.model.mapping.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.mapping.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.mapping.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.mapping.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.mapping.spi.StateArrayContributor;
import org.hibernate.metamodel.model.mapping.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.internal.EntityJavaDescriptorImpl;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Acts as the EntityValuedNavigable for a "polymorphic query" grouping
 *
 * @author Steve Ebersole
 */
public class SqmPolymorphicRootDescriptor<T> implements EntityTypeDescriptor<T> {
	private final Set<EntityTypeDescriptor<?>> implementors;
	private final Map<String, PersistentAttributeDescriptor> commonAttributes;

	private final NavigableRole navigableRole;
	private final EntityJavaDescriptor<T> polymorphicJavaDescriptor;
	private final SessionFactoryImplementor sessionFactory;

	public SqmPolymorphicRootDescriptor(
			JavaTypeDescriptor<T> polymorphicJavaDescriptor,
			Set<EntityTypeDescriptor<?>> implementors) {
		this.implementors = implementors;
		final HashMap<String, PersistentAttributeDescriptor> workMap = new HashMap<>();

		final ArrayList<EntityTypeDescriptor<?>> implementorsList = new ArrayList<>( implementors );

		final EntityTypeDescriptor<?> firstImplementor = implementorsList.get( 0 ).getEntityDescriptor();

		// basically we want to "expose" only the attributes that all the implementors expose...
		// - visit all of the attributes defined on the first implementor in the list
		final List<EntityTypeDescriptor<?>> subList = implementorsList.subList( 1, implementors.size() - 1 );
		firstImplementor.visitAttributes(
				attribute -> {
					// for each of its attributes, check whether the other implementors also expose it
					for ( EntityValuedNavigable navigable : subList ) {
						if ( navigable.findNavigable( attribute.getNavigableName() ) == null ) {
							// we found an implementor that does not expose that attribute,
							// so break-out to the next attribute
							break;
						}

						// if we get here - they all had it.  so put it in the workMap
						//
						// todo (6.0) : Atm We use the attribute from the first implementor directly for each implementor
						//		need to handle this in QuerySplitter somehow
						workMap.put( attribute.getNavigableName(), attribute );
					}

				}
		);
		this.commonAttributes = Collections.unmodifiableMap( workMap );

		this.polymorphicJavaDescriptor = resolveEntityJavaTypeDescriptor( polymorphicJavaDescriptor );
		this.navigableRole = new NavigableRole( this.polymorphicJavaDescriptor.getEntityName() );
		this.sessionFactory = firstImplementor.getFactory();
	}


	@SuppressWarnings("unchecked")
	private EntityJavaDescriptor<T> resolveEntityJavaTypeDescriptor(JavaTypeDescriptor<T> javaTypeDescriptor) {
		if ( javaTypeDescriptor instanceof EntityJavaDescriptor ) {
			return (EntityJavaDescriptor) javaTypeDescriptor;
		}

		return new EntityJavaDescriptorImpl<>(
				javaTypeDescriptor.getTypeName(),
				javaTypeDescriptor.getTypeName(),
				javaTypeDescriptor.getJavaType(),
				null,
				ImmutableMutabilityPlan.INSTANCE,
				null
		);
	}

	public Set<EntityTypeDescriptor<?>> getImplementors() {
		return new HashSet<>( implementors );
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return sessionFactory;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return getFactory().getTypeConfiguration();
	}


	@Override
	public EntityJavaDescriptor<T> getJavaTypeDescriptor() {
		return polymorphicJavaDescriptor;
	}

	@Override
	public NavigableContainer<?> getContainer() {
		return null;
	}

	@Override
	public Class getMappedClass() {
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityTypeDescriptor<T> getEntityDescriptor() {
		return this;
	}
	@Override

	public IdentifiableTypeDescriptor getSuperTypeDescriptor() {
		return null;
	}

	@Override
	public Collection getSubclassTypes() {
		return (Collection) Collections.unmodifiableSet( implementors );
	}

	@Override
	public Object getDiscriminatorValue() {
		return null;
	}

	@Override
	public String getEntityName() {
		return getJavaType().getName();
	}

	@Override
	public String getJpaEntityName() {
		return getJavaTypeDescriptor().getJpaEntityName();
	}

	@Override
	public String getName() {
		return getJavaTypeDescriptor().getJpaEntityName();
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	@Override
	public Class getBindableJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String asLoggableText() {
		return "PolymorphicEntityValuedNavigable( " + getEntityName() + ")";
	}

	@Override
	public Navigable findNavigable(String navigableName) {
		// only return navigables that all of the implementors define
		Navigable navigable = null;
		for ( EntityTypeDescriptor implementor : implementors ) {
			final Navigable current = implementor.findNavigable( navigableName );
			if ( current == null ) {
				return null;
			}
			if ( navigable == null ) {
				navigable = current;
			}
		}

		return navigable;
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		// nothing to do
	}

	@Override
	public Navigable findDeclaredNavigable(String navigableName) {
		return null;
	}

	@Override
	public boolean isSubclassTypeName(String name) {
		for ( EntityTypeDescriptor<?> implementor : implementors ) {
			final EntityJavaDescriptor<?> implementorJtd = implementor.getJavaTypeDescriptor();
			if ( implementorJtd.getEntityName().equals( name )
					|| implementorJtd.getJpaEntityName().equals( name )
					|| implementorJtd.getTypeName().equals( name ) ) {
				return true;
			}
			if ( implementor.isSubclassTypeName( name ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean finishInitialization(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		// nothing to do here
		return true;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) : yet to implement


	@Override
	public EntityHierarchy getHierarchy() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public boolean canReadFromCache() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean canWriteToCache() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		// todo (6.0) : not sure how visitation should work here
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		// none
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		// todo (6.0) : not sure how visitation should work here
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public TableGroup createRootTableGroup(
			NavigablePath navigablePath,
			String explicitSourceAlias,
			JoinType tableReferenceJoinType,
			LockMode lockMode,
			SqlAstCreationState creationState) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean isNullable() {
		return false;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) : decide what to do for these.
	//		they are examples of some of the unwanted leakages mentioned on
	//		Navigable and NavigableSource



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// unsupported operations

	@Override
	public ManagedTypeRepresentationStrategy getRepresentationStrategy() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public List<StateArrayContributor<?>> getStateArrayContributors() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public NonIdPersistentAttribute findPersistentAttribute(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public NonIdPersistentAttribute findDeclaredPersistentAttribute(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public List<NonIdPersistentAttribute> getPersistentAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public List<NonIdPersistentAttribute> getDeclaredPersistentAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SubGraphImplementor<T> makeSubGraph() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> SubGraphImplementor<S> makeSubGraph(Class<S> subType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> ManagedTypeDescriptor<S> findSubType(String subTypeName) {
		// technically we could support this
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> ManagedTypeDescriptor<S> findSubType(Class<S> type) {
		// technically we could support this
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void visitStateArrayContributors(Consumer consumer) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public EntityEntryFactory getEntityEntryFactory() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public BytecodeEnhancementMetadata getBytecodeEnhancementMetadata() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Table getPrimaryTable() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public List<JoinedTableBinding> getSecondaryTableBindings() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingleIdEntityLoader getSingleIdLoader() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public MultiIdEntityLoader getMultiIdLoader(MultiIdLoaderSelectors selectors) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public NaturalIdLoader getNaturalIdLoader() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingleUniqueKeyEntityLoader getSingleUniqueKeyLoader(
			Navigable navigable, LoadQueryInfluencers loadQueryInfluencers) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public EntityLocker getLocker(
			LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			LockMode lockMode,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Set<String> getAffectedTableNames() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public List<EntityNameResolver> getEntityNameResolvers() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasProxy() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public int[] findDirty(
			Object[] currentState,
			Object[] previousState,
			Object owner,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public int[] findModified(
			Object[] old,
			Object[] current,
			Object object,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void insert(
			Object id,
			Object[] fields,
			Object object,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Object insert(
			Object[] fields,
			Object object,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void delete(
			Object id,
			Object version,
			Object object,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
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
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasCascades() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Type getIdentifierType() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public String getIdentifierPropertyName() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean isCacheInvalidationRequired() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean isLazyPropertiesCacheable() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public CacheEntry buildCacheEntry(
			Object entity,
			Object[] state,
			Object version,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean isBatchLoadable() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean isSelectBeforeUpdateRequired() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Object[] getDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Serializable getIdByUniqueKey(
			Serializable key,
			String uniquePropertyName,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Object getCurrentVersion(Object id, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Object forceVersionIncrement(
			Object id,
			Object currentVersion,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean isInstrumented() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void afterReassociate(Object entity, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Object createProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Boolean isTransient(Object object, SharedSessionContractImplementor session) throws HibernateException {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Object[] getPropertyValuesToInsert(
			Object object,
			Map mergeMap,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void processInsertGeneratedProperties(
			Object id,
			Object entity,
			Object[] state,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void processUpdateGeneratedProperties(
			Object id,
			Object entity,
			Object[] state,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean implementsLifecycle() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Class getConcreteProxyClass() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Serializable getIdentifier(Object entity) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Object getVersion(Object object) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Object instantiate(Object id, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean isInstance(Object object) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasUninitializedLazyProperties(Object object) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void resetIdentifier(
			Object entity,
			Object currentId,
			Object currentVersion,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public int[] resolveAttributeIndexes(String[] attributeNames) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean canUseReferenceCacheEntries() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void registerAffectingFetchProfile(String fetchProfileName) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasNaturalIdentifier() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasCollections() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public org.hibernate.type.descriptor.java.JavaTypeDescriptor[] getPropertyJavaTypeDescriptors() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public String[] getPropertyNames() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public ValueInclusion[] getPropertyInsertGenerationInclusions() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean[] getPropertyUpdateability() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean[] getPropertyVersionability() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean[] getPropertyLaziness() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public CascadeStyle[] getPropertyCascadeStyles() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingularAttribute getId(Class type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingularAttribute getDeclaredId(Class type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingularAttribute getVersion(Class type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingularAttribute getDeclaredVersion(Class type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasSingleIdAttribute() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean hasVersionAttribute() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Set getIdClassAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SimpleTypeDescriptor getIdType() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean isAffectedByEnabledFilters(LoadQueryInfluencers loadQueryInfluencers) {
		for ( EntityTypeDescriptor<?> implementor : implementors ) {
			if ( implementor.isAffectedByEnabledFilters( loadQueryInfluencers ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers loadQueryInfluencers) {
		for ( EntityTypeDescriptor<?> implementor : implementors ) {
			if ( implementor.isAffectedByEnabledFetchProfiles( loadQueryInfluencers ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isAffectedByEntityGraph(LoadQueryInfluencers loadQueryInfluencers) {
		for ( EntityTypeDescriptor<?> implementor : implementors ) {
			if ( implementor.isAffectedByEntityGraph( loadQueryInfluencers ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public Set getAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Set getDeclaredAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Set getSingularAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Set getDeclaredSingularAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Set getPluralAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public Set getDeclaredPluralAttributes() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public PersistentAttributeDescriptor getAttribute(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public PersistentAttributeDescriptor getDeclaredAttribute(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingularAttribute getSingularAttribute(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingularAttribute getDeclaredSingularAttribute(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public CollectionAttribute getCollection(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public CollectionAttribute getDeclaredCollection(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SetAttribute getSet(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SetAttribute getDeclaredSet(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public ListAttribute getList(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public ListAttribute getDeclaredList(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public MapAttribute getMap(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public MapAttribute getDeclaredMap(String name) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public MapAttribute getDeclaredMap(String name, Class keyType, Class valueType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public MapAttribute getMap(String name, Class keyType, Class valueType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public ListAttribute getDeclaredList(String name, Class elementType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public ListAttribute getList(String name, Class elementType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SetAttribute getDeclaredSet(String name, Class elementType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SetAttribute getSet(String name, Class elementType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public CollectionAttribute getDeclaredCollection(String name, Class elementType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public CollectionAttribute getCollection(String name, Class elementType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingularAttribute getDeclaredSingularAttribute(String name, Class type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SingularAttribute getSingularAttribute(String name, Class type) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public boolean canCompositeContainCollections() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public String getSqlAliasStem() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public EntityTypeDescriptor getSubclassEntityPersister(
			Object instance,
			SessionFactoryImplementor factory) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public InFlightAccess<T> getInFlightAccess() {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public void visitSubTypeDescriptors(Consumer<IdentifiableTypeDescriptor<? extends T>> action) {
		for ( EntityTypeDescriptor<?> implementor : implementors ) {
			action.accept( (EntityTypeDescriptor) implementor );
		}
	}

	@Override
	public void visitAllSubTypeDescriptors(Consumer<IdentifiableTypeDescriptor<? extends T>> action) {
		for ( EntityTypeDescriptor<?> implementor : implementors ) {
			action.accept( (IdentifiableTypeDescriptor) implementor );
			implementor.visitAllSubTypeDescriptors( (Consumer) action );
		}

	}

	@Override
	public IdentifiableTypeDescriptor findMatchingSubTypeDescriptors(Predicate<IdentifiableTypeDescriptor<? extends T>> matcher) {
		return null;
	}

}
