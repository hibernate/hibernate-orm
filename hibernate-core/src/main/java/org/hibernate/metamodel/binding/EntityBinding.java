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
import org.hibernate.metamodel.binding.state.EntityBindingState;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.JavaType;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.source.spi.BindingContext;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.classloading.spi.ClassLoaderService;
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
	private JavaType proxyInterfaceType;

	private String jpaEntityName;

	private Class<EntityPersister> entityPersisterClass;
	private Class<EntityTuplizer> entityTuplizerClass;

	private boolean isRoot;
	private InheritanceType entityInheritanceType;

	private final EntityIdentifier entityIdentifier = new EntityIdentifier( this );
	private EntityDiscriminator entityDiscriminator;
	private SimpleAttributeBinding versionBinding;

	private Map<String, AttributeBinding> attributeBindingMap = new HashMap<String, AttributeBinding>();
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
	private int optimisticLockMode;

	private Boolean isAbstract;

	private CustomSQL customInsert;
	private CustomSQL customUpdate;
	private CustomSQL customDelete;

	private Set<String> synchronizedTableNames = new HashSet<String>();

	public EntityBinding initialize(BindingContext bindingContext, EntityBindingState state) {
		// todo : Entity will need both entityName and className to be effective
		this.entity = new Entity( state.getEntityName(), state.getSuperType(), bindingContext.makeJavaType( state.getClassName() ) );

		this.isRoot = state.isRoot();
		this.entityInheritanceType = state.getEntityInheritanceType();

		this.entityMode = state.getEntityMode();
		this.jpaEntityName = state.getJpaEntityName();

		// todo : handle the entity-persister-resolver stuff
		this.entityPersisterClass = state.getCustomEntityPersisterClass();
		this.entityTuplizerClass = state.getCustomEntityTuplizerClass();

		this.caching = state.getCaching();
		this.metaAttributeContext = state.getMetaAttributeContext();

		if ( entityMode == EntityMode.POJO ) {
			if ( state.getProxyInterfaceName() != null ) {
				this.proxyInterfaceType = bindingContext.makeJavaType( state.getProxyInterfaceName() );
				this.lazy = true;
			}
			else if ( state.isLazy() ) {
				this.proxyInterfaceType = entity.getJavaType();
				this.lazy = true;
			}
		}
		else {
			this.proxyInterfaceType = new JavaType( Map.class );
			this.lazy = state.isLazy();
		}

		this.mutable = state.isMutable();
		this.explicitPolymorphism = state.isExplicitPolymorphism();
		this.whereFilter = state.getWhereFilter();
		this.rowId = state.getRowId();
		this.dynamicUpdate = state.isDynamicUpdate();
		this.dynamicInsert = state.isDynamicInsert();
		this.batchSize = state.getBatchSize();
		this.selectBeforeUpdate = state.isSelectBeforeUpdate();
		this.optimisticLockMode = state.getOptimisticLockMode();
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

	/**
	 * Gets the number of attribute bindings defined on this class, including the
	 * identifier attribute binding and attribute bindings defined
	 * as part of a join.
	 *
	 * @return The number of attribute bindings
	 */
	public int getAttributeBindingClosureSpan() {
		// TODO: fix this after HHH-6337 is fixed.
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
		// TODO: fix this after HHH-6337 is fixed.
		// if this is not a root, then need to include the superclass attribute bindings
		return getAttributeBindings();
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

	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	public boolean isMutable() {
		return mutable;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public JavaType getProxyInterfaceType() {
		return proxyInterfaceType;
	}

	public String getWhereFilter() {
		return whereFilter;
	}

	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public String getRowId() {
		return rowId;
	}

	public String getDiscriminatorValue() {
		return entityDiscriminator == null ? null : entityDiscriminator.getDiscriminatorValue();
	}

	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
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

	public Class<EntityPersister> getEntityPersisterClass() {
		return entityPersisterClass;
	}

	public Class<EntityTuplizer> getEntityTuplizerClass() {
		return entityTuplizerClass;
	}

	public Boolean isAbstract() {
		return isAbstract;
	}

	protected void addSynchronizedTable(String tableName) {
		synchronizedTableNames.add( tableName );
	}

	public Set<String> getSynchronizedTableNames() {
		return synchronizedTableNames;
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
