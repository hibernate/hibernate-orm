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
package org.hibernate.cfg;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.JoinTable;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

/**
 * @author Emmanuel Bernard
 */
public class ClassPropertyHolder extends AbstractPropertyHolder {
	private PersistentClass persistentClass;
	private Map<String, Join> joins;
	private transient Map<String, Join> joinsPerRealTableName;
	private EntityBinder entityBinder;
	private final Map<XClass, InheritanceState> inheritanceStatePerClass;

	public ClassPropertyHolder(
			PersistentClass persistentClass,
			XClass clazzToProcess,
			Map<String, Join> joins,
			Mappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		super( persistentClass.getEntityName(), null, clazzToProcess, mappings );
		this.persistentClass = persistentClass;
		this.joins = joins;
		this.inheritanceStatePerClass = inheritanceStatePerClass;
	}

	public ClassPropertyHolder(
			PersistentClass persistentClass,
			XClass clazzToProcess,
			EntityBinder entityBinder,
			Mappings mappings,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		this( persistentClass, clazzToProcess, entityBinder.getSecondaryTables(), mappings, inheritanceStatePerClass );
		this.entityBinder = entityBinder;
	}

	public String getEntityName() {
		return persistentClass.getEntityName();
	}

	public void addProperty(Property prop, Ejb3Column[] columns, XClass declaringClass) {
		//Ejb3Column.checkPropertyConsistency( ); //already called earlier
		if ( columns != null && columns[0].isSecondary() ) {
			//TODO move the getJoin() code here?
			final Join join = columns[0].getJoin();
			addPropertyToJoin( prop, declaringClass, join );
		}
		else {
			addProperty( prop, declaringClass );
		}
	}

	public void addProperty(Property prop, XClass declaringClass) {
		if ( prop.getValue() instanceof Component ) {
			//TODO handle quote and non quote table comparison
			String tableName = prop.getValue().getTable().getName();
			if ( getJoinsPerRealTableName().containsKey( tableName ) ) {
				final Join join = getJoinsPerRealTableName().get( tableName );
				addPropertyToJoin( prop, declaringClass, join );
			}
			else {
				addPropertyToPersistentClass( prop, declaringClass );
			}
		}
		else {
			addPropertyToPersistentClass( prop, declaringClass );
		}
	}

	public Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation) {
		Join join = entityBinder.addJoin( joinTableAnn, this, noDelayInPkColumnCreation );
		this.joins = entityBinder.getSecondaryTables();
		return join;
	}

	private void addPropertyToPersistentClass(Property prop, XClass declaringClass) {
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				persistentClass.addMappedsuperclassProperty(prop);
				addPropertyToMappedSuperclass( prop, declaringClass );
			}
			else {
				persistentClass.addProperty( prop );
			}
		}
		else {
			persistentClass.addProperty( prop );
		}
	}

	private void addPropertyToMappedSuperclass(Property prop, XClass declaringClass) {
		final Mappings mappings = getMappings();
		final Class type = mappings.getReflectionManager().toClass( declaringClass );
		MappedSuperclass superclass = mappings.getMappedSuperclass( type );
		superclass.addDeclaredProperty( prop );
	}

	private void addPropertyToJoin(Property prop, XClass declaringClass, Join join) {
		if ( declaringClass != null ) {
			final InheritanceState inheritanceState = inheritanceStatePerClass.get( declaringClass );
			if ( inheritanceState == null ) {
				throw new AssertionFailure(
						"Declaring class is not found in the inheritance state hierarchy: " + declaringClass
				);
			}
			if ( inheritanceState.isEmbeddableSuperclass() ) {
				join.addMappedsuperclassProperty(prop);
				addPropertyToMappedSuperclass( prop, declaringClass );
			}
			else {
				join.addProperty( prop );
			}
		}
		else {
			join.addProperty( prop );
		}
	}

	/**
	 * Needed for proper compliance with naming strategy, the property table
	 * can be overriden if the properties are part of secondary tables
	 */
	private Map<String, Join> getJoinsPerRealTableName() {
		if ( joinsPerRealTableName == null ) {
			joinsPerRealTableName = new HashMap<String, Join>( joins.size() );
			for (Join join : joins.values()) {
				joinsPerRealTableName.put( join.getTable().getName(), join );
			}
		}
		return joinsPerRealTableName;
	}

	public String getClassName() {
		return persistentClass.getClassName();
	}

	public String getEntityOwnerClassName() {
		return getClassName();
	}

	public Table getTable() {
		return persistentClass.getTable();
	}

	public boolean isComponent() {
		return false;
	}

	public boolean isEntity() {
		return true;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public KeyValue getIdentifier() {
		return persistentClass.getIdentifier();
	}

	public boolean isOrWithinEmbeddedId() {
		return false;
	}
}
