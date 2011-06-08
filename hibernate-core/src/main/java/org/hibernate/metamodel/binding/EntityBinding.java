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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.metamodel.binding.state.EntityBindingState;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;

/**
 * Provides the link between the domain and the relational model for an entity.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class EntityBinding {
	private final EntityIdentifier entityIdentifier = new EntityIdentifier( this );

	private boolean isRoot;

	private InheritanceType entityInheritanceType;
	private EntityDiscriminator entityDiscriminator;
	private SimpleAttributeBinding versionBinding;

	private Entity entity;
	private TableSpecification baseTable;

	private Map<String, AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();
	private Set<EntityReferencingAttributeBinding> entityReferencingAttributeBindings = new HashSet<EntityReferencingAttributeBinding>();

	private Caching caching;

	private MetaAttributeContext metaAttributeContext;

	private String proxyInterfaceName;
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
	private int optimisticLockMode;

	private Class entityPersisterClass;
	private Boolean isAbstract;

	private CustomSQL customInsert;
	private CustomSQL customUpdate;
	private CustomSQL customDelete;

	private List<String> synchronizedTableNames;

	public EntityBinding initialize(EntityBindingState state) {
		this.isRoot = state.isRoot();
		this.entityInheritanceType = state.getEntityInheritanceType();
		this.caching = state.getCaching();
		this.metaAttributeContext = state.getMetaAttributeContext();
		this.proxyInterfaceName = state.getProxyInterfaceName();
		this.lazy = state.isLazy();
		this.mutable = state.isMutable();
		this.explicitPolymorphism = state.isExplicitPolymorphism();
		this.whereFilter = state.getWhereFilter();
		this.rowId = state.getRowId();
		this.dynamicInsert = state.isDynamicUpdate();
		this.dynamicInsert = state.isDynamicInsert();
		this.batchSize = state.getBatchSize();
		this.selectBeforeUpdate = state.isSelectBeforeUpdate();
		this.optimisticLockMode = state.getOptimisticLockMode();
		this.entityPersisterClass = state.getEntityPersisterClass();
		this.isAbstract = state.isAbstract();
		this.customInsert = state.getCustomInsert();
		this.customUpdate = state.getCustomUpdate();
		this.customDelete = state.getCustomDelete();
		if ( state.getSynchronizedTableNames() != null ) {
			for ( String synchronizedTableName : state.getSynchronizedTableNames() ) {
				addSynchronizedTable( synchronizedTableName );
			}
		}
		return this;
	}

	public boolean isRoot() {
		return isRoot;
	}

	public void setRoot(boolean isRoot) {
		this.isRoot = isRoot;
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

	public SimpleAttributeBinding getVersioningValueBinding() {
		return versionBinding;
	}

	public Iterable<AttributeBinding> getAttributeBindings() {
		return attributeBindingMap.values();
	}

	public AttributeBinding getAttributeBinding(String name) {
		return attributeBindingMap.get( name );
	}

	public Iterable<EntityReferencingAttributeBinding> getEntityReferencingAttributeBindings() {
		return entityReferencingAttributeBindings;
	}

	public SimpleAttributeBinding makeSimpleIdAttributeBinding(String name) {
		final SimpleAttributeBinding binding = makeSimpleAttributeBinding( name, true, true );
		getEntityIdentifier().setValueBinding( binding );
		return binding;
	}

	public EntityDiscriminator makeEntityDiscriminator(String attributeName) {
		if ( entityDiscriminator != null ) {
			throw new AssertionFailure( "Creation of entity discriminator was called more than once" );
		}
		entityDiscriminator = new EntityDiscriminator();
		entityDiscriminator.setValueBinding( makeSimpleAttributeBinding( attributeName, true, false ) );
		return entityDiscriminator;
	}

	public SimpleAttributeBinding makeVersionBinding(String attributeName) {
		versionBinding = makeSimpleAttributeBinding( attributeName, true, false );
		return versionBinding;
	}

	public SimpleAttributeBinding makeSimpleAttributeBinding(String name) {
		return makeSimpleAttributeBinding( name, false, false );
	}

	private SimpleAttributeBinding makeSimpleAttributeBinding(String name, boolean forceNonNullable, boolean forceUnique) {
		final SimpleAttributeBinding binding = new SimpleAttributeBinding( this, forceNonNullable, forceUnique );
		registerAttributeBinding( name, binding );
		binding.setAttribute( entity.getAttribute( name ) );
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

	public void setProxyInterfaceName(String proxyInterfaceName) {
		this.proxyInterfaceName = proxyInterfaceName;
	}

	public String getProxyInterfaceName() {
		return proxyInterfaceName;
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

	public void setSelectBeforeUpdate(Boolean selectBeforeUpdate) {
		this.selectBeforeUpdate = selectBeforeUpdate;
	}

	public boolean hasSubselectLoadableCollections() {
		return hasSubselectLoadableCollections;
	}

	/* package-protected */
	void setSubselectLoadableCollections(boolean hasSubselectLoadableCollections) {
		this.hasSubselectLoadableCollections = hasSubselectLoadableCollections;
	}

	public int getOptimisticLockMode() {
		return optimisticLockMode;
	}

	public void setOptimisticLockMode(int optimisticLockMode) {
		this.optimisticLockMode = optimisticLockMode;
	}

	public Class getEntityPersisterClass() {
		return entityPersisterClass;
	}

	public void setEntityPersisterClass(Class entityPersisterClass) {
		this.entityPersisterClass = entityPersisterClass;
	}

	public Boolean isAbstract() {
		return isAbstract;
	}

	protected void addSynchronizedTable(String tablename) {
		if ( synchronizedTableNames == null ) {
			synchronizedTableNames = new ArrayList<String>();
		}
		synchronizedTableNames.add( tablename );
	}


	// Custom SQL ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private String loaderName;

	public String getLoaderName() {
		return loaderName;
	}

	public void setLoaderName(String loaderName) {
		this.loaderName = loaderName;
	}

	public CustomSQL getCustomInsert() {
		return customInsert;
	}

	public CustomSQL getCustomUpdate() {
		return customUpdate;
	}

	public CustomSQL getCustomDelete() {
		return customDelete;
	}
}
