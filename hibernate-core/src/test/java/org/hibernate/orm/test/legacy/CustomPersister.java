/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.legacy;

import java.io.Serializable;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
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
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
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
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyEntry;
import org.hibernate.persister.entity.mutation.DeleteCoordinator;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.persister.entity.mutation.InsertCoordinator;
import org.hibernate.persister.entity.mutation.UpdateCoordinator;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;

import org.checkerframework.checker.nullness.qual.Nullable;

public class CustomPersister implements EntityPersister {

	private static final Hashtable<Object,Object> INSTANCES = new Hashtable<>();
	private static final IdentifierGenerator GENERATOR = new UUIDHexGenerator();

	private final SessionFactoryImplementor factory;
	private final EntityMetamodel entityMetamodel;

	@SuppressWarnings("UnusedParameters")
	public CustomPersister(
			PersistentClass model,
			EntityDataAccess cacheAccessStrategy,
			NaturalIdDataAccess naturalIdRegionAccessStrategy,
			PersisterCreationContext creationContext) {
		this.factory = creationContext.getSessionFactory();
		this.entityMetamodel = new EntityMetamodel( model, this, creationContext );
	}

	public boolean hasLazyProperties() {
		return false;
	}

	public boolean isInherited() {
		return false;
	}

	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return new NavigableRole( getEntityName() );
	}

	@Override
	public EntityEntryFactory getEntityEntryFactory() {
		return MutableEntityEntryFactory.INSTANCE;
	}

	@Override
	public Class<?> getMappedClass() {
		return Custom.class;
	}

	public void postInstantiate() throws MappingException {}

	public String getEntityName() {
		return Custom.class.getName();
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
		throw new UnsupportedOperationException();
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

	public boolean isSubclassEntityName(String entityName) {
		return Custom.class.getName().equals(entityName);
	}

	public boolean hasProxy() {
		return false;
	}

	public boolean hasCollections() {
		return false;
	}

	public boolean hasCascades() {
		return false;
	}

	public boolean isMutable() {
		return true;
	}

	public boolean isSelectBeforeUpdateRequired() {
		return false;
	}

	public boolean isIdentifierAssignedByInsert() {
		return false;
	}

	public Boolean isTransient(Object object, SharedSessionContractImplementor session) {
		return ( (Custom) object ).id==null;
	}

	@Override
	public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SharedSessionContractImplementor session) {
		return getPropertyValues( object );
	}

	public void processInsertGeneratedProperties(Object id, Object entity, Object[] state, GeneratedValues generatedValues, SharedSessionContractImplementor session) {
	}

	public void processUpdateGeneratedProperties(Object id, Object entity, Object[] state, GeneratedValues generatedValues, SharedSessionContractImplementor session) {
	}

	@Override
	public boolean implementsLifecycle() {
		return false;
	}

	@Override
	public Class<?> getConcreteProxyClass() {
		return Custom.class;
	}

	@Override
	public void setPropertyValues(Object object, Object[] values) {
		setPropertyValue( object, 0, values[0] );
	}

	@Override
	public void setPropertyValue(Object object, int i, Object value) {
		( (Custom) object ).setName( (String) value );
	}

	@Override
	public Object[] getPropertyValues(Object object) throws HibernateException {
		Custom c = (Custom) object;
		return new Object[] { c.getName() };
	}

	@Override
	public Object getPropertyValue(Object object, int i) throws HibernateException {
		return ( (Custom) object ).getName();
	}

	@Override
	public Object getPropertyValue(Object object, String propertyName) throws HibernateException {
		return ( (Custom) object ).getName();
	}

	@Override
	public Serializable getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return ( (Custom) entity ).id;
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		( (Custom) entity ).id = (String) id;
	}

	@Override
	public Object getVersion(Object object) throws HibernateException {
		return null;
	}

	@Override
	public Object instantiate(Object id, SharedSessionContractImplementor session) {
		Custom c = new Custom();
		c.id = (String) id;
		return c;
	}

	@Override
	public boolean isInstance(Object object) {
		return object instanceof Custom;
	}

	@Override
	public boolean hasUninitializedLazyProperties(Object object) {
		return false;
	}

	@Override
	public void resetIdentifier(Object entity, Object currentId, Object currentVersion, SharedSessionContractImplementor session) {
		( ( Custom ) entity ).id = ( String ) currentId;
	}

	public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory) {
		return this;
	}

	public int[] findDirty(
		Object[] x,
		Object[] y,
		Object owner,
		SharedSessionContractImplementor session) throws HibernateException {
		if ( !Objects.equals( x[0], y[0] ) ) {
			return new int[] { 0 };
		}
		else {
			return null;
		}
	}

	public int[] findModified(
		Object[] x,
		Object[] y,
		Object owner,
		SharedSessionContractImplementor session) throws HibernateException {
		if ( !Objects.equals( x[0], y[0] ) ) {
			return new int[] { 0 };
		}
		else {
			return null;
		}
	}

	/**
	 * @see EntityPersister#hasIdentifierProperty()
	 */
	public boolean hasIdentifierProperty() {
		return true;
	}

	/**
	 * @see EntityPersister#isVersioned()
	 */
	public boolean isVersioned() {
		return false;
	}

	/**
	 * @see EntityPersister#getVersionType()
	 */
	public BasicType<?> getVersionType() {
		return null;
	}

	/**
	 * @see EntityPersister#getVersionProperty()
	 */
	public int getVersionProperty() {
		return 0;
	}

	/**
	 * @see EntityPersister#getIdentifierGenerator()
	 */
	public IdentifierGenerator getIdentifierGenerator()
	throws HibernateException {
		return GENERATOR;
	}

	/**
	 * @see EntityPersister#load(Object, Object, LockOptions, SharedSessionContractImplementor)
	 */
	public Object load(
			Object id,
			Object optionalObject,
			LockOptions lockOptions,
			SharedSessionContractImplementor session
	) throws HibernateException {
		return load(id, optionalObject, lockOptions.getLockMode(), session);
	}

	@Override
	public List<?> multiLoad(Object[] ids, EventSource session, MultiIdLoadOptions loadOptions) {
		return Collections.emptyList();
	}

	/**
	 * @see EntityPersister#load(Object, Object, LockMode, SharedSessionContractImplementor)
	 */
	public Object load(
			Object id,
			Object optionalObject,
			LockMode lockMode,
			SharedSessionContractImplementor session) {

		throw new UnsupportedOperationException();
//
//		// fails when optional object is supplied
//
//		Custom clone = null;
//		Custom obj = (Custom) INSTANCES.get(id);
//		if (obj!=null) {
//			clone = (Custom) obj.clone();
//			TwoPhaseLoad.addUninitializedEntity(
//					session.generateEntityKey( id, this ),
//					clone,
//					this,
//					LockMode.NONE,
//					session
//			);
//			TwoPhaseLoad.postHydrate(
//					this,
//					id,
//					new String[] { obj.getName() },
//					null,
//					clone,
//					LockMode.NONE,
//					session
//			);
//			TwoPhaseLoad.initializeEntity(
//					clone,
//					false,
//					session,
//					new PreLoadEvent( (EventSource) session )
//			);
//			TwoPhaseLoad.afterInitialize( clone, session );
//			TwoPhaseLoad.postLoad( clone, session, new PostLoadEvent( (EventSource) session ) );
//		}
//		return clone;
	}

	/**
	 * @see EntityPersister#lock(Object, Object, Object, LockMode, EventSource)
	 */
	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			LockOptions lockOptions,
			EventSource session
	) throws HibernateException {

		throw new UnsupportedOperationException();
	}

	/**
	 * @see EntityPersister#lock(Object, Object, Object, LockMode, EventSource)
	 */
	public void lock(
			Object id,
			Object version,
			Object object,
			LockMode lockMode,
			EventSource session
	) throws HibernateException {

		throw new UnsupportedOperationException();
	}

	@Override
	public InsertCoordinator getInsertCoordinator() {
		return new InsertCoordinator() {
			@Override
			public @Nullable GeneratedValues insert(
					Object entity,
					Object[] values,
					SharedSessionContractImplementor session) {
				throw new UnsupportedOperationException();
			}

			@Override
			public @Nullable GeneratedValues insert(
					Object entity,
					Object id,
					Object[] values,
					SharedSessionContractImplementor session) {
				INSTANCES.put( id, ( (Custom) entity ).clone() );
				return null;
			}

			@Override
			public MutationOperationGroup getStaticMutationOperationGroup() {
				return null;
			}
		};
	}

	@Override
	public UpdateCoordinator getUpdateCoordinator() {
		return new UpdateCoordinator() {
			@Override
			public @Nullable GeneratedValues update(
					Object entity,
					Object id,
					Object rowId,
					Object[] values,
					Object oldVersion,
					Object[] incomingOldValues,
					int[] dirtyAttributeIndexes,
					boolean hasDirtyCollection,
					SharedSessionContractImplementor session) {
				INSTANCES.put( id, ( (Custom) entity ).clone() );
				return null;
			}

			@Override
			public void forceVersionIncrement(
					Object id,
					Object currentVersion,
					Object nextVersion,
					SharedSessionContractImplementor session) {
			}

			@Override
			public MutationOperationGroup getStaticMutationOperationGroup() {
				return null;
			}
		};
	}

	@Override
	public DeleteCoordinator getDeleteCoordinator() {
		return new DeleteCoordinator() {
			@Override
			public void delete(Object entity, Object id, Object version, SharedSessionContractImplementor session) {
				INSTANCES.remove( id );
			}

			@Override
			public MutationOperationGroup getStaticMutationOperationGroup() {
				return null;
			}
		};
	}

	private static final BasicType<String> STRING_TYPE = new BasicTypeImpl<>(
			StringJavaType.INSTANCE,
			VarcharJdbcType.INSTANCE
	);
	private static final Type[] TYPES = new Type[] { STRING_TYPE };
	private static final String[] NAMES = new String[] { "name" };
	private static final boolean[] MUTABILITY = new boolean[] { true };
	private static final boolean[] GENERATION = new boolean[] { false };

	public Type[] getPropertyTypes() {
		return TYPES;
	}

	public String[] getPropertyNames() {
		return NAMES;
	}

	public CascadeStyle[] getPropertyCascadeStyles() {
		return null;
	}

	public Type getIdentifierType() {
		return STRING_TYPE;
	}

	public String getIdentifierPropertyName() {
		return "id";
	}

	public boolean hasCache() {
		return false;
	}

	public EntityDataAccess getCacheAccessStrategy() {
		return null;
	}

	public boolean hasNaturalIdCache() {
		return false;
	}

	public NaturalIdDataAccess getNaturalIdCacheAccessStrategy() {
		return null;
	}

	public String getRootEntityName() {
		return "CUSTOMS";
	}

	public String[] getPropertySpaces() {
		return new String[] { "CUSTOMS" };
	}

	public Serializable[] getQuerySpaces() {
		return new String[] { "CUSTOMS" };
	}

	public ClassMetadata getClassMetadata() {
		return null;
	}

	public boolean[] getPropertyUpdateability() {
		return MUTABILITY;
	}

	public boolean[] getPropertyCheckability() {
		return MUTABILITY;
	}

	public boolean[] getPropertyInsertability() {
		return MUTABILITY;
	}

	public boolean canExtractIdOutOfEntity() {
		return true;
	}

	public Type getPropertyType(String propertyName) {
		throw new UnsupportedOperationException();
	}

	public Object createProxy(Object id, SharedSessionContractImplementor session)
		throws HibernateException {
		throw new UnsupportedOperationException("no proxy for this class");
	}

	public Object getCurrentVersion(
			Object id,
			SharedSessionContractImplementor session)
		throws HibernateException {

		return INSTANCES.get(id);
	}

	@Override
	public Object forceVersionIncrement(Object id, Object currentVersion, SharedSessionContractImplementor session)
			throws HibernateException {
		return null;
	}

	@Override
	public boolean[] getPropertyNullability() {
		return MUTABILITY;
	}

	@Override
	public boolean isCacheInvalidationRequired() {
		return false;
	}

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
	}

	@Override
	public void afterReassociate(Object entity, SharedSessionContractImplementor session) {
	}

	@Override
	public Object[] getDatabaseSnapshot(Object id, SharedSessionContractImplementor session) throws HibernateException {
		return null;
	}

	@Override
	public Object getIdByUniqueKey(Object key, String uniquePropertyName, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "not supported" );
	}

	@Override
	public boolean[] getPropertyVersionability() {
		return MUTABILITY;
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		return UnstructuredCacheEntry.INSTANCE;
	}

	@Override
	public CacheEntry buildCacheEntry(
			Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
		return new StandardCacheEntryImpl(
				state,
				this,
				version,
				session,
				entity
		);
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
	public int[] getNaturalIdentifierProperties() {
		return null;
	}

	@Override
	public boolean hasNaturalIdentifier() {
		return false;
	}

	@Override
	public boolean hasMutableProperties() {
		return false;
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
	public boolean[] getPropertyLaziness() {
		return null;
	}

	@Override
	public boolean isLazyPropertiesCacheable() {
		return true;
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
	public boolean isVersionPropertyGenerated() {
		return false;
	}

	@Override
	public Object[] getNaturalIdentifierSnapshot(Object id, SharedSessionContractImplementor session) throws HibernateException {
		return null;
	}

	@Override
	public Serializable loadEntityIdByNaturalId(Object[] naturalIdValues, LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public EntityMetamodel getEntityMetamodel() {
		return entityMetamodel;
	}

	@Override
	public BytecodeEnhancementMetadata getInstrumentationMetadata() {
		return new BytecodeEnhancementMetadataNonPojoImpl( getEntityName() );
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator(rootAlias);
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
	public String getIdentitySelectString() {
		return null;
	}

	@Override
	public String[] getIdentifierColumnNames() {
		return new String[0];
	}

	@Override
	public String[] getRootTableKeyColumnNames() {
		return new String[0];
	}

	@Override
	public boolean isAffectedByEntityGraph(LoadQueryInfluencers loadQueryInfluencers) {
		return loadQueryInfluencers.getEffectiveEntityGraph().getGraph() != null;
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers loadQueryInfluencers) {
		return false;
	}

	@Override
	public boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters) {
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
