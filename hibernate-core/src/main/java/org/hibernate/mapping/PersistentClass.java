/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.EmptyIterator;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.collections.SingletonIterator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.Alias;

/**
 * Mapping for an entity.
 *
 * @author Gavin King
 */
public abstract class PersistentClass implements AttributeContainer, Serializable, Filterable, MetaAttributable {
	private static final Alias PK_ALIAS = new Alias( 15, "PK" );

	public static final String NULL_DISCRIMINATOR_MAPPING = "null";
	public static final String NOT_NULL_DISCRIMINATOR_MAPPING = "not null";

	private final MetadataBuildingContext metadataBuildingContext;

	private String entityName;

	private String className;
	private transient Class mappedClass;

	private String proxyInterfaceName;
	private transient Class proxyInterface;

	private String nodeName;
	private String jpaEntityName;

	private String discriminatorValue;
	private boolean lazy;
	private ArrayList properties = new ArrayList();
	private ArrayList declaredProperties = new ArrayList();
	private final ArrayList<Subclass> subclasses = new ArrayList<Subclass>();
	private final ArrayList subclassProperties = new ArrayList();
	private final ArrayList subclassTables = new ArrayList();
	private boolean dynamicInsert;
	private boolean dynamicUpdate;
	private int batchSize = -1;
	private boolean selectBeforeUpdate;
	private java.util.Map metaAttributes;
	private ArrayList<Join> joins = new ArrayList<Join>();
	private final ArrayList subclassJoins = new ArrayList();
	private final java.util.List filters = new ArrayList();
	protected final java.util.Set synchronizedTables = new HashSet();
	private String loaderName;
	private Boolean isAbstract;
	private boolean hasSubselectLoadableCollections;
	private Component identifierMapper;

	// Custom SQL
	private String customSQLInsert;
	private boolean customInsertCallable;
	private ExecuteUpdateResultCheckStyle insertCheckStyle;
	private String customSQLUpdate;
	private boolean customUpdateCallable;
	private ExecuteUpdateResultCheckStyle updateCheckStyle;
	private String customSQLDelete;
	private boolean customDeleteCallable;
	private ExecuteUpdateResultCheckStyle deleteCheckStyle;

	private java.util.Map tuplizerImpls;

	private MappedSuperclass superMappedSuperclass;
	private Component declaredIdentifierMapper;
	private OptimisticLockStyle optimisticLockStyle;

	public PersistentClass(MetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
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

	public Class getMappedClass() throws MappingException {
		if ( className == null ) {
			return null;
		}

		try {
			if ( mappedClass == null ) {
				mappedClass = metadataBuildingContext.getClassLoaderAccess().classForName( className );
			}
			return mappedClass;
		}
		catch (ClassLoadingException e) {
			throw new MappingException( "entity class not found: " + className, e );
		}
	}

	public Class getProxyInterface() {
		if ( proxyInterfaceName == null ) {
			return null;
		}
		try {
			if ( proxyInterface == null ) {
				proxyInterface = metadataBuildingContext.getClassLoaderAccess().classForName( proxyInterfaceName );
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
						"Circular inheritance mapping detected: " +
								subclass.getEntityName() +
								" will have it self as superclass when extending " +
								getEntityName()
				);
			}
			superclass = superclass.getSuperclass();
		}
		subclasses.add( subclass );
	}

	public boolean hasSubclasses() {
		return subclasses.size() > 0;
	}

	public int getSubclassSpan() {
		int n = subclasses.size();
		for ( Subclass subclass : subclasses ) {
			n += subclass.getSubclassSpan();
		}
		return n;
	}

	/**
	 * Iterate over subclasses in a special 'order', most derived subclasses
	 * first.
	 */
	public Iterator getSubclassIterator() {
		Iterator[] iters = new Iterator[subclasses.size() + 1];
		Iterator iter = subclasses.iterator();
		int i = 0;
		while ( iter.hasNext() ) {
			iters[i++] = ( (Subclass) iter.next() ).getSubclassIterator();
		}
		iters[i] = subclasses.iterator();
		return new JoinedIterator( iters );
	}

