/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataBuildingContext;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.ReflectHelper.overridesEquals;
import static org.hibernate.internal.util.ReflectHelper.overridesHashCode;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

/**
 * A mapping model object that represents the root class in an entity class
 * {@linkplain jakarta.persistence.Inheritance inheritance} hierarchy.
 *
 * @author Gavin King
 */
public final class RootClass extends PersistentClass implements TableOwner, SoftDeletable {

	private Property identifierProperty;
	private KeyValue identifier;
	private Property version;
	private boolean polymorphic;

	private String cacheConcurrencyStrategy;
	private String cacheRegionName;
	private boolean lazyPropertiesCacheable = true;
	private String naturalIdCacheRegionName;

	private Value discriminator;
	private boolean mutable = true;
	private boolean embeddedIdentifier;
	private boolean forceDiscriminator;
	private boolean concreteProxy;
	private String where;
	private Table table;
	private boolean discriminatorInsertable = true;
	private int nextSubclassId;
	private Property declaredIdentifierProperty;
	private Property declaredVersion;
	private Column softDeleteColumn;
	private SoftDeleteType softDeleteStrategy;

	public RootClass(MetadataBuildingContext buildingContext) {
		super( buildingContext );
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
		return identifierProperty;
	}

	@Override
	public Property getDeclaredIdentifierProperty() {
		return declaredIdentifierProperty;
	}

	public void setDeclaredIdentifierProperty(Property declaredIdentifierProperty) {
		this.declaredIdentifierProperty = declaredIdentifierProperty;
	}

	@Override
	public KeyValue getIdentifier() {
		return identifier;
	}

	@Override
	public boolean hasIdentifierProperty() {
		return identifierProperty != null;
	}

	public boolean hasDiscriminator() {
		return discriminator != null;
	}

	@Override
	public Value getDiscriminator() {
		return discriminator;
	}

	@Override
	public boolean isInherited() {
		return false;
	}

	@Override
	public boolean isPolymorphic() {
		return polymorphic;
	}

	/**
	 * @deprecated No longer supported
	 */
	@Deprecated
	public void setPolymorphic(boolean polymorphic) {
		this.polymorphic = polymorphic;
	}

	@Override
	public RootClass getRootClass() {
		return this;
	}

	@Override
	public List<Property> getPropertyClosure() {
		return getProperties();
	}

	@Override
	public List<Table> getTableClosure() {
		return List.of( getTable() );
	}

	@Override
	public List<KeyValue> getKeyClosure() {
		return List.of( getKey() );
	}

	@Override
	public void addSubclass(Subclass subclass) throws MappingException {
		super.addSubclass( subclass );
		setPolymorphic( true );
	}

	@Override
	public Property getVersion() {
		return version;
	}

	@Override
	public Property getDeclaredVersion() {
		return declaredVersion;
	}

	public void setDeclaredVersion(Property declaredVersion) {
		this.declaredVersion = declaredVersion;
	}

	public void setVersion(Property version) {
		this.version = version;
	}

	@Override
	public boolean isVersioned() {
		return version != null;
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean hasEmbeddedIdentifier() {
		return embeddedIdentifier;
	}

	@Override
	public Table getRootTable() {
		return getTable();
	}

	@Override
	public PersistentClass getSuperclass() {
		return null;
	}

	@Override
	public KeyValue getKey() {
		return getIdentifier();
	}

	public void setDiscriminator(Value discriminator) {
		this.discriminator = discriminator;
	}

	public void setEmbeddedIdentifier(boolean embeddedIdentifier) {
		this.embeddedIdentifier = embeddedIdentifier;
	}

	/**
	 * @deprecated No longer supported
	 */
	@Deprecated(since = "7", forRemoval = true)
	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
	}

	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
	}

	public void setIdentifierProperty(Property identifierProperty) {
		this.identifierProperty = identifierProperty;
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
	public boolean isConcreteProxy() {
		return concreteProxy;
	}

	public void setConcreteProxy(boolean concreteProxy) {
		this.concreteProxy = concreteProxy;
	}

	@Override
	public String getWhere() {
		return where;
	}

	public void setWhere(String string) {
		where = string;
	}

	@Override
	public void validate(Metadata mapping) throws MappingException {
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
		checkTableDuplication();
	}

	/**
	 * In {@linkplain jakarta.persistence.InheritanceType#SINGLE_TABLE single table}
	 * inheritance, subclasses share a table with the root class by definition. But
	 * for {@linkplain jakarta.persistence.InheritanceType#JOINED joined} or
	 * {@linkplain jakarta.persistence.InheritanceType#TABLE_PER_CLASS union} mappings,
	 * the subclasses are assumed to occupy distinct tables, and it's an error to map
	 * two subclasses to the same table.
	 * <p>
	 * As a special exception to this, if a joined inheritance hierarchy defines an
	 * explicit {@link jakarta.persistence.DiscriminatorColumn}, we tolerate table
	 * duplication among the subclasses, but we must "force" the discriminator to
	 * account for this. (See issue HHH-14526.)
	 */
	private void checkTableDuplication() {
		if ( hasSubclasses() ) {
			final Set<Table> tables = new HashSet<>();
			tables.add( getTable() );
			for ( Subclass subclass : getSubclasses() ) {
				if ( !(subclass instanceof SingleTableSubclass) ) {
					final var table = subclass.getTable();
					if ( !tables.add( table ) ) {
						// we encountered a duplicate table mapping
						if ( getDiscriminator() == null ) {
							throw new MappingException( "Two different subclasses of '" + getEntityName()
									+ "' map to the table '" + table.getName()
									+ "' and the hierarchy has no discriminator column" );
						}
						else {
							// This is arguably not the right place to do this.
							// Perhaps it's an issue better dealt with later on
							// by the persisters. See HHH-14526.
							forceDiscriminator = true;
						}
						break;
					}
				}
			}
		}
	}

	/**
	 * Composite id classes are supposed to override {@link #equals} and
	 * {@link #hashCode}, and programs will typically experience bugs if
	 * they don't. But instead of actually enforcing this with an error
	 * (because we can't anyway verify that the implementation is actually
	 * <em>correct</em>) we simply log a warning.
	 */
	private void checkCompositeIdentifier() {
		if ( getIdentifier() instanceof Component id
				&& !id.isDynamic() ) {
			final var idClass = id.getComponentClass();
			if ( idClass != null ) {
				if ( !overridesEquals( idClass ) ) {
					CORE_LOGGER.compositeIdClassDoesNotOverrideEquals( idClass.getName() );
				}
				else if ( !overridesHashCode( idClass ) ) {
					CORE_LOGGER.compositeIdClassDoesNotOverrideHashCode( idClass.getName() );
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
		this.cacheRegionName = nullIfEmpty( cacheRegionName );
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
	public Set<String> getSynchronizedTables() {
		return synchronizedTables;
	}

	public Set<Table> getIdentityTables() {
		final Set<Table> tables = new HashSet<>();
		for ( var clazz : getSubclassClosure() ) {
			if ( clazz.isAbstract() == null || !clazz.isAbstract() ) {
				tables.add( clazz.getIdentityTable() );
			}
		}
		return tables;
	}

	@Override
	public void enableSoftDelete(Column indicatorColumn, SoftDeleteType strategy) {
		softDeleteColumn = indicatorColumn;
		softDeleteStrategy = strategy;
	}

	@Override
	public Column getSoftDeleteColumn() {
		return softDeleteColumn;
	}

	@Override
	public SoftDeleteType getSoftDeleteStrategy() {
		return softDeleteStrategy;
	}

	@Override
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept( this );
	}

}
