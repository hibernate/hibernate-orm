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

import org.hibernate.EntityMode;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.PluralAttribute;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.source.MetaAttributeContext;
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
	private final EntityBinding superEntityBinding;
	private final HierarchyDetails hierarchyDetails;

	private Entity entity;
	private TableSpecification baseTable;

	private Value<Class<?>> proxyInterfaceType;

	private String jpaEntityName;

	private Class<? extends EntityPersister> customEntityPersisterClass;
	private Class<? extends EntityTuplizer> customEntityTuplizerClass;

	private String discriminatorMatchValue;

	private Map<String, AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();

	private Set<FilterDefinition> filterDefinitions = new HashSet<FilterDefinition>();
	private Set<SingularAssociationAttributeBinding> entityReferencingAttributeBindings = new HashSet<SingularAssociationAttributeBinding>();

	/**
	 * Used to instantiate the EntityBinding for an entity that is the root of an inheritance hierarchy
	 *
	 * @param inheritanceType The inheritance type for the hierarchy
	 * @param entityMode The entity mode used in this hierarchy.
	 */
	public EntityBinding(InheritanceType inheritanceType, EntityMode entityMode) {
		this.superEntityBinding = null;
		this.hierarchyDetails = new HierarchyDetails( this, inheritanceType, entityMode );
	}

	public EntityBinding(EntityBinding superEntityBinding) {
		this.superEntityBinding = superEntityBinding;
		this.hierarchyDetails = superEntityBinding.getHierarchyDetails();
	}

	private MetaAttributeContext metaAttributeContext;

	private boolean lazy;
	private boolean mutable;
	private String whereFilter;
	private String rowId;

	private boolean dynamicUpdate;
	private boolean dynamicInsert;

	private int batchSize;
	private boolean selectBeforeUpdate;
	private boolean hasSubselectLoadableCollections;

	private Boolean isAbstract;

	private String customLoaderName;
	private CustomSQL customInsert;
	private CustomSQL customUpdate;
	private CustomSQL customDelete;

	private Set<String> synchronizedTableNames = new HashSet<String>();


	public HierarchyDetails getHierarchyDetails() {
		return hierarchyDetails;
	}

	public EntityBinding getSuperEntityBinding() {
		return superEntityBinding;
	}

	public boolean isRoot() {
		return superEntityBinding == null;
	}

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

	public TableSpecification getTable(String containingTableName) {
		// todo : implement this for secondary table look ups.  for now we just return the base table
		return baseTable;
	}

	public boolean isVersioned() {
		return getHierarchyDetails().getVersioningAttributeBinding() != null;
	}

	public String getDiscriminatorMatchValue() {
		return discriminatorMatchValue;
	}

	public void setDiscriminatorMatchValue(String discriminatorMatchValue) {
		this.discriminatorMatchValue = discriminatorMatchValue;
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

	public Iterable<SingularAssociationAttributeBinding> getEntityReferencingAttributeBindings() {
		return entityReferencingAttributeBindings;
	}

	public SimpleSingularAttributeBinding makeSimpleAttributeBinding(SingularAttribute attribute) {
		return makeSimpleAttributeBinding( attribute, false, false );
	}

	private SimpleSingularAttributeBinding makeSimpleAttributeBinding(SingularAttribute attribute, boolean forceNonNullable, boolean forceUnique) {
		final SimpleSingularAttributeBinding binding = new SimpleSingularAttributeBinding(
				this,
				attribute,
				forceNonNullable,
				forceUnique
		);
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	public ManyToOneAttributeBinding makeManyToOneAttributeBinding(SingularAttribute attribute) {
		final ManyToOneAttributeBinding binding = new ManyToOneAttributeBinding( this, attribute );
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	public BagBinding makeBagAttributeBinding(PluralAttribute attribute, CollectionElementNature nature) {
		final BagBinding binding = new BagBinding( this, attribute, nature );
		registerAttributeBinding( attribute.getName(), binding );
		return binding;
	}

	private void registerAttributeBinding(String name, SingularAssociationAttributeBinding attributeBinding) {
		entityReferencingAttributeBindings.add( attributeBinding );
		registerAttributeBinding( name, (AttributeBinding) attributeBinding );
	}

	private void registerAttributeBinding(String name, AttributeBinding attributeBinding) {
		attributeBindingMap.put( name, attributeBinding );
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

	public String getRowId() {
		return rowId;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
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

	public void addSynchronizedTableNames(java.util.Collection<String> synchronizedTableNames) {
		this.synchronizedTableNames.addAll( synchronizedTableNames );
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntityBinding" );
		sb.append( "{entity=" ).append( entity != null ? entity.getName() : "not set" );
		sb.append( '}' );
		return sb.toString();
	}
}