	public Iterator getSubclassClosureIterator() {
		ArrayList iters = new ArrayList();
		iters.add( new SingletonIterator( this ) );
		Iterator iter = getSubclassIterator();
		while ( iter.hasNext() ) {
			PersistentClass clazz = (PersistentClass) iter.next();
			iters.add( clazz.getSubclassClosureIterator() );
		}
		return new JoinedIterator( iters );
	}

	public Table getIdentityTable() {
		return getRootTable();
	}

	public Iterator getDirectSubclasses() {
		return subclasses.iterator();
	}

	@Override
	public void addProperty(Property p) {
		properties.add( p );
		declaredProperties.add( p );
		p.setPersistentClass( this );
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

	public abstract String getNaturalIdCacheRegionName();

	public abstract String getCacheConcurrencyStrategy();

	public abstract PersistentClass getSuperclass();

	public abstract boolean isExplicitPolymorphism();

	public abstract boolean isDiscriminatorInsertable();

	public abstract Iterator getPropertyClosureIterator();

	public abstract Iterator getTableClosureIterator();

	public abstract Iterator getKeyClosureIterator();

	protected void addSubclassProperty(Property prop) {
		subclassProperties.add( prop );
	}

	protected void addSubclassJoin(Join join) {
		subclassJoins.add( join );
	}

	protected void addSubclassTable(Table subclassTable) {
		subclassTables.add( subclassTable );
	}

	public Iterator getSubclassPropertyClosureIterator() {
		ArrayList iters = new ArrayList();
		iters.add( getPropertyClosureIterator() );
		iters.add( subclassProperties.iterator() );
		for ( int i = 0; i < subclassJoins.size(); i++ ) {
			Join join = (Join) subclassJoins.get( i );
			iters.add( join.getPropertyIterator() );
		}
		return new JoinedIterator( iters );
	}

	public Iterator getSubclassJoinClosureIterator() {
		return new JoinedIterator( getJoinClosureIterator(), subclassJoins.iterator() );
	}

	public Iterator getSubclassTableClosureIterator() {
		return new JoinedIterator( getTableClosureIterator(), subclassTables.iterator() );
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

	public abstract boolean hasEmbeddedIdentifier();

	public abstract Class getEntityPersisterClass();

	public abstract void setEntityPersisterClass(Class classPersisterClass);

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
		PrimaryKey pk = new PrimaryKey();
		Table table = getTable();
		pk.setTable( table );
		pk.setName( PK_ALIAS.toAliasString( table.getName() ) );
		table.setPrimaryKey( pk );

		pk.addColumns( getKey().getColumnIterator() );
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
	 * Build an iterator of properties which are "referenceable".
	 *
	 * @return The property iterator.
	 *
	 * @see #getReferencedProperty for a discussion of "referenceable"
	 */
	public Iterator getReferenceablePropertyIterator() {
		return getPropertyClosureIterator();
	}

	/**
	 * Given a property path, locate the appropriate referenceable property reference.
	 * <p/>
	 * A referenceable property is a property  which can be a target of a foreign-key
	 * mapping (an identifier or explcitly named in a property-ref).
	 *
	 * @param propertyPath The property path to resolve into a property reference.
	 *
	 * @return The property reference (never null).
	 *
	 * @throws MappingException If the property could not be found.
	 */
	public Property getReferencedProperty(String propertyPath) throws MappingException {
		try {
			return getRecursiveProperty( propertyPath, getReferenceablePropertyIterator() );
		}
		catch (MappingException e) {
			throw new MappingException(
					"property-ref [" + propertyPath + "] not found on entity [" + getEntityName() + "]", e
			);
		}
	}

	public Property getRecursiveProperty(String propertyPath) throws MappingException {
		try {
			return getRecursiveProperty( propertyPath, getPropertyIterator() );
		}
		catch (MappingException e) {
			throw new MappingException(
					"property [" + propertyPath + "] not found on entity [" + getEntityName() + "]", e
			);
		}
	}

	private Property getRecursiveProperty(String propertyPath, Iterator iter) throws MappingException {
		Property property = null;
		StringTokenizer st = new StringTokenizer( propertyPath, ".", false );
		try {
			while ( st.hasMoreElements() ) {
				final String element = (String) st.nextElement();
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
							identifierProperty = getProperty( element, getIdentifierMapper().getPropertyIterator() );
							if ( identifierProperty != null ) {
								// the root of the incoming property path matched one
								// of the embedded composite identifier properties
								property = identifierProperty;
							}
						}
						catch (MappingException ignore) {
							// ignore it...
						}
					}

					if ( property == null ) {
						property = getProperty( element, iter );
					}
				}
				else {
					//flat recursive algorithm
					property = ( (Component) property.getValue() ).getProperty( element );
				}
			}
		}
		catch (MappingException e) {
			throw new MappingException( "property [" + propertyPath + "] not found on entity [" + getEntityName() + "]" );
		}

