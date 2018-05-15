/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.boot.model.domain.internal.EntityJavaTypeMappingImpl;
import org.hibernate.boot.model.domain.internal.EntityMappingHierarchyImpl;
import org.hibernate.boot.model.domain.spi.EntityMappingHierarchyImplementor;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.SingletonIterator;

/**
 * The root class of an inheritance hierarchy
 *
 * @author Gavin King
 */
public class RootClass extends PersistentClass implements TableOwner {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( RootClass.class );

	public static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
	public static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";

	private KeyValue identifier;
	private boolean polymorphic;

	private String cacheConcurrencyStrategy;
	private String cacheRegionName;
	private boolean lazyPropertiesCacheable = true;
	private String naturalIdCacheRegionName;

	private boolean mutable = true;
	private boolean explicitPolymorphism;
	private Class entityPersisterClass;
	private boolean forceDiscriminator;
	private String where;
	private MappedTable table;
	private boolean discriminatorInsertable = true;
	private int nextSubclassId;

	public RootClass(MetadataBuildingContext metadataBuildingContext) {
		super( metadataBuildingContext, new EntityMappingHierarchyImpl() );
		setJavaTypeMapping( new EntityJavaTypeMappingImpl(
				metadataBuildingContext,
				this,
				null
		) );
		getEntityMappingHierarchy().setRootType( this );
	}

	@Override
	public int nextSubclassId() {
		return ++nextSubclassId;
	}

	@Override
	public int getSubclassId() {
		return 0;
	}

	@Override
	public void setMappedTable(MappedTable table) {
		this.table = table;
	}


	@Override
	public Table getTable() {
		return (Table)table;
	}

	@Override
	public MappedTable getMappedTable() {
		return table;
	}

	/**
	 * @deprecated since 6.0 use {@link #getIdentifierAttributeMapping()}.
	 */
	@Deprecated
	@Override
	public Property getIdentifierProperty() {
		return (Property) getIdentifierAttributeMapping();
	}

	@Override
	public Property getDeclaredIdentifierProperty() {
		return (Property) getDeclaredIdentifierAttributeMapping();
	}

	/**
	 * @deprecated since 6.0, use {@link #setDeclaredIdentifierAttributeMapping(PersistentAttributeMapping)}.
	 */
	@Deprecated
	public void setDeclaredIdentifierProperty(Property declaredIdentifierProperty) {
		setDeclaredIdentifierAttributeMapping( declaredIdentifierProperty );
	}

	@Override
	public void setDeclaredIdentifierAttributeMapping(PersistentAttributeMapping declaredIdentifierAttributeMapping) {
		getEntityMappingHierarchy().setIdentifierAttributeMapping( declaredIdentifierAttributeMapping );
		super.setDeclaredIdentifierAttributeMapping( declaredIdentifierAttributeMapping );
	}

	@Override
	public KeyValue getIdentifier() {
		return identifier;
	}

	@Override
	public boolean hasIdentifierProperty() {
		return getEntityMappingHierarchy().hasIdentifierAttributeMapping();
	}

	@Override
	public Value getDiscriminator() {
		return (Value) getEntityMappingHierarchy().getDiscriminatorMapping();
	}

	@Override
	public boolean isInherited() {
		return false;
	}

	@Override
	public boolean isPolymorphic() {
		return polymorphic;
	}

	public void setPolymorphic(boolean polymorphic) {
		this.polymorphic = polymorphic;
	}

	@Override
	public RootClass getRootClass() {
		return this;
	}

	@Override
	public Iterator getPropertyClosureIterator() {
		return getPropertyIterator();
	}

	@Override
	public Iterator getTableClosureIterator() {
		return new SingletonIterator( getTable() );
	}

	@Override
	public Iterator getKeyClosureIterator() {
		return new SingletonIterator( getKey() );
	}

