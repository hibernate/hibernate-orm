/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.FilterConfiguration;
import org.hibernate.internal.util.collections.JoinedList;

/**
 * A mapping model object that represents a subclass in an entity class
 * {@linkplain jakarta.persistence.Inheritance inheritance} hierarchy.
 *
 * @author Gavin King
 */
public sealed class Subclass extends PersistentClass
		permits SingleTableSubclass, JoinedSubclass, UnionSubclass {

	private PersistentClass superclass;
	private final int subclassId;

	public Subclass(PersistentClass superclass, MetadataBuildingContext buildingContext) {
		super( buildingContext );
		this.superclass = superclass;
		this.subclassId = superclass.nextSubclassId();
	}

	@Override
	int nextSubclassId() {
		return getSuperclass().nextSubclassId();
	}

	@Override
	public int getSubclassId() {
		return subclassId;
	}

	@Override
	public String getNaturalIdCacheRegionName() {
		return getSuperclass().getNaturalIdCacheRegionName();
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return getRootClass().getCacheConcurrencyStrategy();
	}

	@Override
	public RootClass getRootClass() {
		return getSuperclass().getRootClass();
	}

	@Override
	public PersistentClass getSuperclass() {
		return superclass;
	}

	@Override
	public Property getIdentifierProperty() {
		return getSuperclass().getIdentifierProperty();
	}

	@Override
	public Property getDeclaredIdentifierProperty() {
		return null;
	}

	@Override
	public KeyValue getIdentifier() {
		return getSuperclass().getIdentifier();
	}

	@Override
	public boolean hasIdentifierProperty() {
		return getSuperclass().hasIdentifierProperty();
	}

	@Override
	public Value getDiscriminator() {
		return getSuperclass().getDiscriminator();
	}

	@Override
	public boolean isMutable() {
		return getSuperclass().isMutable();
	}

	@Override
	public boolean isInherited() {
		return true;
	}

	@Override
	public boolean isPolymorphic() {
		return true;
	}

	@Override
	public void addProperty(Property property) {
		super.addProperty( property );
		getSuperclass().addSubclassProperty( property );
	}

	@Override
	public void addMappedSuperclassProperty(Property property) {
		super.addMappedSuperclassProperty( property );
		getSuperclass().addSubclassProperty( property );
	}

	@Override
	public void addJoin(Join j) {
		super.addJoin(j);
		getSuperclass().addSubclassJoin(j);
	}

	@Override
	public List<Property> getPropertyClosure() {
		return new JoinedList<>( getSuperclass().getPropertyClosure(), getProperties() );
	}

	@Override
	public List<Table> getTableClosure() {
		return new JoinedList<>(
				getSuperclass().getTableClosure(),
				List.of( getTable() )
		);
	}

	@Override
	public List<KeyValue> getKeyClosure() {
		return new JoinedList<>(
				getSuperclass().getKeyClosure(),
				List.of( getKey() )
		);
	}

	@Override
	protected void addSubclassProperty(Property p) {
		super.addSubclassProperty(p);
		getSuperclass().addSubclassProperty(p);
	}

	@Override
	protected void addSubclassJoin(Join j) {
		super.addSubclassJoin(j);
		getSuperclass().addSubclassJoin(j);
	}

	@Override
	protected void addSubclassTable(Table table) {
		super.addSubclassTable(table);
		getSuperclass().addSubclassTable(table);
	}

	@Override
	public boolean isVersioned() {
		return getSuperclass().isVersioned();
	}

	@Override
	public Property getVersion() {
		return getSuperclass().getVersion();
	}

	@Override
	public Property getDeclaredVersion() {
		return null;
	}

	@Override
	public boolean hasEmbeddedIdentifier() {
		return getSuperclass().hasEmbeddedIdentifier();
	}

	@Override
	public Table getRootTable() {
		return getSuperclass().getRootTable();
	}

	@Override
	public KeyValue getKey() {
		return getSuperclass().getIdentifier();
	}

	@Override
	public boolean isConcreteProxy() {
		return getRootClass().isConcreteProxy();
	}

	public void setSuperclass(PersistentClass superclass) {
		this.superclass = superclass;
	}

	@Override
	public String getWhere() {
		return getSuperclass().getWhere();
	}

	@Override
	public boolean isJoinedSubclass() {
		return getTable() != getRootTable();
	}

	public void createForeignKey() {
		if ( isJoinedSubclass() ) {
			getKey().createForeignKeyOfEntity( getSuperclass().getEntityName() );
		}
	}


	@Override
	public int getJoinClosureSpan() {
		return getSuperclass().getJoinClosureSpan() + super.getJoinClosureSpan();
	}

	@Override
	public int getPropertyClosureSpan() {
		return getSuperclass().getPropertyClosureSpan() + super.getPropertyClosureSpan();
	}

	@Override
	public List<Join> getJoinClosure() {
		return new JoinedList<>( getSuperclass().getJoinClosure(), super.getJoinClosure() );
	}

	@Override
	public boolean isClassOrSuperclassJoin(Join join) {
		return super.isClassOrSuperclassJoin( join )
			|| getSuperclass().isClassOrSuperclassJoin( join );
	}

	@Override
	public boolean isClassOrSuperclassTable(Table table) {
		return super.isClassOrSuperclassTable( table )
			|| getSuperclass().isClassOrSuperclassTable( table );
	}

	@Override
	public Table getTable() {
		return getSuperclass().getTable();
	}

	@Override
	public boolean isForceDiscriminator() {
		return getSuperclass().isForceDiscriminator();
	}

	@Override
	public boolean isDiscriminatorInsertable() {
		return getSuperclass().isDiscriminatorInsertable();
	}

	@Override
	public java.util.Set<String> getSynchronizedTables() {
		final HashSet<String> result = new HashSet<>();
		result.addAll( synchronizedTables );
		result.addAll( getSuperclass().getSynchronizedTables() );
		return result;
	}

	@Override
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}

	@Override
	public java.util.List<FilterConfiguration> getFilters() {
		final ArrayList<FilterConfiguration> filters = new ArrayList<>( super.getFilters() );
		filters.addAll( getSuperclass().getFilters() );
		return filters;
	}

	@Override
	public boolean hasSubselectLoadableCollections() {
		return super.hasSubselectLoadableCollections()
			|| getSuperclass().hasSubselectLoadableCollections();
	}

	@Override
	public Component getIdentifierMapper() {
		return superclass.getIdentifierMapper();
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return superclass.getOptimisticLockStyle();
	}
}
