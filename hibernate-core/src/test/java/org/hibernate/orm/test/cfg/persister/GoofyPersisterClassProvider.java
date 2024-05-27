/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cfg.persister;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.AttributeMappingsMap;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyEntry;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Emmanuel Bernard
 */
public class GoofyPersisterClassProvider implements PersisterClassResolver {
	@Override
	public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata) {
		return NoopEntityPersister.class;
	}

	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
		return NoopCollectionPersister.class;
	}

	public static class NoopEntityPersister implements EntityPersister {

		public NoopEntityPersister(
				final PersistentClass persistentClass,
				final EntityDataAccess cacheAccessStrategy,
				final NaturalIdDataAccess naturalIdRegionAccessStrategy,
				final PersisterCreationContext creationContext) {
			throw new GoofyException(NoopEntityPersister.class);
		}

		@Override
		public NavigableRole getNavigableRole() {
			return null;
		}

		@Override
		public BytecodeEnhancementMetadata getInstrumentationMetadata() {
			return new BytecodeEnhancementMetadataNonPojoImpl( null );
		}

		@Override
		public void postInstantiate() throws MappingException {

		}

		@Override
		public SessionFactoryImplementor getFactory() {
			return null;
		}

		@Override
		public EntityEntryFactory getEntityEntryFactory() {
			return MutableEntityEntryFactory.INSTANCE;
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
		public TableDetails getMappedTableDetails() {
			throw new UnsupportedOperationException();
		}

		@Override
		public TableDetails getIdentifierTableDetails() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ModelPart findSubPart(
				String name, EntityMappingType targetType) {
			return null;
		}

		@Override
		public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType targetType) {

		}

		@Override
		public <T> DomainResult<T> createDomainResult(
				NavigablePath navigablePath,
				TableGroup tableGroup,
				String resultVariable,
				DomainResultCreationState creationState) {
			return null;
		}

		@Override
		public void applySqlSelections(
				NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {

		}

		@Override
		public void applySqlSelections(
				NavigablePath navigablePath,
				TableGroup tableGroup,
				DomainResultCreationState creationState,
				BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {

		}

		@Override
		public JdbcMapping getJdbcMapping(int index) {
			throw new IndexOutOfBoundsException( index );
		}

		@Override
		public int forEachJdbcType(
				int offset, IndexedConsumer<JdbcMapping> action) {
			return 0;
		}

		@Override
		public Object disassemble(Object value, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public <X, Y> int forEachDisassembledJdbcValue(
				Object value,
				int offset,
				X x,
				Y y,
				JdbcValuesBiConsumer<X, Y> valuesConsumer,
				SharedSessionContractImplementor session) {
			return 0;
		}

		@Override
		public boolean isExplicitPolymorphism() {
			return false;
		}

		@Override
		public SqmMultiTableMutationStrategy getSqmMultiTableMutationStrategy() {
			return null;
		}

		@Override
		public SqmMultiTableInsertStrategy getSqmMultiTableInsertStrategy() {
			return null;
		}

		@Override
		public AttributeMapping findDeclaredAttributeMapping(String name) {
			return null;
		}

		@Override
		public AttributeMappingsMap getDeclaredAttributeMappings() {
			return null;
		}

		@Override
		public void visitDeclaredAttributeMappings(Consumer<? super AttributeMapping> action) {

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
		public String[] getPropertySpaces() {
			return new String[0];
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
		public boolean hasCollectionNotReferencingPK() {
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
		public int[] findDirty(Object[] currentState, Object[] previousState, Object owner, SharedSessionContractImplementor session) {
			return new int[0];
		}

		@Override
		public int[] findModified(Object[] old, Object[] current, Object object, SharedSessionContractImplementor session) {
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
		public BasicType<?> getVersionType() {
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
		public Object[] getNaturalIdentifierSnapshot(Object id, SharedSessionContractImplementor session) {
			return new Object[0];
		}

		@Override
		public Serializable loadEntityIdByNaturalId(
				Object[] naturalIdValues, LockOptions lockOptions, SharedSessionContractImplementor session) {
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
		public Object load(Object id, Object optionalObject, LockMode lockMode, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public Object load(Object id, Object optionalObject, LockOptions lockOptions, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public List<?> multiLoad(Object[] ids, EventSource session, MultiIdLoadOptions loadOptions) {
			return Collections.emptyList();
		}

		@Override
		public void lock(Object id, Object version, Object object, LockMode lockMode, EventSource session) {
		}

		@Override
		public void lock(Object id, Object version, Object object, LockOptions lockOptions, EventSource session) {
		}

		@Override
		public InsertCoordinator getInsertCoordinator() {
			return null;
		}

		@Override
		public UpdateCoordinator getUpdateCoordinator() {
			return null;
		}

		@Override
		public DeleteCoordinator getDeleteCoordinator() {
			return null;
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
		public boolean canReadFromCache() {
			return false;
		}

		@Override
		public boolean canWriteToCache() {
			return false;
		}

		@Override
		public boolean hasCache() {
			return false;
		}

		@Override
		public EntityDataAccess getCacheAccessStrategy() {
			return null;
		}
		
		@Override
		public boolean hasNaturalIdCache() {
			return false;
		}

		@Override
		public NaturalIdDataAccess getNaturalIdCacheAccessStrategy() {
			return null;
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
		public ClassMetadata getClassMetadata() {
			return null;
		}

		@Override
		public boolean isSelectBeforeUpdateRequired() {
			return false;
		}

		@Override
		public Object[] getDatabaseSnapshot(Object id, SharedSessionContractImplementor session) throws HibernateException {
			return new Object[0];
		}

		@Override
		public Object getIdByUniqueKey(Object key, String uniquePropertyName, SharedSessionContractImplementor session) {
			throw new UnsupportedOperationException( "not supported" );
		}

		@Override
		public Object getCurrentVersion(Object id, SharedSessionContractImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object forceVersionIncrement(Object id, Object currentVersion, SharedSessionContractImplementor session) {
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
		public Object createProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Boolean isTransient(Object object, SharedSessionContractImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SharedSessionContractImplementor session) {
			return new Object[0];
		}

		@Override
		public void processInsertGeneratedProperties(Object id, Object entity, Object[] state, GeneratedValues generatedValues, SharedSessionContractImplementor session) {
		}

		@Override
		public void processUpdateGeneratedProperties(Object id, Object entity, Object[] state, GeneratedValues generatedValues, SharedSessionContractImplementor session) {
		}

		@Override
		public Class<?> getMappedClass() {
			return null;
		}

		@Override
		public boolean implementsLifecycle() {
			return false;
		}

		@Override
		public Class<?> getConcreteProxyClass() {
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
		public Serializable getIdentifier(Object entity, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		}

		@Override
		public Object getVersion(Object object) {
			return null;
		}

		@Override
		public Object instantiate(Object id, SharedSessionContractImplementor session) {
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
		public void resetIdentifier(Object entity, Object currentId, Object currentVersion, SharedSessionContractImplementor session) {
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
		public EntityIdentifierMapping getIdentifierMapping() {
			return null;
		}

		@Override
		public EntityVersionMapping getVersionMapping() {
			return null;
		}

		@Override
		public EntityRowIdMapping getRowIdMapping() {
			return null;
		}

		@Override
		public void visitConstraintOrderedTables(ConstraintOrderedTableConsumer consumer) {

		}

		@Override
		public EntityDiscriminatorMapping getDiscriminatorMapping() {
			return null;
		}

		@Override
		public Object getDiscriminatorValue() {
			return null;
		}

		@Override
		public NaturalIdMapping getNaturalIdMapping() {
			return null;
		}

		@Override
		public boolean isTypeOrSuperType(EntityMappingType targetType) {
			return targetType == this;
		}

		@Override
		public EntityRepresentationStrategy getRepresentationStrategy() {
			return null;
		}

        @Override
        public int[] resolveAttributeIndexes(String[] attributeNames) {
            return null;
        }

		@Override
		public boolean canUseReferenceCacheEntries() {
			return false;
		}

		@Override
		public boolean useShallowQueryCacheLayout() {
			return false;
		}

		@Override
		public boolean storeDiscriminatorInShallowQueryCacheLayout() {
			return false;
		}

		@Override
		public boolean hasFilterForLoadByKey() {
			return false;
		}

		@Override
		public Iterable<UniqueKeyEntry> uniqueKeyEntries() {
			return Collections.emptyList();
		}

		@Override
		public String getSelectByUniqueKeyString(String propertyName) {
			return null;
		}

		@Override
		public String getSelectByUniqueKeyString(String[] propertyNames, String[] columnNames) {
			return null;
		}

		@Override
		public String[] getRootTableKeyColumnNames() {
			return new String[0];
		}

		@Override
		public String getIdentitySelectString() {
			return null;
		}

		@Override
		public String[] getIdentifierColumnNames() {
			return new String[0];
		}

		@Override
		public boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters) {
			return false;
		}

		@Override
		public boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers) {
			return false;
		}

		@Override
		public boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers) {
			return false;
		}

		@Override
		public AttributeMappingsList getAttributeMappings() {
			return null;
		}

		@Override
		public void forEachAttributeMapping(Consumer<? super AttributeMapping> action) {

		}

		@Override
		public JavaType getMappedJavaType() {
			return null;
		}

		@Override
		public EntityMappingType getTargetPart() {
			return null;
		}

		@Override
		public void forEachMutableTable(Consumer<EntityTableMapping> consumer) {

		}

		@Override
		public void forEachMutableTableReverse(Consumer<EntityTableMapping> consumer) {

		}

		@Override
		public String getIdentifierTableName() {
			return null;
		}

		@Override
		public EntityTableMapping getIdentifierTableMapping() {
			return null;
		}

		@Override
		public ModelPart getIdentifierDescriptor() {
			return null;
		}

		@Override
		public boolean hasSkippableTables() {
			return false;
		}

		@Override
		public GeneratedValuesMutationDelegate getInsertDelegate() {
			return null;
		}

		@Override
		public GeneratedValuesMutationDelegate getUpdateDelegate() {
			return null;
		}
	}

	public static class NoopCollectionPersister implements CollectionPersister {

		public NoopCollectionPersister(
				Collection collectionBinding,
				CollectionDataAccess cacheAccessStrategy,
				PersisterCreationContext creationContext) {
			throw new GoofyException(NoopCollectionPersister.class);
		}

		@Override
		public NavigableRole getNavigableRole() {
			return null;
		}

		public void initialize(Object key, SharedSessionContractImplementor session) throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasCache() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public CollectionDataAccess getCacheAccessStrategy() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public CacheEntryStructure getCacheEntryStructure() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public boolean useShallowQueryCacheLayout() {
			return false;
		}

		public CollectionType getCollectionType() {
			throw new UnsupportedOperationException();
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

		public Class<?> getElementClass() {
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

		public String getManyToManyFilterFragment(TableGroup tableGroup, Map<String, Filter> enabledFilters) {
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

		public void remove(Object id, SharedSessionContractImplementor session) throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void recreate(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void deleteRows(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void updateRows(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void insertRows(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session)
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

		public String[] getCollectionSpaces() {
			return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
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

		public void postInstantiate() throws MappingException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public SessionFactoryImplementor getFactory() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isAffectedByEnabledFilters(SharedSessionContractImplementor session) {
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

		public int getSize(Object key, SharedSessionContractImplementor session) {
			return 0;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean indexExists(Object key, Object index, SharedSessionContractImplementor session) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean elementExists(Object key, Object element, SharedSessionContractImplementor session) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object getElementByIndex(Object key, Object index, SharedSessionContractImplementor session, Object owner) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public String getMappedByProperty() {
			return null;
		}

		@Override
		public Comparator<?> getSortingComparator() {
			return null;
		}

		@Override
		public CollectionSemantics<?,?> getCollectionSemantics() {
			return null;
		}

		@Override
		public void applyBaseManyToManyRestrictions(Consumer<Predicate> predicateConsumer, TableGroup tableGroup, boolean useQualifier, Map<String, Filter> enabledFilters, Set<String> treatAsDeclarations, SqlAstCreationState creationState) {

		}

		@Override
		public void processQueuedOps(PersistentCollection<?> collection, Object key, SharedSessionContractImplementor session)
				throws HibernateException {
		}

		@Override
		public void applyFilterRestrictions(
				Consumer<Predicate> predicateConsumer,
				TableGroup tableGroup,
				boolean useQualifier,
				Map<String, Filter> enabledFilters,
				boolean onlyApplyLoadByKeyFilters,
				SqlAstCreationState creationState) {

		}

		@Override
		public void applyBaseRestrictions(Consumer<Predicate> predicateConsumer, TableGroup tableGroup, boolean useQualifier, Map<String, Filter> enabledFilters, Set<String> treatAsDeclarations, SqlAstCreationState creationState) {

		}

		@Override
		public void applyBaseRestrictions(
				Consumer<Predicate> predicateConsumer,
				TableGroup tableGroup,
				boolean useQualifier,
				Map<String, Filter> enabledFilters,
				boolean onlyApplyLoadByKeyFilters,
				Set<String> treatAsDeclarations,
				SqlAstCreationState creationState) {

		}

		@Override
		public boolean hasWhereRestrictions() {
			return false;
		}

		@Override
		public void applyWhereRestrictions(Consumer<Predicate> predicateConsumer, TableGroup tableGroup, boolean useQualifier, SqlAstCreationState creationState) {

		}
	}
}
