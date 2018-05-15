/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.model.domain.EntityJavaTypeMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.internal.EntityJavaTypeMappingImpl;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.collections.SingletonIterator;

/**
 * A sublass in a table-per-class-hierarchy mapping
 * @author Gavin King
 */
public abstract class Subclass extends PersistentClass {
	private IdentifiableTypeMapping superclass;
	private Class classPersisterClass;
	private final int subclassId;
	
	public Subclass(
			IdentifiableTypeMapping superclass,
			MetadataBuildingContext metadataBuildingContext) {
		super( metadataBuildingContext, superclass.getEntityMappingHierarchy() );
		setJavaTypeMapping( resolveJavaTypeMapping(
				superclass,
				metadataBuildingContext
		) );
		this.superclass = superclass;
		this.subclassId = ( (IdentifiableTypeMappingImplementor) superclass ).nextSubclassId();
	}

	private EntityJavaTypeMapping resolveJavaTypeMapping(
			IdentifiableTypeMapping superEntity,
			MetadataBuildingContext metadataBuildingContext) {
		return new EntityJavaTypeMappingImpl(
				metadataBuildingContext,
				this,
				superEntity == null ? null : superEntity.getJavaTypeMapping()
		);
	}

	@Override
	public int nextSubclassId() {
		return getSuperclass().nextSubclassId();
	}
	
	public int getSubclassId() {
		return subclassId;
	}
	
	@Override
	public String getNaturalIdCacheRegionName() {
		return getSuperclass().getNaturalIdCacheRegionName();
	}

	public String getCacheConcurrencyStrategy() {
		return getRootClass().getCacheConcurrencyStrategy();
	}

	/**
	 * @deprecated since 6.0, use {@link EntityMappingHierarchy#getRootType()}.
	 */
	@Deprecated
	public RootClass getRootClass() {
		return getSuperclass().getRootClass();
	}

	/**
	 * @deprecated since 6.0 use {@link #getSuperManagedTypeMapping()}.
	 */
	@Deprecated
	public PersistentClass getSuperclass() {
		return (PersistentClass) superclass;
	}

	@Override
	public Property getIdentifierProperty() {
		return getSuperclass().getIdentifierProperty();
	}

	@Override
	public Property getDeclaredIdentifierProperty() {
		return null;
	}

