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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;

import org.hibernate.MappingException;
import org.hibernate.engine.Versioning;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class EntityBinding {
	private final EntityIdentifier entityIdentifier = new EntityIdentifier( this );
	private EntityDiscriminator entityDiscriminator;
	private SimpleAttributeBinding versionBinding;

	private Entity entity;
	private TableSpecification baseTable;

	private Map<String,AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();

	private Caching caching;

	private Map<String, MetaAttribute> metaAttributes;

	private boolean lazy;
	private boolean mutable;
	private boolean explicitPolymorphism;
	private String whereFilter;
	private String rowId;

	private String discriminatorValue;
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

	// TODO: change to initialize from DomainState
	public void fromHbmXml(MappingDefaults defaults, Element node, Entity entity) {
		this.entity = entity;
		metaAttributes = HbmHelper.extractMetas( node, true, defaults.getMappingMetas() );

		// go ahead and set the lazy here, since pojo.proxy can override it.
		lazy = DomHelper.extractBooleanAttributeValue( node, "lazy", defaults.isDefaultLazy()  );

		discriminatorValue = DomHelper.extractAttributeValue( node, "discriminator-value", entity.getName() );
		dynamicUpdate = DomHelper.extractBooleanAttributeValue( node, "dynamic-update", false );
		dynamicInsert = DomHelper.extractBooleanAttributeValue( node, "dynamic-insert", false );
		batchSize = DomHelper.extractIntAttributeValue( node, "batch-size", 0 );
		selectBeforeUpdate = DomHelper.extractBooleanAttributeValue( node, "select-before-update", false );

		// OPTIMISTIC LOCK MODE
		String optimisticLockModeString = DomHelper.extractAttributeValue( node,  "optimistic-lock", "version" );
		if ( "version".equals( optimisticLockModeString ) ) {
			optimisticLockMode = Versioning.OPTIMISTIC_LOCK_VERSION;
		}
		else if ( "dirty".equals( optimisticLockModeString ) ) {
			optimisticLockMode = Versioning.OPTIMISTIC_LOCK_DIRTY;
		}
		else if ( "all".equals( optimisticLockModeString ) ) {
			optimisticLockMode = Versioning.OPTIMISTIC_LOCK_ALL;
		}
		else if ( "none".equals( optimisticLockModeString ) ) {
			optimisticLockMode = Versioning.OPTIMISTIC_LOCK_NONE;
		}
		else {
			throw new MappingException( "Unsupported optimistic-lock style: " + optimisticLockModeString );
		}

		// PERSISTER
		Attribute persisterNode = node.attribute( "persister" );
		if ( persisterNode != null ) {
			try {
				entityPersisterClass = ReflectHelper.classForName( persisterNode.getValue() );
			}
			catch (ClassNotFoundException cnfe) {
				throw new MappingException( "Could not find persister class: "
					+ persisterNode.getValue() );
			}
		}

		// CUSTOM SQL
		customInsert = HbmHelper.getCustomSql( node.element( "sql-insert" ) );
		customDelete = HbmHelper.getCustomSql( node.element( "sql-delete" ) );
		customUpdate = HbmHelper.getCustomSql( node.element( "sql-update" ) );

		Iterator tables = node.elementIterator( "synchronize" );
		while ( tables.hasNext() ) {
			addSynchronizedTable( ( ( Element ) tables.next() ).attributeValue( "table" ) );
		}

		isAbstract = DomHelper.extractBooleanAttributeValue( node, "abstract", false );
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

	public EntityDiscriminator makeEntityDiscriminator() {
		entityDiscriminator = new EntityDiscriminator( this );
		return entityDiscriminator;
	}

	public EntityDiscriminator getEntityDiscriminator() {
		return entityDiscriminator;
	}

	public SimpleAttributeBinding getVersioningValueBinding() {
		return versionBinding;
	}

	public void setVersioningValueBinding(SimpleAttributeBinding versionBinding) {
		this.versionBinding = versionBinding;
	}

	public Iterable<AttributeBinding> getAttributeBindings() {
		return attributeBindingMap.values();
	}

	public AttributeBinding getAttributeBinding(String name) {
		return attributeBindingMap.get( name );
	}

	public SimpleAttributeBinding makeSimpleAttributeBinding(String name) {
		final SimpleAttributeBinding binding = new SimpleAttributeBinding( this );
		attributeBindingMap.put( name, binding );
		binding.setAttribute( entity.getAttribute( name ) );
		return binding;
	}

	public BagBinding makeBagAttributeBinding(String name) {
		final BagBinding binding = new BagBinding( this );
		attributeBindingMap.put( name, binding );
		binding.setAttribute( entity.getAttribute( name ) );
		return binding;
	}

	public Caching getCaching() {
		return caching;
	}

	public void setCaching(Caching caching) {
		this.caching = caching;
	}

	public Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
	}

	public void setMetaAttributes(Map<String, MetaAttribute> metaAttributes) {
		this.metaAttributes = metaAttributes;
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
		return discriminatorValue;
	}

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
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
