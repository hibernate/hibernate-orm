/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.SingletonIterator;

/**
 * The root class of an inheritance hierarchy
 * @author Gavin King
 */
public class RootClass extends PersistentClass implements TableOwner {

	public static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
	public static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";

	private Property identifierProperty; //may be final
	private KeyValue identifier; //may be final
	private Property version; //may be final
	private boolean polymorphic;
	private String cacheConcurrencyStrategy;
	private String cacheRegionName;
	private boolean lazyPropertiesCacheable = true;
	private Value discriminator; //may be final
	private boolean mutable = true;
	private boolean embeddedIdentifier = false; // may be final
	private boolean explicitPolymorphism;
	private Class entityPersisterClass;
	private boolean forceDiscriminator = false;
	private String where;
	private Table table;
	private boolean discriminatorInsertable = true;
	private int nextSubclassId = 0;
	private Property declaredIdentifierProperty;
	private Property declaredVersion;

	int nextSubclassId() {
		return ++nextSubclassId;
	}

	public int getSubclassId() {
		return 0;
	}
	
	public void setTable(Table table) {
		this.table=table;
	}
	public Table getTable() {
		return table;
	}

	public Property getIdentifierProperty() {
		return identifierProperty;
	}

	public Property getDeclaredIdentifierProperty() {
		return declaredIdentifierProperty;
	}

	public void setDeclaredIdentifierProperty(Property declaredIdentifierProperty) {
		this.declaredIdentifierProperty = declaredIdentifierProperty;
	}

	public KeyValue getIdentifier() {
		return identifier;
	}
	public boolean hasIdentifierProperty() {
		return identifierProperty!=null;
	}

	public Value getDiscriminator() {
		return discriminator;
	}

	public boolean isInherited() {
		return false;
	}
	public boolean isPolymorphic() {
		return polymorphic;
	}

	public void setPolymorphic(boolean polymorphic) {
		this.polymorphic = polymorphic;
	}

	public RootClass getRootClass() {
		return this;
	}

	public Iterator getPropertyClosureIterator() {
		return getPropertyIterator();
	}
	public Iterator getTableClosureIterator() {
		return new SingletonIterator( getTable() );
	}
	public Iterator getKeyClosureIterator() {
		return new SingletonIterator( getKey() );
	}

	public void addSubclass(Subclass subclass) throws MappingException {
		super.addSubclass(subclass);
		setPolymorphic(true);
	}

	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public Property getVersion() {
		return version;
	}

	public Property getDeclaredVersion() {
		return declaredVersion;
	}

	public void setDeclaredVersion(Property declaredVersion) {
		this.declaredVersion = declaredVersion;
	}

	public void setVersion(Property version) {
		this.version = version;
	}
	public boolean isVersioned() {
		return version!=null;
	}

	public boolean isMutable() {
		return mutable;
	}
	public boolean hasEmbeddedIdentifier() {
		return embeddedIdentifier;
	}

	public Class getEntityPersisterClass() {
		return entityPersisterClass;
	}

	public Table getRootTable() {
		return getTable();
	}

	public void setEntityPersisterClass(Class persister) {
		this.entityPersisterClass = persister;
	}

	public PersistentClass getSuperclass() {
		return null;
	}

	public KeyValue getKey() {
		return getIdentifier();
	}

	public void setDiscriminator(Value discriminator) {
		this.discriminator = discriminator;
	}

	public void setEmbeddedIdentifier(boolean embeddedIdentifier) {
		this.embeddedIdentifier = embeddedIdentifier;
	}

	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
		this.explicitPolymorphism = explicitPolymorphism;
	}

	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
	}

	public void setIdentifierProperty(Property identifierProperty) {
		this.identifierProperty = identifierProperty;
		identifierProperty.setPersistentClass(this);

	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public boolean isDiscriminatorInsertable() {
		return discriminatorInsertable;
	}
	
	public void setDiscriminatorInsertable(boolean insertable) {
		this.discriminatorInsertable = insertable;
	}

	public boolean isForceDiscriminator() {
		return forceDiscriminator;
	}

	public void setForceDiscriminator(boolean forceDiscriminator) {
		this.forceDiscriminator = forceDiscriminator;
	}

	public String getWhere() {
		return where;
	}

	public void setWhere(String string) {
		where = string;
	}

	public void validate(Mapping mapping) throws MappingException {
		super.validate(mapping);
		if ( !getIdentifier().isValid(mapping) ) {
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
				Class idClass = id.getComponentClass();
				if ( idClass != null && !ReflectHelper.overridesEquals( idClass ) ) {
					LoggerFactory.getLogger( RootClass.class )
						.warn( "composite-id class does not override equals(): "
							+ id.getComponentClass().getName() );
				}
				if ( !ReflectHelper.overridesHashCode( idClass ) ) {
					LoggerFactory.getLogger( RootClass.class )
						.warn( "composite-id class does not override hashCode(): "
							+ id.getComponentClass().getName() );
				}
				if ( !Serializable.class.isAssignableFrom( idClass ) ) {
					throw new MappingException( "composite-id class must implement Serializable: "
						+ id.getComponentClass().getName() );
				}
			}
		}
	}
	
	public String getCacheConcurrencyStrategy() {
		return cacheConcurrencyStrategy;
	}

	public void setCacheConcurrencyStrategy(String cacheConcurrencyStrategy) {
		this.cacheConcurrencyStrategy = cacheConcurrencyStrategy;
	}

	public String getCacheRegionName() {
		return cacheRegionName==null ? getEntityName() : cacheRegionName;
	}
	public void setCacheRegionName(String cacheRegionName) {
		this.cacheRegionName = cacheRegionName;
	}

	public boolean isLazyPropertiesCacheable() {
		return lazyPropertiesCacheable;
	}

	public void setLazyPropertiesCacheable(boolean lazyPropertiesCacheable) {
		this.lazyPropertiesCacheable = lazyPropertiesCacheable;
	}
	
	public boolean isJoinedSubclass() {
		return false;
	}

	public java.util.Set getSynchronizedTables() {
		return synchronizedTables;
	}
	
	public Set getIdentityTables() {
		Set tables = new HashSet();
		Iterator iter = getSubclassClosureIterator();
		while ( iter.hasNext() ) {
			PersistentClass clazz = (PersistentClass) iter.next();
			if ( clazz.isAbstract() == null || !clazz.isAbstract().booleanValue() ) tables.add( clazz.getIdentityTable() );
		}
		return tables;
	}
	
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}
	
	public int getOptimisticLockMode() {
		return optimisticLockMode;
	}

}