	public PersistentAttributeMapping getDeclaredIdentifierAttributeMapping() {
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

	public Value getDiscriminator() {
		return getSuperclass().getDiscriminator();
	}

	public boolean isMutable() {
		return getSuperclass().isMutable();
	}

	public boolean isInherited() {
		return true;
	}

	public boolean isPolymorphic() {
		return true;
	}

	public void addProperty(Property p) {
		super.addProperty(p);
		getSuperclass().addSubclassProperty(p);
	}

	public void addMappedsuperclassProperty(Property p) {
		super.addMappedsuperclassProperty( p );
		getSuperclass().addSubclassProperty(p);
	}

	public void addJoin(Join j) {
		super.addJoin(j);
		getSuperclass().addSubclassJoin(j);
	}

	public Iterator getPropertyClosureIterator() {
		return new JoinedIterator(
				getSuperclass().getPropertyClosureIterator(),
				getPropertyIterator()
			);
	}

	public Iterator getTableClosureIterator() {
		return new JoinedIterator(
				getSuperclass().getTableClosureIterator(),
				new SingletonIterator( getTable() )
			);
	}

	public Iterator getKeyClosureIterator() {
		return new JoinedIterator(
				getSuperclass().getKeyClosureIterator(),
				new SingletonIterator( getKey() )
			);
	}

	protected void addSubclassProperty(Property p) {
		super.addSubclassProperty(p);
		getSuperclass().addSubclassProperty(p);
	}

	protected void addSubclassJoin(Join j) {
		super.addSubclassJoin(j);
		getSuperclass().addSubclassJoin(j);
	}

	protected void addSubclassTable(MappedTable table) {
		super.addSubclassTable(table);
		getSuperclass().addSubclassTable(table);
	}

	@Override
	public boolean isVersioned() {
		return getSuperclass().isVersioned();
	}

	/**
	 * @deprecated since 6.0, use {@link #getVersionAttributeMapping()}.
	 */
	@Deprecated
	@Override
	public Property getVersion() {
		return getSuperclass().getVersion();
	}

	@Override
	public Property getDeclaredVersion() {
		return null;
	}

	@Override
	public PersistentAttributeMapping getDeclaredVersionAttributeMapping() {
		return null;
	}

	@Override
	public boolean hasEmbeddedIdentifier() {
		return getSuperclass().hasEmbeddedIdentifier();
	}

	public Class getRuntimeEntityDescriptorClass() {
		if (classPersisterClass==null) {
			return getSuperclass().getRuntimeEntityDescriptorClass();
		}
		else {
			return classPersisterClass;
		}
	}

	public Table getRootTable() {
		return getSuperclass().getRootTable();
	}

	public KeyValue getKey() {
		return getSuperclass().getIdentifier();
	}

	public boolean isExplicitPolymorphism() {
		return getSuperclass().isExplicitPolymorphism();
	}

	public void setSuperclass(PersistentClass superclass) {
		this.superclass = superclass;
	}

	public String getWhere() {
		return getSuperclass().getWhere();
	}

	public boolean isJoinedSubclass() {
		return getTable()!=getRootTable();
	}

	public void createForeignKey() {
		if ( !isJoinedSubclass() ) {
			throw new AssertionFailure( "not a joined-subclass" );
		}
		getKey().createForeignKeyOfEntity( getSuperclass().getEntityName() );
	}

	public void setEntityPersisterClass(Class classPersisterClass) {
		this.classPersisterClass = classPersisterClass;
	}

	public int getJoinClosureSpan() {
		return getSuperclass().getJoinClosureSpan() + super.getJoinClosureSpan();
	}

	public int getPropertyClosureSpan() {
		return getSuperclass().getPropertyClosureSpan() + super.getPropertyClosureSpan();
	}

	public Iterator getJoinClosureIterator() {
		return new JoinedIterator(
			getSuperclass().getJoinClosureIterator(),
			super.getJoinClosureIterator()
		);
	}

	public boolean isClassOrSuperclassJoin(Join join) {
		return super.isClassOrSuperclassJoin(join) || getSuperclass().isClassOrSuperclassJoin(join);
	}

	public boolean isClassOrSuperclassTable(Table table) {
		return super.isClassOrSuperclassTable(table) || getSuperclass().isClassOrSuperclassTable(table);
	}

	public Table getTable() {
		return getSuperclass().getTable();
	}

	public boolean isForceDiscriminator() {
		return getSuperclass().isForceDiscriminator();
	}

	public boolean isDiscriminatorInsertable() {
		return getSuperclass().isDiscriminatorInsertable();
	}

	public java.util.Set getSynchronizedTables() {
		HashSet result = new HashSet();
		result.addAll(synchronizedTables);
		result.addAll( getSuperclass().getSynchronizedTables() );
		return result;
	}

	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}

	public java.util.List getFilters() {
		java.util.List filters = new ArrayList(super.getFilters());
		filters.addAll(getSuperclass().getFilters());
		return filters;
	}

	public boolean hasSubselectLoadableCollections() {
		return super.hasSubselectLoadableCollections() || 
			getSuperclass().hasSubselectLoadableCollections();
	}

	/**
	 * @deprecated since 6.0, use {@link EntityMappingHierarchy#getIdentifierEmbeddedValueMapping()}.
	 */
	@Override
	@Deprecated
	public Component getIdentifierMapper() {
		return (Component) getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping();
	}

	/**
	 * @deprecated since 6.0, use {@link EntityMappingHierarchy#getOptimisticLockStyle()}.
	 */
	@Override
	@Deprecated
	public OptimisticLockStyle getOptimisticLockStyle() {
		return getEntityMappingHierarchy().getOptimisticLockStyle();
	}
}