		return property;
	}

	private Property getProperty(String propertyName, Iterator iterator) throws MappingException {
		if ( iterator.hasNext() ) {
			String root = StringHelper.root( propertyName );
			while ( iterator.hasNext() ) {
				Property prop = (Property) iterator.next();
				if ( prop.getName().equals( root ) ) {
					return prop;
				}
			}
		}
		throw new MappingException( "property [" + propertyName + "] not found on entity [" + getEntityName() + "]" );
	}

	public Property getProperty(String propertyName) throws MappingException {
		Iterator iter = getPropertyClosureIterator();
		Property identifierProperty = getIdentifierProperty();
		if ( identifierProperty != null
				&& identifierProperty.getName().equals( StringHelper.root( propertyName ) ) ) {
			return identifierProperty;
		}
		else {
			return getProperty( propertyName, iter );
		}
	}

	/**
	 * @deprecated prefer {@link #getOptimisticLockStyle}
	 */
	@Deprecated
	public int getOptimisticLockMode() {
		return getOptimisticLockStyle().getOldCode();
	}

	/**
	 * @deprecated prefer {@link #setOptimisticLockStyle}
	 */
	@Deprecated
	public void setOptimisticLockMode(int optimisticLockMode) {
		setOptimisticLockStyle( OptimisticLockStyle.interpretOldCode( optimisticLockMode ) );
	}

	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	public void setOptimisticLockStyle(OptimisticLockStyle optimisticLockStyle) {
		this.optimisticLockStyle = optimisticLockStyle;
	}

	public void validate(Mapping mapping) throws MappingException {
		Iterator iter = getPropertyIterator();
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			if ( !prop.isValid( mapping ) ) {
				throw new MappingException(
						"property mapping has wrong number of columns: " +
								StringHelper.qualify( getEntityName(), prop.getName() ) +
								" type: " +
								prop.getType().getName()
				);
			}
		}
		checkPropertyDuplication();
		checkColumnDuplication();
	}

	private void checkPropertyDuplication() throws MappingException {
		HashSet<String> names = new HashSet<String>();
		Iterator iter = getPropertyIterator();
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			if ( !names.add( prop.getName() ) ) {
				throw new MappingException( "Duplicate property mapping of " + prop.getName() + " found in " + getEntityName() );
			}
		}
	}

	public boolean isDiscriminatorValueNotNull() {
		return NOT_NULL_DISCRIMINATOR_MAPPING.equals( getDiscriminatorValue() );
	}

	public boolean isDiscriminatorValueNull() {
		return NULL_DISCRIMINATOR_MAPPING.equals( getDiscriminatorValue() );
	}

	public java.util.Map getMetaAttributes() {
		return metaAttributes;
	}

	public void setMetaAttributes(java.util.Map metas) {
		this.metaAttributes = metas;
	}

	public MetaAttribute getMetaAttribute(String name) {
		return metaAttributes == null
				? null
				: (MetaAttribute) metaAttributes.get( name );
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + getEntityName() + ')';
	}

	public Iterator getJoinIterator() {
		return joins.iterator();
	}

	public Iterator getJoinClosureIterator() {
		return joins.iterator();
	}

	public void addJoin(Join join) {
		joins.add( join );
		join.setPersistentClass( this );
	}

	public int getJoinClosureSpan() {
		return joins.size();
	}

	public int getPropertyClosureSpan() {
		int span = properties.size();
		for ( Join join : joins ) {
			span += join.getPropertySpan();
		}
		return span;
	}

	public int getJoinNumber(Property prop) {
		int result = 1;
		Iterator iter = getSubclassJoinClosureIterator();
		while ( iter.hasNext() ) {
			Join join = (Join) iter.next();
			if ( join.containsProperty( prop ) ) {
				return result;
			}
			result++;
		}
		return 0;
	}

	/**
	 * Build an iterator over the properties defined on this class.  The returned
	 * iterator only accounts for "normal" properties (i.e. non-identifier
	 * properties).
	 * <p/>
	 * Differs from {@link #getUnjoinedPropertyIterator} in that the iterator
	 * we return here will include properties defined as part of a join.
	 *
	 * @return An iterator over the "normal" properties.
	 */
	public Iterator getPropertyIterator() {
		ArrayList iterators = new ArrayList();
		iterators.add( properties.iterator() );
		for ( int i = 0; i < joins.size(); i++ ) {
			Join join = (Join) joins.get( i );
			iterators.add( join.getPropertyIterator() );
		}
		return new JoinedIterator( iterators );
	}

	/**
	 * Build an iterator over the properties defined on this class <b>which
	 * are not defined as part of a join</b>.  As with {@link #getPropertyIterator},
	 * the returned iterator only accounts for non-identifier properties.
	 *
	 * @return An iterator over the non-joined "normal" properties.
	 */
	public Iterator getUnjoinedPropertyIterator() {
		return properties.iterator();
	}

	public void setCustomSQLInsert(String customSQLInsert, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLInsert = customSQLInsert;
		this.customInsertCallable = callable;
		this.insertCheckStyle = checkStyle;
	}

	public String getCustomSQLInsert() {
		return customSQLInsert;
	}

	public boolean isCustomInsertCallable() {
		return customInsertCallable;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLInsertCheckStyle() {
		return insertCheckStyle;
	}

	public void setCustomSQLUpdate(String customSQLUpdate, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLUpdate = customSQLUpdate;
		this.customUpdateCallable = callable;
		this.updateCheckStyle = checkStyle;
	}

	public String getCustomSQLUpdate() {
		return customSQLUpdate;
	}

	public boolean isCustomUpdateCallable() {
		return customUpdateCallable;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLUpdateCheckStyle() {
		return updateCheckStyle;
	}

	public void setCustomSQLDelete(String customSQLDelete, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLDelete = customSQLDelete;
		this.customDeleteCallable = callable;
		this.deleteCheckStyle = checkStyle;
	}

	public String getCustomSQLDelete() {
		return customSQLDelete;
	}

	public boolean isCustomDeleteCallable() {
		return customDeleteCallable;
	}

	public ExecuteUpdateResultCheckStyle getCustomSQLDeleteCheckStyle() {
		return deleteCheckStyle;
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

	public java.util.List getFilters() {
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

	public abstract java.util.Set getSynchronizedTables();

	public void addSynchronizedTable(String table) {
		synchronizedTables.add( table );
	}

	public Boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(Boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	protected void checkColumnDuplication(Set distinctColumns, Iterator columns)
			throws MappingException {
		while ( columns.hasNext() ) {
			Selectable columnOrFormula = (Selectable) columns.next();
			if ( !columnOrFormula.isFormula() ) {
				Column col = (Column) columnOrFormula;
				if ( !distinctColumns.add( col.getName() ) ) {
					throw new MappingException(
							"Repeated column in mapping for entity: " +
									getEntityName() +
									" column: " +
									col.getName() +
									" (should be mapped with insert=\"false\" update=\"false\")"
					);
				}
			}
		}
	}

	protected void checkPropertyColumnDuplication(Set distinctColumns, Iterator properties)
			throws MappingException {
		while ( properties.hasNext() ) {
			Property prop = (Property) properties.next();
			if ( prop.getValue() instanceof Component ) { //TODO: remove use of instanceof!
				Component component = (Component) prop.getValue();
				checkPropertyColumnDuplication( distinctColumns, component.getPropertyIterator() );
			}
			else {
				if ( prop.isUpdateable() || prop.isInsertable() ) {
					checkColumnDuplication( distinctColumns, prop.getColumnIterator() );
				}
			}
		}
	}

	protected Iterator getNonDuplicatedPropertyIterator() {
		return getUnjoinedPropertyIterator();
	}

	protected Iterator getDiscriminatorColumnIterator() {
		return EmptyIterator.INSTANCE;
	}

	protected void checkColumnDuplication() {
		HashSet cols = new HashSet();
		if ( getIdentifierMapper() == null ) {
			//an identifier mapper => getKey will be included in the getNonDuplicatedPropertyIterator()
			//and checked later, so it needs to be excluded
			checkColumnDuplication( cols, getKey().getColumnIterator() );
		}
		checkColumnDuplication( cols, getDiscriminatorColumnIterator() );
		checkPropertyColumnDuplication( cols, getNonDuplicatedPropertyIterator() );
		Iterator iter = getJoinIterator();
		while ( iter.hasNext() ) {
			cols.clear();
			Join join = (Join) iter.next();
			checkColumnDuplication( cols, join.getKey().getColumnIterator() );
			checkPropertyColumnDuplication( cols, join.getPropertyIterator() );
		}
	}

	public abstract Object accept(PersistentClassVisitor mv);

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getJpaEntityName() {
		return jpaEntityName;
	}

	public void setJpaEntityName(String jpaEntityName) {
		this.jpaEntityName = jpaEntityName;
	}

	public boolean hasPojoRepresentation() {
		return getClassName() != null;
	}

	public boolean hasDom4jRepresentation() {
		return getNodeName() != null;
	}

	public boolean hasSubselectLoadableCollections() {
		return hasSubselectLoadableCollections;
	}

	public void setSubselectLoadableCollections(boolean hasSubselectCollections) {
		this.hasSubselectLoadableCollections = hasSubselectCollections;
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

	public void setIdentifierMapper(Component handle) {
		this.identifierMapper = handle;
	}

	public void addTuplizer(EntityMode entityMode, String implClassName) {
		if ( tuplizerImpls == null ) {
			tuplizerImpls = new HashMap();
		}
		tuplizerImpls.put( entityMode, implClassName );
	}

	public String getTuplizerImplClassName(EntityMode mode) {
		if ( tuplizerImpls == null ) {
			return null;
		}
		return (String) tuplizerImpls.get( mode );
	}

	public java.util.Map getTuplizerMap() {
		if ( tuplizerImpls == null ) {
			return null;
		}
		return java.util.Collections.unmodifiableMap( tuplizerImpls );
	}

	public boolean hasNaturalId() {
		Iterator props = getRootClass().getPropertyIterator();
		while ( props.hasNext() ) {
			if ( ( (Property) props.next() ).isNaturalIdentifier() ) {
				return true;
			}
		}
		return false;
	}

	public abstract boolean isLazyPropertiesCacheable();

	// The following methods are added to support @MappedSuperclass in the metamodel
	public Iterator getDeclaredPropertyIterator() {
		ArrayList iterators = new ArrayList();
		iterators.add( declaredProperties.iterator() );
		for ( int i = 0; i < joins.size(); i++ ) {
			Join join = (Join) joins.get( i );
			iterators.add( join.getDeclaredPropertyIterator() );
		}
		return new JoinedIterator( iterators );
	}

	public void addMappedsuperclassProperty(Property p) {
		properties.add( p );
		p.setPersistentClass( this );
	}

	public MappedSuperclass getSuperMappedSuperclass() {
		return superMappedSuperclass;
	}

	public void setSuperMappedSuperclass(MappedSuperclass superMappedSuperclass) {
		this.superMappedSuperclass = superMappedSuperclass;
	}

	// End of @Mappedsuperclass support

}
