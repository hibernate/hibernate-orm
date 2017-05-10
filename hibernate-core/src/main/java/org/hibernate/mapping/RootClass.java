/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.boot.model.domain.internal.EntityMappingHierarchyImpl;
import org.hibernate.boot.model.domain.spi.EntityMappingHierarchyImplementor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
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
	private String naturalIdCacheRegionName;
	private boolean lazyPropertiesCacheable = true;
	private boolean mutable = true;
	private boolean explicitPolymorphism;
	private Class entityPersisterClass;
	private boolean forceDiscriminator;
	private String where;
	private Table table;
	private boolean discriminatorInsertable = true;
	private int nextSubclassId;
	private PersistentAttributeMapping declaredIdentifierAttributeMapping;
	private PersistentAttributeMapping declaredVersionAttributeMapping;
	private boolean cachingExplicitlyRequested;

	public RootClass(MetadataBuildingContext metadataBuildingContext) {
		super( metadataBuildingContext, new EntityMappingHierarchyImpl() );
		getEntityMappingHierarchy().setRootType( this );
	}

	@Override
	int nextSubclassId() {
		return ++nextSubclassId;
	}

	@Override
	public int getSubclassId() {
		return 0;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	@Override
	public Table getTable() {
		return table;
	}

	@Override
	public Property getIdentifierProperty() {
		return (Property) getIdentifierAttributeMapping();
	}

	public PersistentAttributeMapping getIdentifierAttributeMapping() {
		return getEntityMappingHierarchy().getIdentifierAttributeMapping();
	}

	@Override
	public Property getDeclaredIdentifierProperty() {
		return (Property) getDeclaredIdentifierAttributeMapping();
	}

	public PersistentAttributeMapping getDeclaredIdentifierAttributeMapping() {
		return declaredIdentifierAttributeMapping;
	}

	/**
	 * @deprecated since 6.0, use {@link #setDeclaredIdentifierAttributeMapping()}.
	 */
	@Deprecated
	public void setDeclaredIdentifierProperty(Property declaredIdentifierProperty) {
		setDeclaredIdentifierAttributeMapping( declaredIdentifierProperty );
	}

	public void setDeclaredIdentifierAttributeMapping(PersistentAttributeMapping declaredIdentifierAttributeMapping) {
		this.declaredIdentifierAttributeMapping = declaredIdentifierAttributeMapping;
		getEntityMappingHierarchy().setIdentifierAttributeMapping( declaredIdentifierAttributeMapping );
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

	@Override
	public Property getVersion() {
		return (Property) getEntityMappingHierarchy().getVersionAttributeMapping();
	}

	public PersistentAttributeMapping getDeclaredVersionAttributeMapping() {
		return declaredVersionAttributeMapping;
	}

	@Override
	public Property getDeclaredVersion() {
		return (Property) getDeclaredVersionAttributeMapping();
	}

	public void setDeclaredVersionAttributeMapping(PersistentAttributeMapping versionAttributeMapping) {
		setVersionAttributeMapping( versionAttributeMapping );
		this.declaredVersionAttributeMapping = versionAttributeMapping;
	}

	/**
	 * @deprecated since 6.0, use {@link #setDeclaredVersionAttributeMapping(PersistentAttributeMapping)}.
	 */
	@Deprecated
	public void setDeclaredVersion(Property declaredVersion) {
		setDeclaredVersionAttributeMapping( declaredVersion );
	}

	public void setVersionAttributeMapping(PersistentAttributeMapping versionAttributeMapping) {
		getEntityMappingHierarchy().setVersionAttributeMapping( versionAttributeMapping );
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
		return getEntityMappingHierarchy().isVersioned();
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
	public Class getEntityPersisterClass() {
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
	public void validate(Mapping mapping) throws MappingException {
		super.validate( mapping );
		if ( !getIdentifier().isValid( mapping ) ) {
			throw new MappingException(
					"identifier mapping has wrong number of columns: " +
							getEntityName() +
							" type: " +
							getIdentifier().getType().getName()
			);
		}
		checkCompositeIdentifier();
	}

	private void checkCompositeIdentifier() {
		if ( getIdentifier() instanceof Component ) {
			Component id = (Component) getIdentifier();
			if ( !id.isDynamic() ) {
				final Class idClass = id.getComponentClass();
				if ( idClass != null ) {
					final String idComponentClassName = idClass.getName();
					if ( !ReflectHelper.overridesEquals( idClass ) ) {
						LOG.compositeIdClassDoesNotOverrideEquals( idComponentClassName );
					}
					if ( !ReflectHelper.overridesHashCode( idClass ) ) {
						LOG.compositeIdClassDoesNotOverrideHashCode( idComponentClassName );
					}
					if ( !Serializable.class.isAssignableFrom( idClass ) ) {
						throw new MappingException(
								"Composite-id class must implement Serializable: " + idComponentClassName
						);
					}
				}
			}
		}
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
		this.cacheRegionName = cacheRegionName;
	}

	@Override
	public String getNaturalIdCacheRegionName() {
		return naturalIdCacheRegionName;
	}

	public void setNaturalIdCacheRegionName(String naturalIdCacheRegionName) {
		this.naturalIdCacheRegionName = naturalIdCacheRegionName;
	}

	@Override
	public boolean isLazyPropertiesCacheable() {
		return lazyPropertiesCacheable;
	}

	public void setLazyPropertiesCacheable(boolean lazyPropertiesCacheable) {
		this.lazyPropertiesCacheable = lazyPropertiesCacheable;
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
	public Set<Table> getIdentityTables() {
		Set<Table> tables = new HashSet<Table>();
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

	public void setCachingExplicitlyRequested(boolean explicitlyRequested) {
		this.cachingExplicitlyRequested = explicitlyRequested;
	}

	public boolean isCachingExplicitlyRequested() {
		return cachingExplicitlyRequested;
	}
}
