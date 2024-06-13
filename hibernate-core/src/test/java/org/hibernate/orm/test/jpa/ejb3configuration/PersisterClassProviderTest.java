/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.bytecode.internal.BytecodeEnhancementMetadataNonPojoImpl;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.cfg.AvailableSettings;
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
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
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
import org.hibernate.orm.test.jpa.SettingsGenerator;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyEntry;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

/**
 * @author Emmanuel Bernard
 */
public class PersisterClassProviderTest {
	@Test
	public void testPersisterClassProvider() {
		Map settings = SettingsGenerator.generateSettings(
				PersisterClassResolverInitiator.IMPL_NAME, GoofyPersisterClassProvider.class,
				AvailableSettings.LOADED_CLASSES, Arrays.asList( Bell.class )
		);
		ServiceRegistryUtil.applySettings( settings );
		try {
			EntityManagerFactory entityManagerFactory = Bootstrap.getEntityManagerFactoryBuilder(
					new PersistenceUnitDescriptorAdapter(),
					settings
			).build();
			entityManagerFactory.close();
		}
		catch ( PersistenceException e ) {
            Assertions.assertNotNull( e.getCause() );
			Assertions.assertNotNull( e.getCause().getCause() );
			Assertions.assertEquals( GoofyException.class, e.getCause().getCause().getClass() );

		}
	}

	public static class GoofyPersisterClassProvider implements PersisterClassResolver {
		@Override
		public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata) {
			return GoofyProvider.class;
		}

		@Override
		public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
			return null;
		}
	}

	public static class GoofyProvider implements EntityPersister {

		@SuppressWarnings( {"UnusedParameters"})
		public GoofyProvider(
				PersistentClass persistentClass,
				EntityDataAccess entityDataAccessstrategy,
				NaturalIdDataAccess naturalIdRegionAccessStrategy,
				PersisterCreationContext creationContext) {
			throw new GoofyException();
		}

		@Override
		public BytecodeEnhancementMetadata getInstrumentationMetadata() {
			return new BytecodeEnhancementMetadataNonPojoImpl( getEntityName() );
		}

		@Override
		public void postInstantiate() throws MappingException {

		}

		@Override
		public SessionFactoryImplementor getFactory() {
			return null;
		}

		@Override
		public NavigableRole getNavigableRole() {
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

		public Comparator getVersionComparator() {
			return null;
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
		public Serializable loadEntityIdByNaturalId(Object[] naturalIdValues, LockOptions lockOptions,
				SharedSessionContractImplementor session) {
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
		public List multiLoad(Object[] ids, EventSource session, MultiIdLoadOptions loadOptions) {
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
		public CacheEntryStructure getCacheEntryStructure() {
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
			throw new UnsupportedOperationException( "Not supported" );
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
		public EntityRepresentationStrategy getRepresentationStrategy() {
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
			return false;  //To change body of implemented methods use File | Settings | File Templates.
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
		public CacheEntry buildCacheEntry(Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public EntityPersister getEntityPersister() {
			return this;
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

	public static class GoofyException extends RuntimeException {

	}
}
