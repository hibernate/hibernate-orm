/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.binding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * Provides the link between the domain and the relational model for an entity.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class EntityBinding {
	private Entity entity;
	private TableSpecification baseTable;

	private EntityMode entityMode;
	private Value<Class<?>> proxyInterfaceType;

	private String jpaEntityName;

	private Class<? extends EntityPersister> customEntityPersisterClass;
	private Class<? extends EntityTuplizer> customEntityTuplizerClass;

	private boolean isRoot;
	private InheritanceType entityInheritanceType;

	private final EntityIdentifier entityIdentifier = new EntityIdentifier( this );
	private EntityDiscriminator entityDiscriminator;
	private SimpleAttributeBinding versionBinding;

	private Map<String, AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();
	private Set<FilterDefinition> filterDefinitions = new HashSet<FilterDefinition>( );
	private Set<EntityReferencingAttributeBinding> entityReferencingAttributeBindings = new HashSet<EntityReferencingAttributeBinding>();

	private Caching caching;

	private MetaAttributeContext metaAttributeContext;

	private boolean lazy;
	private boolean mutable;
	private boolean explicitPolymorphism;
	private String whereFilter;
	private String rowId;

	private boolean dynamicUpdate;
	private boolean dynamicInsert;

	private int batchSize;
	private boolean selectBeforeUpdate;
	private boolean hasSubselectLoadableCollections;
	private OptimisticLockStyle optimisticLockStyle;

	private Boolean isAbstract;

	private String customLoaderName;
	private CustomSQL customInsert;
	private CustomSQL customUpdate;
	private CustomSQL customDelete;

	private Set<String> synchronizedTableNames = new HashSet<String>();

//	public EntityBinding initialize(BindingContext bindingContext, EntityDescriptor state) {
//		// todo : Entity will need both entityName and className to be effective
//		this.entity = new Entity( state.getEntityName(), state.getSuperType(), bindingContext.makeJavaType( state.getClassName() ) );
//
//		this.isRoot = state.isRoot();
//		this.entityInheritanceType = state.getEntityInheritanceType();
//
//		this.entityMode = state.getEntityMode();
//		this.jpaEntityName = state.getJpaEntityName();
//
//		// todo : handle the entity-persister-resolver stuff
//		this.customEntityPersisterClass = state.getCustomEntityPersisterClass();
//		this.customEntityTuplizerClass = state.getCustomEntityTuplizerClass();
//
//		this.caching = state.getCaching();
//		this.metaAttributeContext = state.getMetaAttributeContext();
//
//		if ( entityMode == EntityMode.POJO ) {
//			if ( state.getProxyInterfaceName() != null ) {
//				this.proxyInterfaceType = bindingContext.makeJavaType( state.getProxyInterfaceName() );
//				this.lazy = true;
//			}
//			else if ( state.isLazy() ) {
//				this.proxyInterfaceType = entity.getJavaType();
//				this.lazy = true;
//			}
//		}
//		else {
//			this.proxyInterfaceType = new JavaType( Map.class );
//			this.lazy = state.isLazy();
//		}
//
//		this.mutable = state.isMutable();
//		this.explicitPolymorphism = state.isExplicitPolymorphism();
//		this.whereFilter = state.getWhereFilter();
//		this.rowId = state.getRowId();
//		this.dynamicUpdate = state.isDynamicUpdate();
//		this.dynamicInsert = state.isDynamicInsert();
//		this.batchSize = state.getBatchSize();
//		this.selectBeforeUpdate = state.isSelectBeforeUpdate();
//		this.optimisticLockMode = state.getOptimisticLockMode();
//		this.isAbstract = state.isAbstract();
//		this.customInsert = state.getCustomInsert();
//		this.customUpdate = state.getCustomUpdate();
//		this.customDelete = state.getCustomDelete();
//		if ( state.getSynchronizedTableNames() != null ) {
//			for ( String synchronizedTableName : state.getSynchronizedTableNames() ) {
//				addSynchronizedTable( synchronizedTableName );
//			}
//		}
//		return this;
//	}

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public TableSpecification getBaseTable() {
		return baseTable;
	}

	public void setBaseTable(TableSpecification baseTable) {
		this.baseTable = baseTable;
	}

	public boolean isRoot() {
		return isRoot;
	}

	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}

	public EntityIdentifier getEntityIdentifier() {
		return entityIdentifier;
	}

	public void bindEntityIdentifier(SimpleAttributeBinding attributeBinding) {
		if ( !Column.class.isInstance( attributeBinding.getValue() ) ) {
			throw new MappingException(
					"Identifier value must be a Column; instead it is: " + attributeBinding.getValue().getClass()
			);
		}
		entityIdentifier.setValueBinding( attributeBinding );
		baseTable.getPrimaryKey().addColumn( Column.class.cast( attributeBinding.getValue() ) );
	}

	public EntityDiscriminator getEntityDiscriminator() {
		return entityDiscriminator;
	}

	public void setInheritanceType(InheritanceType entityInheritanceType) {
		this.entityInheritanceType = entityInheritanceType;
	}

	public InheritanceType getInheritanceType() {
		return entityInheritanceType;
	}

	public boolean isVersioned() {
		return versionBinding != null;
	}

	public void setVersionBinding(SimpleAttributeBinding versionBinding) {
		this.versionBinding = versionBinding;
	}

	public SimpleAttributeBinding getVersioningValueBinding() {
		return versionBinding;
	}

	public Iterable<AttributeBinding> getAttributeBindings() {
		return attributeBindingMap.values();
	}

	public AttributeBinding getAttributeBinding(String name) {
		return attributeBindingMap.get( name );
	}

	/**
	 * Gets the number of attribute bindings defined on this class, including the
	 * identifier attribute binding and attribute bindings defined
	 * as part of a join.
	 *
	 * @return The number of attribute bindings
	 */
	public int getAttributeBindingClosureSpan() {
		// TODO: fix this after HHH-6337 is fixed; for now just return size of attributeBindingMap
		// if this is not a root, then need to include the superclass attribute bindings
		return attributeBindingMap.size();
	}

	/**
	 * Gets the attribute bindings defined on this class, including the
	 * identifier attribute binding and attribute bindings defined
	 * as part of a join.
	 *
	 * @return The attribute bindings.
	 */
	public Iterable<AttributeBinding> getAttributeBindingClosure() {
		// TODO: fix this after HHH-6337 is fixed. for now, just return attributeBindings
		// if this is not a root, then need to include the superclass attribute bindings
		return getAttributeBindings();
	}

	public Iterable<FilterDefinition> getFilterDefinitions() {
		return filterDefinitions;
	}

	public void addFilterDefinition(FilterDefinition filterDefinition) {
		filterDefinitions.add( filterDefinition );
	}

	public Iterable<EntityReferencingAttributeBinding> getEntityReferencingAttributeBindings() {
		return entityReferencingAttributeBindings;
	}

	public SimpleAttributeBinding makeSimpleIdAttributeBinding(Attribute attribute) {
		final SimpleAttributeBinding binding = makeSimpleAttributeBinding( attribute, true, true );
		getEntityIdentifier().setValueBinding( binding );
		return binding;
	}

	public EntityDiscriminator makeEntityDiscriminator(Attribute attribute) {
		if ( entityDiscriminator != null ) {
			throw new AssertionFailure( "Creation of entity discriminator was called more than once" );
		}
		entityDiscriminator = new EntityDiscriminator();
		entityDiscriminator.setValueBinding( makeSimpleAttributeBinding( attribute, true, false ) );
		return entityDiscriminator;
	}

	public SimpleAttributeBinding makeVersionBinding(Attribute attribute) {
		versionBinding = makeSimpleAttributeBinding( attribute, true, false );
		return versionBinding;
	}

	public SimpleAttributeBinding makeSimpleAttributeBinding(Attribute attribute) {
		return makeSimpleAttributeBinding( attribute, false, false );
	}

	private SimpleAttributeBinding makeSimpleAttributeBinding(Attribute attribute, boolean forceNonNullable, boolean forceUnique) {
		final SimpleAttributeBinding binding = new SimpleAttributeBinding( this, forceNonNullable, forceUnique );
		registerAttributeBinding( attribute.getName(), binding );
		binding.setAttribute( attribute );
		return binding;
	}

	public ManyToOneAttributeBinding makeManyToOneAttributeBinding(String attributeName) {
		final ManyToOneAttributeBinding binding = new ManyToOneAttributeBinding( this );
		registerAttributeBinding( attributeName, binding );
		binding.setAttribute( entity.getAttribute( attributeName ) );
		return binding;
	}

	public BagBinding makeBagAttributeBinding(String attributeName, CollectionElementType collectionElementType) {
		final BagBinding binding = new BagBinding( this, collectionElementType );
		registerAttributeBinding( attributeName, binding );
		binding.setAttribute( entity.getAttribute( attributeName ) );
		return binding;
	}

	private void registerAttributeBinding(String name, EntityReferencingAttributeBinding attributeBinding) {
		entityReferencingAttributeBindings.add( attributeBinding );
		registerAttributeBinding( name, (AttributeBinding) attributeBinding );
	}

	private void registerAttributeBinding(String name, AttributeBinding attributeBinding) {
		attributeBindingMap.put( name, attributeBinding );
	}

	public Caching getCaching() {
		return caching;
	}

	public void setCaching(Caching caching) {
		this.caching = caching;
	}

	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	public void setMetaAttributeContext(MetaAttributeContext metaAttributeContext) {
		this.metaAttributeContext = metaAttributeContext;
	}

	public boolean isMutable() {
		return mutable;
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public Value<Class<?>> getProxyInterfaceType() {
		return proxyInterfaceType;
	}

	public void setProxyInterfaceType(Value<Class<?>> proxyInterfaceType) {
		this.proxyInterfaceType = proxyInterfaceType;
	}

	public String getWhereFilter() {
		return whereFilter;
	}

	public void setWhereFilter(String whereFilter) {
		this.whereFilter = whereFilter;
	}

	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
		this.explicitPolymorphism = explicitPolymorphism;
	}

	public String getRowId() {
		return rowId;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	public String getDiscriminatorValue() {
		return entityDiscriminator == null ? null : entityDiscriminator.getDiscriminatorValue();
	}

	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	public void setDynamicUpdate(boolean dynamicUpdate) {
		this.dynamicUpdate = dynamicUpdate;
	}

	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	public void setDynamicInsert(boolean dynamicInsert) {
		this.dynamicInsert = dynamicInsert;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	public void setSelectBeforeUpdate(boolean selectBeforeUpdate) {
		this.selectBeforeUpdate = selectBeforeUpdate;
	}

	public boolean hasSubselectLoadableCollections() {
		return hasSubselectLoadableCollections;
	}

	/* package-protected */
	void setSubselectLoadableCollections(boolean hasSubselectLoadableCollections) {
		this.hasSubselectLoadableCollections = hasSubselectLoadableCollections;
	}

	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	public void setOptimisticLockStyle(OptimisticLockStyle optimisticLockStyle) {
		this.optimisticLockStyle = optimisticLockStyle;
	}

	public Class<? extends EntityPersister> getCustomEntityPersisterClass() {
		return customEntityPersisterClass;
	}

	public void setCustomEntityPersisterClass(Class<? extends EntityPersister> customEntityPersisterClass) {
		this.customEntityPersisterClass = customEntityPersisterClass;
	}

	public Class<? extends EntityTuplizer> getCustomEntityTuplizerClass() {
		return customEntityTuplizerClass;
	}

	public void setCustomEntityTuplizerClass(Class<? extends EntityTuplizer> customEntityTuplizerClass) {
		this.customEntityTuplizerClass = customEntityTuplizerClass;
	}

	public Boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(Boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public Set<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	public void addSynchronizedTable(String tableName) {
		synchronizedTableNames.add( tableName );
	}

	public void addSynchronizedTableNames(java.util.Collection<String> synchronizedTableNames) {
		this.synchronizedTableNames.addAll( synchronizedTableNames );
	}

	public EntityMode getEntityMode() {
		return entityMode;
	}

	public void setEntityMode(EntityMode entityMode) {
		this.entityMode = entityMode;
	}

	public String getJpaEntityName() {
		return jpaEntityName;
	}

	public void setJpaEntityName(String jpaEntityName) {
		this.jpaEntityName = jpaEntityName;
	}

	public String getCustomLoaderName() {
		return customLoaderName;
	}

	public void setCustomLoaderName(String customLoaderName) {
		this.customLoaderName = customLoaderName;
	}

	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public void setCustomInsert(CustomSQL customInsert) {
		this.customInsert = customInsert;
	}

	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public void setCustomUpdate(CustomSQL customUpdate) {
		this.customUpdate = customUpdate;
	}

	public CustomSQL getCustomDelete() {
		return customDelete;
	}

	public void setCustomDelete(CustomSQL customDelete) {
		this.customDelete = customDelete;
	}
}
