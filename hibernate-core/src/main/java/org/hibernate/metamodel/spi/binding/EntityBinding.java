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
package org.hibernate.metamodel.spi.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.spi.JpaCallbackSource;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.domain.AttributeContainer;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * Provides the link between the domain and the relational model for an entity.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 * @author Strong Liu
 */
public class EntityBinding extends AbstractAttributeBindingContainer implements Filterable {
	private static final String NULL_DISCRIMINATOR_MATCH_VALUE = "null";
	private static final String NOT_NULL_DISCRIMINATOR_MATCH_VALUE = "not null";

	private final EntityBinding superEntityBinding;
	private final List<EntityBinding> subEntityBindings = new ArrayList<EntityBinding>();
	private final HierarchyDetails hierarchyDetails;

	private Entity entity;
	private TableSpecification primaryTable;
	private String primaryTableName;
	private Map<Identifier, SecondaryTable> secondaryTables = new LinkedHashMap<Identifier, SecondaryTable>();

	private JavaTypeDescriptor proxyInterfaceType;

	private String entityName;
	private String jpaEntityName;

	private Class<? extends EntityPersister> customEntityPersisterClass;
	private Class<? extends EntityTuplizer> customEntityTuplizerClass;

	private String discriminatorMatchValue;

	private List<FilterConfiguration> filterConfigurations = new ArrayList<FilterConfiguration>();

	private MetaAttributeContext metaAttributeContext;

	private boolean lazy;
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

	private String[] synchronizedTableNames = StringHelper.EMPTY_STRINGS;
	private Map<String, AttributeBinding> attributeBindingMap = new LinkedHashMap<String, AttributeBinding>();

	private List<JpaCallbackSource> jpaCallbackClasses = new ArrayList<JpaCallbackSource>();
	private final int subEntityBindingId;
	private int nextSubEntityBindingId = 0;
	//for joined sub entitybinding only
	private boolean isCascadeDeleteEnabled = false;

	private AttributePath pathBase;
	private AttributeRole roleBase;

	public EntityBinding(HierarchyDetails hierarchyDetails) {
		this.hierarchyDetails = hierarchyDetails;
		this.superEntityBinding = null;
		this.subEntityBindingId = 0;
	}

	public EntityBinding makeSubBinding() {
		final EntityBinding sub = new EntityBinding( this );
		subEntityBindings.add( sub );
		return sub;
	}

	/**
	 * Used to instantiate the EntityBinding for an entity that is a subclass (sub-entity) in an inheritance hierarchy
	 *
	 * @param superEntityBinding The entity binding of this binding's super
	 */
	public EntityBinding(EntityBinding superEntityBinding) {
		this.superEntityBinding = superEntityBinding;
		this.hierarchyDetails = superEntityBinding.getHierarchyDetails();
		this.subEntityBindingId = superEntityBinding.nextSubEntityBindingId();

		// TODO: the ID attribute binding needs to be recreated for this EntityBinding
		// otherwise, this !=  hierarchyDetails.getEntityIdentifier().getAttributeBinding().getContainer()
	}

	private int nextSubEntityBindingId(){
		return isRoot()? ++nextSubEntityBindingId : superEntityBinding.nextSubEntityBindingId();
	}

	public HierarchyDetails getHierarchyDetails() {
		return hierarchyDetails;
	}

	public EntityBinding getSuperEntityBinding() {
		return superEntityBinding;
	}

	public int getSubEntityBindingId() {
		return subEntityBindingId;
	}

	public boolean isRoot() {
		return superEntityBinding == null;
	}

	public boolean isPolymorphic() {
		return !isRoot() ||
				hierarchyDetails.getEntityDiscriminator() != null ||
				!subEntityBindings.isEmpty();
	}

	public boolean hasSubEntityBindings() {
		return !subEntityBindings.isEmpty();
	}



	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;

