/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Supplier;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.util.collections.JoinedList;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.Alias;
import org.hibernate.type.CollectionType;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle.expectationConstructor;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.StringHelper.root;
import static org.hibernate.mapping.MappingHelper.checkPropertyColumnDuplication;
import static org.hibernate.mapping.MappingHelper.classForName;
import static org.hibernate.sql.Template.collectColumnNames;

/**
 * A mapping model object that represents an {@linkplain jakarta.persistence.Entity entity class}.
 *
 * @author Gavin King
 */
public abstract sealed class PersistentClass
		implements IdentifiableTypeClass, AttributeContainer, Filterable, MetaAttributable, Contributable, Serializable
		permits RootClass, Subclass {

	private static final Alias PK_ALIAS = new Alias( 15, "PK" );

	/**
	 * The magic value of {@link jakarta.persistence.DiscriminatorValue#value}
	 * which indicates that the subclass is distinguished by a null value of the
	 * discriminator column.
	 */
	public static final String NULL_DISCRIMINATOR_MAPPING = "null";
	/**
	 * The magic value of {@link jakarta.persistence.DiscriminatorValue#value}
	 * which indicates that the subclass is distinguished by any non-null value
	 * of the discriminator column.
	 */
	public static final String NOT_NULL_DISCRIMINATOR_MAPPING = "not null";

	private final MetadataBuildingContext metadataBuildingContext;
	private final String contributor;

	private String entityName;

	private String className;
	private transient Class<?> mappedClass;

	private String proxyInterfaceName;
	private transient Class<?> proxyInterface;

	private String jpaEntityName;

	private String discriminatorValue;
	private boolean lazy;
	private final List<Property> properties = new ArrayList<>();
	private final List<Property> declaredProperties = new ArrayList<>();
	private final List<Subclass> subclasses = new ArrayList<>();
	private final List<Property> subclassProperties = new ArrayList<>();
	private final List<Table> subclassTables = new ArrayList<>();
	private boolean dynamicInsert;
	private boolean dynamicUpdate;
	private int batchSize = -1;
	private boolean selectBeforeUpdate;
	private java.util.Map<String, MetaAttribute> metaAttributes;
	private final List<Join> joins = new ArrayList<>();
	private final List<Join> subclassJoins = new ArrayList<>();
	private final List<FilterConfiguration> filters = new ArrayList<>();
	protected final Set<String> synchronizedTables = new HashSet<>();
	private String loaderName;
	private Boolean isAbstract;
	private boolean hasSubselectLoadableCollections;
	private Component identifierMapper;
	private List<CallbackDefinition> callbackDefinitions;

	private final List<CheckConstraint> checkConstraints = new ArrayList<>();

	// Custom SQL
	private String customSQLInsert;
	private boolean customInsertCallable;
	private String customSQLUpdate;
	private boolean customUpdateCallable;
	private String customSQLDelete;
	private boolean customDeleteCallable;

	private MappedSuperclass superMappedSuperclass;
	private Component declaredIdentifierMapper;
	private OptimisticLockStyle optimisticLockStyle;

	private Supplier<? extends Expectation> insertExpectation;
	private Supplier<? extends Expectation> updateExpectation;
	private Supplier<? extends Expectation> deleteExpectation;

	private boolean isCached;
	private CacheLayout queryCacheLayout;

	public PersistentClass(MetadataBuildingContext buildingContext) {
		this.metadataBuildingContext = buildingContext;
		this.contributor = buildingContext.getCurrentContributorName();
	}

	public String getContributor() {
		return contributor;
	}

	public ServiceRegistry getServiceRegistry() {
		return metadataBuildingContext.getBuildingOptions().getServiceRegistry();
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className == null ? null : className.intern();
		this.mappedClass = null;
	}

	public String getProxyInterfaceName() {
		return proxyInterfaceName;
	}

	public void setProxyInterfaceName(String proxyInterfaceName) {
		this.proxyInterfaceName = proxyInterfaceName;
		this.proxyInterface = null;
	}

	private Class<?> getClassForName(String className) {
		return classForName( className, metadataBuildingContext.getBootstrapContext() );
	}

	public Class<?> getMappedClass() throws MappingException {
		if ( className == null ) {
			return null;
		}
		try {
			if ( mappedClass == null ) {
				mappedClass = getClassForName( className );
			}
			return mappedClass;
		}
		catch (ClassLoadingException e) {
			throw new MappingException( "entity class not found: " + className, e );
		}
	}

	public Class<?> getProxyInterface() {
		if ( proxyInterfaceName == null ) {
			return null;
		}
		try {
			if ( proxyInterface == null ) {
				proxyInterface = getClassForName( proxyInterfaceName );
			}
			return proxyInterface;
		}
		catch (ClassLoadingException e) {
			throw new MappingException( "proxy class not found: " + proxyInterfaceName, e );
		}
	}

	public boolean useDynamicInsert() {
		return dynamicInsert;
	}

	abstract int nextSubclassId();

	public abstract int getSubclassId();

	public boolean useDynamicUpdate() {
		return dynamicUpdate;
	}

	public void setDynamicInsert(boolean dynamicInsert) {
		this.dynamicInsert = dynamicInsert;
	}

	public void setDynamicUpdate(boolean dynamicUpdate) {
		this.dynamicUpdate = dynamicUpdate;
	}


	public String getDiscriminatorValue() {
		return discriminatorValue;
	}

	public void addSubclass(Subclass subclass) throws MappingException {
		// inheritance cycle detection (paranoid check)
		PersistentClass superclass = getSuperclass();
		while ( superclass != null ) {
			if ( subclass.getEntityName().equals( superclass.getEntityName() ) ) {
				throw new MappingException(
						"Circular inheritance mapping: '"
							+ subclass.getEntityName()
							+ "' will have itself as superclass when extending '"
							+ getEntityName()
							+ "'"
				);
			}
			superclass = superclass.getSuperclass();
		}
		subclasses.add( subclass );
	}

	public boolean hasSubclasses() {
		return !subclasses.isEmpty();
	}

	public int getSubclassSpan() {
		int span = subclasses.size();
		for ( var subclass : subclasses ) {
			span += subclass.getSubclassSpan();
		}
		return span;
	}

	/**
	 * Get the subclasses in a special 'order', most derived subclasses first.
	 */
	public List<Subclass> getSubclasses() {
		@SuppressWarnings("unchecked")
		final List<Subclass>[] subclassLists = new List[subclasses.size() + 1];
		int j;
		for (j = 0; j < subclasses.size(); j++) {
			subclassLists[j] = subclasses.get(j).getSubclasses();
		}
		subclassLists[j] = subclasses;
		return new JoinedList<>( subclassLists );
	}

	public List<PersistentClass> getSubclassClosure() {
		final ArrayList<List<PersistentClass>> lists = new ArrayList<>();
		lists.add( List.of( this ) );
		for ( var subclass : getSubclasses() ) {
			lists.add( subclass.getSubclassClosure() );
		}
		return new JoinedList<>( lists );
	}

	public Table getIdentityTable() {
		return getRootTable();
	}

	public List<Subclass> getDirectSubclasses() {
		return subclasses;
	}

	@Override
	public void addProperty(Property property) {
		properties.add( property );
		declaredProperties.add( property );
		property.setPersistentClass( this );
	}

	@Override
	public boolean contains(Property property) {
		return properties.contains( property );
	}

	public abstract Table getTable();

	public String getEntityName() {
		return entityName;
	}

	public abstract boolean isMutable();

	public abstract boolean hasIdentifierProperty();

	public abstract Property getIdentifierProperty();

	public abstract Property getDeclaredIdentifierProperty();

	public abstract KeyValue getIdentifier();

	public abstract Property getVersion();

	public abstract Property getDeclaredVersion();

	public abstract Value getDiscriminator();

	public abstract boolean isInherited();

	public abstract boolean isPolymorphic();

	public abstract boolean isVersioned();


	public boolean isCached() {
		return isCached;
	}

	public void setCached(boolean cached) {
		isCached = cached;
	}

	public CacheLayout getQueryCacheLayout() {
		return queryCacheLayout;
	}

	public void setQueryCacheLayout(CacheLayout queryCacheLayout) {
		this.queryCacheLayout = queryCacheLayout;
	}

	public abstract String getCacheConcurrencyStrategy();

	public abstract String getNaturalIdCacheRegionName();

	public abstract PersistentClass getSuperclass();

	/**
	 * @deprecated No longer supported
	 */
	@Deprecated
	public boolean isExplicitPolymorphism() {
		return false;
	}

	public abstract boolean isDiscriminatorInsertable();

	public abstract List<Property> getPropertyClosure();

	public abstract List<Table> getTableClosure();

	public abstract List<KeyValue> getKeyClosure();

	protected void addSubclassProperty(Property prop) {
		subclassProperties.add( prop );
	}

	protected void addSubclassJoin(Join join) {
		subclassJoins.add( join );
	}

	protected void addSubclassTable(Table subclassTable) {
		subclassTables.add( subclassTable );
	}

	public List<Property> getSubclassPropertyClosure() {
		final ArrayList<List<Property>> lists = new ArrayList<>();
		lists.add( getPropertyClosure() );
		lists.add( subclassProperties );
		for ( var join : subclassJoins ) {
			lists.add( join.getProperties() );
		}
		return new JoinedList<>( lists );
	}

	public List<Join> getSubclassJoinClosure() {
		return new JoinedList<>( getJoinClosure(), subclassJoins );
	}

	public List<Table> getSubclassTableClosure() {
		return new JoinedList<>( getTableClosure(), subclassTables );
	}

	public boolean isClassOrSuperclassJoin(Join join) {
		return joins.contains( join );
	}

	public boolean isClassOrSuperclassTable(Table closureTable) {
		return getTable() == closureTable;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public abstract boolean isConcreteProxy();

	public abstract boolean hasEmbeddedIdentifier();

	public abstract Table getRootTable();

	public abstract RootClass getRootClass();

	public abstract KeyValue getKey();

	public void setDiscriminatorValue(String discriminatorValue) {
		this.discriminatorValue = discriminatorValue;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName == null ? null : entityName.intern();
	}

	public void createPrimaryKey() {
		//Primary key constraint
		final var table = getTable();
		final var primaryKey = new PrimaryKey( table );
		primaryKey.setName( PK_ALIAS.toAliasString( table.getName() ) );
		primaryKey.addColumns( getKey() );
		if ( addPartitionKeyToPrimaryKey() ) {
			for ( var property : getProperties() ) {
				if ( property.getValue().isPartitionKey() ) {
					primaryKey.addColumns( property.getValue() );
				}
			}
		}
		table.setPrimaryKey( primaryKey );
	}

	private boolean addPartitionKeyToPrimaryKey() {
		return metadataBuildingContext.getMetadataCollector()
				.getDatabase().getDialect()
				.addPartitionKeyToPrimaryKey();
	}

	public abstract String getWhere();

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public boolean hasSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	public void setSelectBeforeUpdate(boolean selectBeforeUpdate) {
		this.selectBeforeUpdate = selectBeforeUpdate;
	}

	/**
	 * Build a list of properties which may be referenced in association mappings.
	 * <p>
	 * Includes properties defined in superclasses of the mapping inheritance.
	 * Includes all properties defined as part of a join.
	 *
	 * @see #getReferencedProperty
	 * @return The referenceable property iterator.
	 */
	public List<Property> getReferenceableProperties() {
		return getPropertyClosure();
	}

	/**
	 * Given a property path, locate the appropriate referenceable property reference.
	 * <p>
	 * A referenceable property is a property  which can be a target of a foreign-key
	 * mapping (e.g. {@code @ManyToOne}, {@code @OneToOne}).
	 *
	 * @param propertyPath The property path to resolve into a property reference.
	 *
	 * @return The property reference (never null).
	 *
	 * @throws MappingException If the property could not be found.
	 */
	public Property getReferencedProperty(String propertyPath) throws MappingException {
		try {
			return getRecursiveProperty( propertyPath, getReferenceableProperties() );
		}
		catch ( MappingException e ) {
			throw new MappingException(
					"property-ref [" + propertyPath + "] not found on entity [" + getEntityName() + "]", e
			);
		}
	}

	public Property getRecursiveProperty(String propertyPath) throws MappingException {
		try {
			return getRecursiveProperty( propertyPath, getPropertyClosure() );
		}
		catch ( MappingException e ) {
			throw new MappingException(
					"property [" + propertyPath + "] not found on entity [" + getEntityName() + "]", e
			);
		}
	}

	private Property getRecursiveProperty(String propertyPath, List<Property> properties) throws MappingException {
		Property property = null;
		var tokens = new StringTokenizer( propertyPath, ".", false );
		try {
			while ( tokens.hasMoreElements() ) {
				final String element = (String) tokens.nextElement();
				if ( property == null ) {
					Property identifierProperty = getIdentifierProperty();
					if ( identifierProperty != null && identifierProperty.getName().equals( element ) ) {
						// we have a mapped identifier property and the root of
						// the incoming property path matched that identifier
						// property
						property = identifierProperty;
					}
					else if ( identifierProperty == null && getIdentifierMapper() != null ) {
						// we have an embedded composite identifier
						try {
							identifierProperty = getProperty( element, getIdentifierMapper().getProperties() );
							// the root of the incoming property path matched one
							// of the embedded composite identifier properties
							property = identifierProperty;
						}
						catch ( MappingException ignore ) {
							// ignore it...
						}
					}

					if ( property == null ) {
						property = getProperty( element, properties );
					}
				}
				else {
					//flat recursive algorithm
					final var value = (Component) property.getValue();
					property = value.getProperty( element );
				}
			}
		}
		catch ( MappingException e ) {
			throw new MappingException( "property [" + propertyPath + "] not found on entity [" + getEntityName() + "]" );
		}

		return property;
	}

	private Property getProperty(String propertyName, List<Property> properties) throws MappingException {
		final String root = root( propertyName );
		for ( var property : properties ) {
			if ( property.getName().equals( root )
					|| ( property instanceof Backref || property instanceof IndexBackref )
							&& property.getName().equals( propertyName ) ) {
				return property;
			}
		}
		throw new MappingException( "property [" + propertyName + "] not found on entity [" + getEntityName() + "]" );
	}

	@Override
	public Property getProperty(String propertyName) throws MappingException {
		final var identifierProperty = getIdentifierProperty();
		if ( identifierProperty != null
				&& identifierProperty.getName().equals( root( propertyName ) ) ) {
			return identifierProperty;
		}
		else {
			List<Property> closure = getPropertyClosure();
			final var identifierMapper = getIdentifierMapper();
			if ( identifierMapper != null ) {
				closure = new JoinedList<>( identifierMapper.getProperties(), closure );
			}
			return getProperty( propertyName, closure );
		}
	}

	/**
	 * Check to see if this PersistentClass defines a property with the given name.
	 *
	 * @param name The property name to check
	 *
	 * @return {@code true} if a property with that name exists; {@code false} if not
	 */
	public boolean hasProperty(String name) {
		final var identifierProperty = getIdentifierProperty();
		if ( identifierProperty != null && identifierProperty.getName().equals( name ) ) {
			return true;
		}
		else {
			for ( var property : getPropertyClosure() ) {
				if ( property.getName().equals( name ) ) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Check to see if a property with the given name exists in the super hierarchy
	 * of this PersistentClass.  Does not check this PersistentClass, just up the
	 * hierarchy
	 *
	 * @param name The property name to check
	 *
	 * @return {@code true} if a property with that name exists; {@code false} if not
	 */
	public boolean isPropertyDefinedInSuperHierarchy(String name) {
		return getSuperclass() != null && getSuperclass().isPropertyDefinedInHierarchy( name );

	}

	/**
	 * Check to see if a property with the given name exists in this PersistentClass
	 * or in any of its super hierarchy.  Unlike {@link #isPropertyDefinedInSuperHierarchy},
	 * this method does check this PersistentClass
	 *
	 * @param name The property name to check
	 *
	 * @return {@code true} if a property with that name exists; {@code false} if not
	 */
	public boolean isPropertyDefinedInHierarchy(String name) {
		return hasProperty( name )
			|| getSuperMappedSuperclass() != null && getSuperMappedSuperclass().isPropertyDefinedInHierarchy( name )
			|| getSuperclass() != null && getSuperclass().isPropertyDefinedInHierarchy( name );
	}

	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	public void setOptimisticLockStyle(OptimisticLockStyle optimisticLockStyle) {
		this.optimisticLockStyle = optimisticLockStyle;
	}

	public void validate(Metadata mapping) throws MappingException {
		for ( var prop : getProperties() ) {
			if ( !prop.isValid( mapping ) ) {
				final var type = prop.getType();
				final int actualColumns = prop.getColumnSpan();
				final int requiredColumns = type.getColumnSpan( mapping );
				throw new MappingException(
						"Property '" + qualify( getEntityName(), prop.getName() )
								+ "' maps to " + actualColumns + " columns but " + requiredColumns
								+ " columns are required (type '" + type.getName()
								+ "' spans " + requiredColumns + " columns)"
				);
			}
		}
		checkPropertyDuplication();
		checkColumnDuplication();
	}

	private void checkPropertyDuplication() throws MappingException {
		final HashSet<String> names = new HashSet<>();
		for ( var property : getProperties() ) {
			if ( !names.add( property.getName() ) ) {
				throw new MappingException( "Duplicate property mapping of " + property.getName() + " found in " + getEntityName() );
			}
		}
	}

	public boolean isDiscriminatorValueNotNull() {
		return NOT_NULL_DISCRIMINATOR_MAPPING.equals( getDiscriminatorValue() );
	}

	public boolean isDiscriminatorValueNull() {
		return NULL_DISCRIMINATOR_MAPPING.equals( getDiscriminatorValue() );
	}

	public Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
	}

	public void setMetaAttributes(java.util.Map<String,MetaAttribute> metas) {
		this.metaAttributes = metas;
	}

	public MetaAttribute getMetaAttribute(String name) {
		return metaAttributes == null ? null : metaAttributes.get( name );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '(' + getEntityName() + ')';
	}

	public List<Join> getJoins() {
		return joins;
	}

	public List<Join> getJoinClosure() {
		return joins;
	}

	public void addJoin(Join join) {
		if ( !joins.contains( join ) ) {
			joins.add( join );
		}
		join.setPersistentClass( this );
	}

	public int getJoinClosureSpan() {
		return joins.size();
	}

	public int getPropertyClosureSpan() {
		int span = properties.size();
		for ( var join : joins ) {
			span += join.getPropertySpan();
		}
		return span;
	}

	public int getJoinNumber(Property prop) {
		int result = 1;
		for ( var join : getSubclassJoinClosure() ) {
			if ( join.containsProperty( prop ) ) {
				return result;
			}
			result++;
		}
		return 0;
	}

	/**
	 * Build a list of the properties defined on this class. The returned
	 * iterator only accounts for "normal" properties (i.e. non-identifier
	 * properties).
	 * <p>
	 * Differs from {@link #getUnjoinedProperties} in that the returned list
	 * will include properties defined as part of a join.
	 * <p>
	 * Differs from {@link #getReferenceableProperties} in that the properties
	 * defined in superclasses of the mapping inheritance are not included.
	 *
	 * @return A list over the "normal" properties.
	 */
	public List<Property> getProperties() {
		final ArrayList<List<Property>> list = new ArrayList<>();
		list.add( properties );
		for ( var join : joins ) {
			list.add( join.getProperties() );
		}
		return new JoinedList<>( list );
	}

	/**
	 * Get a list of the properties defined on this class <b>which
	 * are not defined as part of a join</b>.  As with {@link #getProperties},
	 * the returned iterator only accounts for non-identifier properties.
	 *
	 * @return An iterator over the non-joined "normal" properties.
	 */
	public List<Property> getUnjoinedProperties() {
		return properties;
	}

	public void setCustomSQLInsert(String customSQLInsert, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLInsert = customSQLInsert;
		this.customInsertCallable = callable;
		this.insertExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLInsert() {
		return customSQLInsert;
	}

	public boolean isCustomInsertCallable() {
		return customInsertCallable;
	}

	public void setCustomSQLUpdate(String customSQLUpdate, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLUpdate = customSQLUpdate;
		this.customUpdateCallable = callable;
		this.updateExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLUpdate() {
		return customSQLUpdate;
	}

	public boolean isCustomUpdateCallable() {
		return customUpdateCallable;
	}

	public void setCustomSQLDelete(String customSQLDelete, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLDelete = customSQLDelete;
		this.customDeleteCallable = callable;
		this.deleteExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLDelete() {
		return customSQLDelete;
	}

	public boolean isCustomDeleteCallable() {
		return customDeleteCallable;
	}

	public void addFilter(
			String name,
			String condition,
			boolean autoAliasInjection,
			java.util.Map<String, String> aliasTableMap,
			java.util.Map<String, String> aliasEntityMap) {
		filters.add(
				new FilterConfiguration(
						name,
						condition,
						autoAliasInjection,
						aliasTableMap,
						aliasEntityMap,
						this
				)
		);
	}

	public java.util.List<FilterConfiguration> getFilters() {
		return filters;
	}

	public boolean isForceDiscriminator() {
		return false;
	}

	public abstract boolean isJoinedSubclass();

	public String getLoaderName() {
		return loaderName;
	}

	public void setLoaderName(String loaderName) {
		this.loaderName = loaderName == null ? null : loaderName.intern();
	}

	public abstract Set<String> getSynchronizedTables();

	public void addSynchronizedTable(String table) {
		synchronizedTables.add( table );
	}

	public Boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(Boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	protected List<Property> getNonDuplicatedProperties() {
		return getUnjoinedProperties();
	}

	protected void checkColumnDuplication() {
		final String owner = "entity '" + getEntityName() + "'";
		final HashSet<String> cols = new HashSet<>();
		if ( getIdentifierMapper() == null ) {
			//an identifier mapper => getKey will be included in the getNonDuplicatedPropertyIterator()
			//and checked later, so it needs to be excluded
			getKey().checkColumnDuplication( cols, owner );
		}
		if ( isDiscriminatorInsertable() && getDiscriminator() != null ) {
			getDiscriminator().checkColumnDuplication( cols, owner );
		}
		final var softDeleteColumn = getRootClass().getSoftDeleteColumn();
		if ( softDeleteColumn != null ) {
			softDeleteColumn.getValue().checkColumnDuplication( cols, owner );
		}
		checkPropertyColumnDuplication( cols, getNonDuplicatedProperties(), owner );
		for ( var join : getJoins() ) {
			cols.clear();
			join.getKey().checkColumnDuplication( cols, owner );
			checkPropertyColumnDuplication( cols, join.getProperties(), owner );
		}
	}

	public abstract Object accept(PersistentClassVisitor mv);

	public String getJpaEntityName() {
		return jpaEntityName;
	}

	public void setJpaEntityName(String jpaEntityName) {
		this.jpaEntityName = jpaEntityName;
	}

	public boolean hasPojoRepresentation() {
		return getClassName() != null;
	}

	public boolean hasSubselectLoadableCollections() {
		return hasSubselectLoadableCollections;
	}

	public void setSubselectLoadableCollections(boolean hasSubselectCollections) {
		this.hasSubselectLoadableCollections = hasSubselectCollections;
	}

	public boolean hasCollectionNotReferencingPK() {
		return hasCollectionNotReferencingPK( properties );
	}

	private boolean hasCollectionNotReferencingPK(Collection<Property> properties) {
		for ( var property : properties ) {
			final var value = property.getValue();
			if ( value instanceof Component component ) {
				if ( hasCollectionNotReferencingPK( component.getProperties() ) ) {
					return true;
				}
			}
			else if ( value instanceof org.hibernate.mapping.Collection collection ) {
				if ( !( (CollectionType) collection.getType() ).useLHSPrimaryKey() ) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasPartitionedSelectionMapping() {
		final var superclass = getSuperclass();
		if ( superclass != null
			&& superclass.hasPartitionedSelectionMapping() ) {
			return true;
		}
		for ( var property : getProperties() ) {
			if ( property.getValue() instanceof BasicValue basicValue
					&& basicValue.isPartitionKey() ) {
				return true;
			}
		}
		return false;
	}

	public Component getIdentifierMapper() {
		return identifierMapper;
	}

	public Component getDeclaredIdentifierMapper() {
		return declaredIdentifierMapper;
	}

	public void setDeclaredIdentifierMapper(Component declaredIdentifierMapper) {
		this.declaredIdentifierMapper = declaredIdentifierMapper;
	}

	public boolean hasIdentifierMapper() {
		return identifierMapper != null;
	}

	public void addCallbackDefinitions(java.util.List<CallbackDefinition> callbackDefinitions) {
		if ( callbackDefinitions != null && !callbackDefinitions.isEmpty() ) {
			if ( this.callbackDefinitions == null ) {
				this.callbackDefinitions = new ArrayList<>();
			}
			this.callbackDefinitions.addAll( callbackDefinitions );
		}
	}

	public java.util.List<CallbackDefinition> getCallbackDefinitions() {
		return callbackDefinitions == null ? emptyList() : unmodifiableList( callbackDefinitions );
	}

	public void setIdentifierMapper(Component handle) {
		this.identifierMapper = handle;
	}

	private Boolean hasNaturalId;

	public boolean hasNaturalId() {
		if ( hasNaturalId == null ) {
			hasNaturalId = determineIfNaturalIdDefined();
		}
		return hasNaturalId;
	}

	private boolean determineIfNaturalIdDefined() {
		final List<Property> properties = getRootClass().getProperties();
		for ( var property : properties ) {
			if ( property.isNaturalIdentifier() ) {
				return true;
			}
		}
		return false;
	}

	// The following methods are added to support @MappedSuperclass in the metamodel
	public List<Property> getDeclaredProperties() {
		final ArrayList<List<Property>> lists = new ArrayList<>();
		lists.add( declaredProperties );
		for ( var join : joins ) {
			lists.add( join.getDeclaredProperties() );
		}
		return new JoinedList<>( lists );
	}

	public void addMappedSuperclassProperty(Property property) {
		properties.add( property );
		property.setPersistentClass( this );
	}

	public MappedSuperclass getSuperMappedSuperclass() {
		return superMappedSuperclass;
	}

	public void setSuperMappedSuperclass(MappedSuperclass superMappedSuperclass) {
		this.superMappedSuperclass = superMappedSuperclass;
	}

	public void assignCheckConstraintsToTable(Dialect dialect, TypeConfiguration types) {
		for ( var checkConstraint : checkConstraints ) {
			container( collectColumnNames( checkConstraint.getConstraint(), dialect, types ) )
					.getTable().addCheck( checkConstraint );
		}
	}

	// End of @MappedSuperclass support
	public void prepareForMappingModel(RuntimeModelCreationContext context) {
		if ( !joins.isEmpty() ) {
			// we need to deal with references to secondary tables
			// in SQL formulas
			final var dialect = context.getDialect();
			final var types = context.getTypeConfiguration();

			// now, move @Formulas to the correct AttributeContainers
			//TODO: skip this step for hbm.xml
			for ( var property : new ArrayList<>( properties ) ) {
				for ( var selectable : property.getSelectables() ) {
					if ( selectable.isFormula() && properties.contains( property ) ) {
						final var formula = (Formula) selectable;
						final var container =
								container( collectColumnNames( formula.getTemplate( dialect, types ) ) );
						if ( !container.contains( property ) ) {
							properties.remove( property );
							container.addProperty( property );
							break; //TODO: embeddables
						}
					}
				}
			}
		}
		properties.sort( comparing( Property::getName ) );
	}

	private AttributeContainer container(List<String> constrainedColumnNames) {
		final long matches = matchesInTable( constrainedColumnNames, getTable() );
		if ( matches == constrainedColumnNames.size() ) {
			// perfect, all columns matched in the primary table
			return this;
		}
		else {
			// go searching for a secondary table which better matches
			AttributeContainer result = this;
			long max = matches;
			for ( var join : getJoins() ) {
				long secondaryMatches = matchesInTable( constrainedColumnNames, join.getTable() );
				if ( secondaryMatches > max ) {
					result = join;
					max = secondaryMatches;
				}
			}
			return result;
		}
	}

	private static long matchesInTable(List<String> names, Table table) {
		return table.getColumns().stream()
				.filter( col -> col.isQuoted()
						? names.contains( col.getName() )
						: names.stream().anyMatch( name -> name.equalsIgnoreCase( col.getName() ) )
				)
				.count();
	}

	public void addCheckConstraint(CheckConstraint checkConstraint) {
		checkConstraints.add( checkConstraint );
	}

	public List<CheckConstraint> getCheckConstraints() {
		return checkConstraints;
	}


	public Table getImplicitTable() {
		return getTable();
	}

	@Override
	public Table findTable(String name) {
		if ( getTable().getName().equals( name ) ) {
			return getTable();
		}
		final var secondaryTable = findSecondaryTable( name );
		if ( secondaryTable != null ) {
			return secondaryTable.getTable();
		}
		return null;
	}

	@Override
	public Table getTable(String name) {
		final var table = findTable( name );
		if ( table == null ) {
			throw new MappingException( "Could not locate Table : " + name );
		}
		return table;
	}

	@Override
	public Join findSecondaryTable(String name) {
		for ( int i = 0; i < joins.size(); i++ ) {
			final var join = joins.get( i );
			if ( join.getTable().getNameIdentifier().matches( name ) ) {
				return join;
			}
		}
		return null;
	}

	@Override
	public Join getSecondaryTable(String name) {
		final var secondaryTable = findSecondaryTable( name );
		if ( secondaryTable == null ) {
			throw new MappingException( "Could not locate secondary Table : " + name );
		}
		return secondaryTable;
	}

	@Override
	public IdentifiableTypeClass getSuperType() {
		final var superPersistentClass = getSuperclass();
		if ( superPersistentClass != null ) {
			return superPersistentClass;
		}
		return superMappedSuperclass;
	}

	@Override
	public List<IdentifiableTypeClass> getSubTypes() {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void applyProperty(Property property) {
		final var table = property.getValue().getTable();
		if ( table.equals( getImplicitTable() ) ) {
			addProperty( property );
		}
		else {
			final var secondaryTable = getSecondaryTable( table.getName() );
			secondaryTable.addProperty( property );
		}
	}

	private boolean containsColumn(Column column) {
		for ( var declaredProperty : declaredProperties ) {
			if ( declaredProperty.getSelectables().contains( column ) ) {
				return true;
			}
		}
		return false;
	}

	@Internal
	public boolean isDefinedOnMultipleSubclasses(Column column) {
		PersistentClass declaringType = null;
		for ( var persistentClass : getSubclassClosure() ) {
			if ( persistentClass.containsColumn( column ) ) {
				if ( declaringType != null && declaringType != persistentClass ) {
					return true;
				}
				else {
					declaringType = persistentClass;
				}
			}
		}
		return false;
	}

	@Internal
	public PersistentClass getSuperPersistentClass() {
		final var superclass = getSuperclass();
		return superclass == null
				? getSuperPersistentClass( getSuperMappedSuperclass() )
				: superclass;
	}

	private static PersistentClass getSuperPersistentClass(MappedSuperclass mappedSuperclass) {
		if ( mappedSuperclass != null ) {
			final var superclass = mappedSuperclass.getSuperPersistentClass();
			return superclass == null
					? getSuperPersistentClass( mappedSuperclass.getSuperMappedSuperclass() )
					: superclass;
		}
		return null;
	}

	public Supplier<? extends Expectation> getInsertExpectation() {
		return insertExpectation;
	}

	public void setInsertExpectation(Supplier<? extends Expectation> insertExpectation) {
		this.insertExpectation = insertExpectation;
	}

	public Supplier<? extends Expectation> getUpdateExpectation() {
		return updateExpectation;
	}

	public void setUpdateExpectation(Supplier<? extends Expectation> updateExpectation) {
		this.updateExpectation = updateExpectation;
	}

	public Supplier<? extends Expectation> getDeleteExpectation() {
		return deleteExpectation;
	}

	public void setDeleteExpectation(Supplier<? extends Expectation> deleteExpectation) {
		this.deleteExpectation = deleteExpectation;
	}

	public void removeProperty(Property property) {
		if ( !declaredProperties.remove( property ) ) {
			throw new IllegalArgumentException( "Property not among declared properties: " + property.getName() );
		}
		properties.remove( property );
	}

	public void createConstraints(MetadataBuildingContext context) {
	}
}