	@Override
	public void addSubclass(Subclass subclass) throws MappingException {
		super.addSubclass( subclass );
		setPolymorphic( true );
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	/**
	 * @deprecated since 6.0, use {@link #getVersionAttributeMapping()}.
	 */
	@Deprecated
	@Override
	public Property getVersion() {
		return (Property) getVersionAttributeMapping();
	}

	@Override
	public Property getDeclaredVersion() {
		return (Property) getDeclaredVersionAttributeMapping();
	}

	/**
	 * @deprecated since 6.0, use {@link #setDeclaredVersionAttributeMapping(PersistentAttributeMapping)}.
	 */
	@Deprecated
	public void setDeclaredVersion(Property declaredVersion) {
		setDeclaredVersionAttributeMapping( declaredVersion );
	}

	/**
	 * @deprecated since 6.0, use {@link #setVersionAttributeMapping(PersistentAttributeMapping)}.
	 */
	@Deprecated
	public void setVersion(Property version) {
		setVersionAttributeMapping( version );
	}

	@Override
	public boolean isVersioned() {
		return hasVersionAttributeMapping();
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean hasEmbeddedIdentifier() {
		return getEntityMappingHierarchy().hasEmbeddedIdentifier();
	}

	@Override
	public Class getRuntimeEntityDescriptorClass() {
		return entityPersisterClass;
	}

	@Override
	public Table getRootTable() {
		return getTable();
	}

	@Override
	public void setEntityPersisterClass(Class persister) {
		this.entityPersisterClass = persister;
	}

	@Override
	public PersistentClass getSuperclass() {
		return null;
	}

	@Override
	public KeyValue getKey() {
		return getIdentifier();
	}

	/**
	 * @deprecated since 6.0, use {@link #setDiscriminatorValueMapping(ValueMapping)}.
	 */
	@Deprecated
	public void setDiscriminator(Value discriminator) {
		setDiscriminatorValueMapping( discriminator );
	}

	public void setDiscriminatorValueMapping(ValueMapping discriminatorValueMapping) {
		getEntityMappingHierarchy().setDiscriminatorMapping( discriminatorValueMapping );
	}

	/**
	 * @deprecated since 6.0, use {@link EntityMappingHierarchyImplementor#setEmbeddedIdentifier(boolean)}
	 */
	@Deprecated
	public void setEmbeddedIdentifier(boolean embeddedIdentifier) {
		getEntityMappingHierarchy().setEmbeddedIdentifier( embeddedIdentifier );
	}

	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
		this.explicitPolymorphism = explicitPolymorphism;
	}

	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
	}

	public void setIdentifierProperty(Property identifierProperty) {
		getEntityMappingHierarchy().setIdentifierAttributeMapping( identifierProperty );
		// todo: (6.0) do we need this going forward?
		identifierProperty.setPersistentClass( this );
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	@Override
	public boolean isDiscriminatorInsertable() {
		return discriminatorInsertable;
	}

	public void setDiscriminatorInsertable(boolean insertable) {
		this.discriminatorInsertable = insertable;
	}

	@Override
	public boolean isForceDiscriminator() {
		return forceDiscriminator;
	}

	public void setForceDiscriminator(boolean forceDiscriminator) {
		this.forceDiscriminator = forceDiscriminator;
	}

	@Override
	public String getWhere() {
		return where;
	}

	public void setWhere(String string) {
		where = string;
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return cacheConcurrencyStrategy;
	}

	public void setCacheConcurrencyStrategy(String cacheConcurrencyStrategy) {
		this.cacheConcurrencyStrategy = cacheConcurrencyStrategy;
	}

	public String getCacheRegionName() {
		return cacheRegionName == null ? getEntityName() : cacheRegionName;
	}

	public void setCacheRegionName(String cacheRegionName) {
		this.cacheRegionName = StringHelper.nullIfEmpty( cacheRegionName );
	}

	public boolean isLazyPropertiesCacheable() {
		return lazyPropertiesCacheable;
	}

	public void setLazyPropertiesCacheable(boolean lazyPropertiesCacheable) {
		this.lazyPropertiesCacheable = lazyPropertiesCacheable;
	}

	@Override
	public String getNaturalIdCacheRegionName() {
		return naturalIdCacheRegionName;
	}

	public void setNaturalIdCacheRegionName(String naturalIdCacheRegionName) {
		this.naturalIdCacheRegionName = naturalIdCacheRegionName;
	}

	@Override
	public boolean isJoinedSubclass() {
		return false;
	}

	@Override
	public java.util.Set getSynchronizedTables() {
		return synchronizedTables;
	}

	@SuppressWarnings("UnnecessaryUnboxing")
	public Set<MappedTable> getIdentityTables() {
		Set<MappedTable> tables = new HashSet<>();
		Iterator iter = getSubclassClosureIterator();
		while ( iter.hasNext() ) {
			PersistentClass clazz = (PersistentClass) iter.next();
			if ( clazz.isAbstract() == null || !clazz.isAbstract().booleanValue() ) {
				tables.add( clazz.getIdentityTable() );
			}
		}
		return tables;
	}

	@Override
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept( this );
	}

}