		this.pathBase = new AttributePath();
		this.roleBase = new AttributeRole( entity.getName() );
	}

	@Override
	public TableSpecification getPrimaryTable() {
		return primaryTable;
	}

	public void setPrimaryTable(TableSpecification primaryTable) {
		this.primaryTable = primaryTable;
	}

	public boolean hasTable(String tableName) {
		return tableName.equals( getPrimaryTableName() ) ||
				secondaryTables.containsKey( Identifier.toIdentifier( tableName ) );
	}

	public TableSpecification locateTable(String tableName) {
		if ( tableName == null || tableName.equals( getPrimaryTableName() ) ) {
			return primaryTable;
		}
		SecondaryTable secondaryTable = secondaryTables.get( Identifier.toIdentifier( tableName ) );
		if ( secondaryTable == null ) {
			throw new AssertionFailure(
					String.format(
							"Unable to find table %s amongst tables %s",
							tableName,
							secondaryTables.keySet()
					)
			);
		}
		return secondaryTable.getSecondaryTableReference();
	}

	public AttributeBinding locateAttributeBinding(String name, boolean searchParent) {
		AttributeBinding attributeBinding = locateAttributeBinding( name );
		if ( attributeBinding == null && searchParent && getSuperEntityBinding() != null ) {
			return getSuperEntityBinding().locateAttributeBinding( name, searchParent );
		}
		else {
			return attributeBinding;
		}
	}

	public SingularAttributeBinding locateAttributeBinding(
			TableSpecification table,
			List<? extends Value> values,
			boolean searchParent) {
		SingularAttributeBinding attributeBinding = locateAttributeBindingFromIdentifier( table, values );
		if ( attributeBinding ==  null ) {
			attributeBinding = locateAttributeBinding( table, values );
		}
		if ( attributeBinding == null && searchParent && getSuperEntityBinding() != null ) {
			attributeBinding = getSuperEntityBinding().locateAttributeBinding( table, values, searchParent );
		}
		return attributeBinding;
	}

	private SingularAttributeBinding locateAttributeBindingFromIdentifier(
			TableSpecification table,
			List<? extends Value> values) {
		if ( !primaryTable.equals( table ) ) {
			return null;
		}

		final EntityIdentifier idInfo = hierarchyDetails.getEntityIdentifier();
		final SingularAttributeBinding idAttributeBinding = idInfo.getEntityIdentifierBinding().getAttributeBinding();
		final List<? extends Value> idAttributeValues = idAttributeBinding.getValues();
		// order-insensitive check (column order handled later)
		if ( idAttributeValues.size() == values.size()
				&& idAttributeValues.containsAll( values ) ) {
			return idAttributeBinding;
		}
//		if ( idAttributeValues.equals( values ) ) {
//			return idAttributeBinding;
//		}

		return null;
	}

	public AttributeBinding locateAttributeBindingByPath(String path, boolean searchParent) {
		if ( path == null ) {
			throw new IllegalArgumentException( "path must be non-null." );
		}
		final String pathDelimiter = "\\.";
		String[] tokens = path.split( pathDelimiter );
		AttributeBinding attributeBinding = locateAttributeBinding( tokens[ 0 ], searchParent );
		for ( int i = 1 ; i < tokens.length && attributeBinding != null ; i++ )  {
			final AttributeBindingContainer attributeBindingContainer;
			if ( AttributeBindingContainer.class.isInstance( attributeBinding ) ) {
				attributeBindingContainer = (AttributeBindingContainer) attributeBinding;
			}
			else if ( EmbeddedAttributeBinding.class.isInstance( attributeBinding ) ) {
				attributeBindingContainer = ( (EmbeddedAttributeBinding) attributeBinding ).getEmbeddableBinding();
			}
			else {
				// TODO: improve this message!!!
				throw new MappingException( "improve this!!!" );
			}

			attributeBinding = attributeBindingContainer.locateAttributeBinding( tokens[i] );
		}
		return attributeBinding;
	}

	public String getPrimaryTableName() {
		return primaryTableName;
	}

	public void setPrimaryTableName(String primaryTableName) {
		this.primaryTableName = primaryTableName;
	}

	public void addSecondaryTable(SecondaryTable secondaryTable) {
		secondaryTables.put( secondaryTable.getSecondaryTableReference().getLogicalName(), secondaryTable );
	}
	public Map<Identifier, SecondaryTable> getSecondaryTables() {
		return Collections.unmodifiableMap( secondaryTables );
	}

	public boolean isDiscriminatorMatchValueNull() {
		return NULL_DISCRIMINATOR_MATCH_VALUE.equals( discriminatorMatchValue );
	}

	public boolean isDiscriminatorMatchValueNotNull() {
		return NOT_NULL_DISCRIMINATOR_MATCH_VALUE.equals( discriminatorMatchValue );
	}

	public String getDiscriminatorMatchValue() {
		return discriminatorMatchValue;
	}

	public void setDiscriminatorMatchValue(String discriminatorMatchValue) {
		this.discriminatorMatchValue = discriminatorMatchValue;
	}

	@Override
	public void addFilterConfiguration(FilterConfiguration filterConfiguration) {
		filterConfigurations.add( filterConfiguration );
	}

	@Override
	public List<FilterConfiguration> getFilterConfigurations() {
		if ( superEntityBinding != null ) {
			List<FilterConfiguration> results = new ArrayList<FilterConfiguration>( filterConfigurations );
			results.addAll( superEntityBinding.getFilterConfigurations() );
			return results;
		}
		return filterConfigurations;
	}

	@Override
	public EntityBinding seekEntityBinding() {
		return this;
	}

	@Override
	public AttributeRole getRoleBase() {
		return roleBase;
	}

	@Override
	public AttributePath getPathBase() {
		return pathBase;
	}

	@Override
	public AttributeContainer getAttributeContainer() {
		return getEntity();
	}

	@Override
	protected Map<String, AttributeBinding> attributeBindingMapInternal() {
		return attributeBindingMap;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	public void setMetaAttributeContext(MetaAttributeContext metaAttributeContext) {
		this.metaAttributeContext = metaAttributeContext;
	}

	public boolean isCascadeDeleteEnabled() {
		return isCascadeDeleteEnabled;
	}

	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		isCascadeDeleteEnabled = cascadeDeleteEnabled;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public JavaTypeDescriptor getProxyInterfaceType() {
		return proxyInterfaceType;
	}

	public void setProxyInterfaceType(JavaTypeDescriptor proxyInterfaceType) {
		this.proxyInterfaceType = proxyInterfaceType;
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

	public void setSubselectLoadableCollections(boolean hasSubselectLoadableCollections) {
		this.hasSubselectLoadableCollections = hasSubselectLoadableCollections;
	}

	public Class<? extends EntityPersister> getCustomEntityPersisterClass() {
		if ( customEntityPersisterClass != null ) {
			return customEntityPersisterClass;
		}
		else if ( superEntityBinding != null ) {
			return superEntityBinding.getCustomEntityPersisterClass();
		}
		return null;
	}

	public void setCustomEntityPersisterClass(Class<? extends EntityPersister> customEntityPersisterClass) {
		this.customEntityPersisterClass = customEntityPersisterClass;
	}
	@Override
	public Class<? extends EntityTuplizer> getCustomTuplizerClass() {
		if ( customEntityTuplizerClass != null ) {
			return customEntityTuplizerClass;
		}
		else if ( superEntityBinding != null ) {
			return superEntityBinding.getCustomTuplizerClass();
		}
		return null;
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

	public String[] getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	public void addSynchronizedTableNames(String [] synchronizedTableNames) {
		this.synchronizedTableNames = ArrayHelper.join( this.synchronizedTableNames, synchronizedTableNames );
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
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
		return String.format(
				"EntityBinding(%s)",
				entity != null ? StringHelper.collapse( getEntityName() ) : "<not set>"
		);
	}


	public EmbeddedAttributeBinding makeVirtualCompositeAttributeBinding(
			SingularAttribute syntheticAttribute,
			EmbeddableBindingImplementor virtualEmbeddableBinding,
			MetaAttributeContext metaAttributeContext) {
		if ( !syntheticAttribute.isSynthetic() ) {
			throw new AssertionFailure(
					"Illegal attempt to create synthetic attribute binding from non-synthetic attribute reference"
			);
		}

		return new EmbeddedAttributeBinding(
				this,
				syntheticAttribute,
				"embedded",  // TODO: get rid of "magic" string.
				false,
				false,
				NaturalIdMutability.NOT_NATURAL_ID,
				metaAttributeContext,
				getRoleBase(),
				getPathBase(),
				virtualEmbeddableBinding
		);
	}

	public BackRefAttributeBinding makeBackRefAttributeBinding(
			SingularAttribute syntheticAttribute,
			PluralAttributeBinding pluralAttributeBinding,
			boolean isIndexBackRef) {
		if ( ! syntheticAttribute.isSynthetic() ) {
			throw new AssertionFailure(
					"Illegal attempt to create synthetic attribute binding from non-synthetic attribute reference"
			);
		}
		final BackRefAttributeBinding  binding = new BackRefAttributeBinding(
				this,
				syntheticAttribute,
				pluralAttributeBinding,
				isIndexBackRef
		);

		registerAttributeBinding( binding );
		return binding;
	}


	public void setJpaCallbackClasses(List<JpaCallbackSource> jpaCallbackClasses) {
		this.jpaCallbackClasses = jpaCallbackClasses;
	}

	public List<JpaCallbackSource> getJpaCallbackClasses() {
		return Collections.unmodifiableList( jpaCallbackClasses );
	}
	//--------------------------
	//meta methods for persister , to improve performance, these methods below should really be replaced as ValueHolder
	//and only be called in persister -- after build MetadataImpl


	public TableSpecification[] getTableClosure() {
		if ( isRoot() ) {
			return new TableSpecification[] { getPrimaryTable() };
		}
		return ArrayHelper.join( superEntityBinding.getTableClosure(), getPrimaryTable() );
	}

	public EntityBinding[] getEntityBindingClosure() {
		if ( isRoot() ) {
			return new EntityBinding[] { this };
		}
		return ArrayHelper.join( superEntityBinding.getEntityBindingClosure(), this );
	}

	public int getSecondaryTableClosureSpan() {
		return isRoot() ? secondaryTables.size() : superEntityBinding.getSecondaryTableClosureSpan() + secondaryTables.size();
	}

	public SecondaryTable[] getSecondaryTableClosure() {
		if ( isRoot() ) {
			return secondaryTables.values().toArray( new SecondaryTable[secondaryTables.size()] );
		}
		else {
			return ArrayHelper.join(
					superEntityBinding.getSecondaryTableClosure(),
					secondaryTables.values().toArray( new SecondaryTable[secondaryTables.size()] )
			);
		}
	}

	public String[] getSynchronizedTableNameClosure() {
		if ( isRoot() ) {
			return getSynchronizedTableNames();
		}
		return ArrayHelper.join( superEntityBinding.getSynchronizedTableNameClosure(), getSynchronizedTableNames() );
	}


	/**
	 * Gets the number of attribute bindings defined on this class, including the
	 * identifier attribute binding and attribute bindings defined
	 * as part of a join.
	 *
	 * @return The number of attribute bindings
	 */
	public int getAttributeBindingClosureSpan() {
		// TODO: update account for join attribute bindings
		return getAttributeBindingClosure().length;
	}

	/**
	 * Gets the attribute bindings defined on this class, including the
	 * identifier attribute binding and attribute bindings defined
	 * as part of a join.
	 *
	 * @return The attribute bindings.
	 */
	public AttributeBinding[] getAttributeBindingClosure() {
		// TODO: update size to account for joins
		if ( isRoot() ) {
			return attributeBindingMapInternal().values()
					.toArray( new AttributeBinding[attributeBindingMapInternal().size()] );
		}
		else {
			return ArrayHelper.join(
					superEntityBinding.getAttributeBindingClosure(),
					attributeBindingMapInternal().values()
							.toArray( new AttributeBinding[attributeBindingMapInternal().size()] )

			);
		}
	}

	public AttributeBinding[] getNonIdAttributeBindingClosure(){
		// TODO: update size to account for joins
		if ( isRoot() ) {
			return internalGetNonIdAttributeBindings();
		}
		else {
			return ArrayHelper.join(
					superEntityBinding.getNonIdAttributeBindingClosure(),
					internalGetNonIdAttributeBindings()
			);
		}
	}

	public List<AttributeBinding> getNonIdAttributeBindings() {
		final List<AttributeBinding> list = new ArrayList<AttributeBinding>();
		for ( final AttributeBinding ab : attributeBindings() ) {
			boolean isId = getHierarchyDetails().getEntityIdentifier()
					.getEntityIdentifierBinding()
					.isIdentifierAttributeBinding( ab );
			if ( !isId ) {
				list.add( ab );
			}
		}
		return list;

	}

	private AttributeBinding[] internalGetNonIdAttributeBindings() {
		final List<AttributeBinding> list = getNonIdAttributeBindings();
		return list.toArray( new AttributeBinding[list.size()] );
	}

	public List<EntityBinding> getDirectSubEntityBindings() {
		return subEntityBindings;
	}

	/**
	 * Returns sub-EntityBinding objects in a special 'order', most derived subclasses
	 * first. Specifically, the sub-entity bindings follow a depth-first,
	 * post-order traversal
	 *
	 * Note that the returned value excludes this entity binding.
	 *
	 * @return sub-entity bindings ordered by those entity bindings that are most derived.
	 */
	public EntityBinding[] getPostOrderSubEntityBindingClosure() {
		EntityBinding[] results = new EntityBinding[0];
		if ( subEntityBindings.isEmpty() ) {
			return results;
		}
		for ( EntityBinding subEntityBinding : subEntityBindings ) {
			EntityBinding[] subSubEntityBindings = subEntityBinding.getPostOrderSubEntityBindingClosure();
			results  = ArrayHelper.join( results, subSubEntityBindings );
		}
		if ( !subEntityBindings.isEmpty() ) {
			results  = ArrayHelper.join( results, subEntityBindings.toArray( new EntityBinding[subEntityBindings.size()] ) );
		}
		return results;
	}

	/**
	 * Returns sub-EntityBinding ordered as a depth-first,
	 * pre-order traversal (a subclass precedes its own subclasses).
	 *
	 * Note that the returned value specifically excludes this entity binding.
	 *
	 * @return sub-entity bindings ordered as a depth-first,
	 *         pre-order traversal
	 */
	public EntityBinding[] getPreOrderSubEntityBindingClosure() {
		return getPreOrderSubEntityBindingClosure( false, new EntityBinding[0] );
	}

	private EntityBinding[] getPreOrderSubEntityBindingClosure(boolean includeThis, EntityBinding[] results) {
		if ( includeThis ) {
			results = ArrayHelper.join( results, this );
		}
		for ( EntityBinding subEntityBinding : subEntityBindings ) {
			results = subEntityBinding.getPreOrderSubEntityBindingClosure(
					true, results
			);
		}
		return results;
	}

	public TableSpecification[] getPreOrderSubTableClosure(){
		EntityBinding[] subEntityBindings = getPreOrderSubEntityBindingClosure();
		TableSpecification [] tables = new TableSpecification[subEntityBindings.length];
		for(int i=0;i<subEntityBindings.length;i++){
			tables[i] = subEntityBindings[i].getPrimaryTable();
		}
		return tables;
	}

	public SecondaryTable[] getSubEntitySecondaryTables() {
		SecondaryTable[] results = new SecondaryTable[0];
		for ( EntityBinding eb : getPreOrderSubEntityBindingClosure() ) {
			Collection<SecondaryTable> sts = eb.getSecondaryTables().values();
			int size = sts.size();
			if ( size == 0 ) {
				continue;
			}
			results = ArrayHelper.join( results, sts.toArray( new SecondaryTable[size] ) );
		}
		return results;
	}


	public SecondaryTable[] getEntitiesSecondaryTableClosure() {
		if ( ! subEntityBindings.isEmpty() ) {
			return ArrayHelper.join( getSecondaryTableClosure(), getSubEntitySecondaryTables() );
		}
		else {
			return getSecondaryTableClosure();
		}
	}

	public int getSubEntityBindingClosureSpan() {
		int n = subEntityBindings.size();
		for ( final EntityBinding seb : subEntityBindings ) {
			n += seb.getSubEntityBindingClosureSpan();
		}
		return n;
	}

	/**
	 * @return the attribute bindings for this EntityBinding and all of its
	 *         sub-EntityBinding, starting from the root of the hierarchy; includes
	 *         the identifier and attribute bindings defined as part of a join.
	 */
	public AttributeBinding[] getEntitiesAttributeBindingClosure() {
		AttributeBinding[] results = getAttributeBindingClosure();

		for ( EntityBinding subEntityBinding : getPreOrderSubEntityBindingClosure() ) {
			// only add attribute bindings declared for the subEntityBinding

			results = ArrayHelper.join(
					results,
					subEntityBinding.attributeBindingMapInternal().values().toArray( new AttributeBinding[subEntityBinding.attributeBindingMapInternal().size()] )
			);
			// TODO: if EntityBinding.attributeBindings() excludes joined attributes, then they need to be added here
		}
		return results;
	}
	public AttributeBinding[] getNonIdEntitiesAttributeBindingClosure() {
		AttributeBinding[] results = getNonIdAttributeBindingClosure();

		for ( EntityBinding subEntityBinding : getPreOrderSubEntityBindingClosure() ) {
			// only add attribute bindings declared for the subEntityBinding

			results = ArrayHelper.join(
					results,
					subEntityBinding.internalGetNonIdAttributeBindings()
			);
			// TODO: if EntityBinding.attributeBindings() excludes joined attributes, then they need to be added here
		}
		return results;
	}

	public boolean isClassOrSuperclassSecondaryTable(SecondaryTable secondaryTable) {
		return secondaryTables.containsKey( secondaryTable.getSecondaryTableReference().getLogicalName() ) ||
				( superEntityBinding != null && superEntityBinding.isClassOrSuperclassSecondaryTable( secondaryTable ) );
	}

	private List<RelationalValueBinding> keyRelationalValueBindings;

	public List<RelationalValueBinding> getKeyRelationalValueBindings() {
		if ( keyRelationalValueBindings == null ) {
			keyRelationalValueBindings = getHierarchyDetails().getEntityIdentifier()
					.getEntityIdentifierBinding()
					.getRelationalValueBindings();
		}
		return keyRelationalValueBindings;
	}

	public void setKeyRelationalValueBindings(List<RelationalValueBinding> keyRelationalValueBindings) {
		this.keyRelationalValueBindings = keyRelationalValueBindings;
	}

	public int getSecondaryTableNumber(SingularAttributeBinding attributeBinding) {
		if ( attributeBinding.getRelationalValueBindings().isEmpty() ) {
			return 0;
		}
		int result=1;
		TableSpecification table = attributeBinding.getRelationalValueBindings().get( 0 ).getTable();
		for ( SecondaryTable secondaryTable : getEntitiesSecondaryTableClosure() ) {
			if ( secondaryTable.getSecondaryTableReference() == table ) {
				return result;
			}
			result++;
		}
		return 0;
	}
}
