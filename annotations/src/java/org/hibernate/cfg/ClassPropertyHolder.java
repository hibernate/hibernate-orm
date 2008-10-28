//$Id$
package org.hibernate.cfg;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.JoinTable;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
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

	public ClassPropertyHolder(
			PersistentClass persistentClass, XClass clazzToProcess, Map<String, Join> joins, ExtendedMappings mappings
	) {
		super( persistentClass.getEntityName(), null, clazzToProcess, mappings );
		this.persistentClass = persistentClass;
		this.joins = joins;
	}

	public ClassPropertyHolder(
			PersistentClass persistentClass, XClass clazzToProcess, EntityBinder entityBinder,
			ExtendedMappings mappings
	) {
		this( persistentClass, clazzToProcess, entityBinder.getSecondaryTables(), mappings );
		this.entityBinder = entityBinder;
	}

	public String getEntityName() {
		return persistentClass.getEntityName();
	}

	public void addProperty(Property prop, Ejb3Column[] columns) {
		//Ejb3Column.checkPropertyConsistency( ); //already called earlier
		if ( columns[0].isSecondary() ) {
			//TODO move the getJoin() code here?
			columns[0].getJoin().addProperty( prop );
		}
		else {
			addProperty( prop );
		}
	}

	public Join addJoin(JoinTable joinTableAnn, boolean noDelayInPkColumnCreation) {
		Join join = entityBinder.addJoin( joinTableAnn, this, noDelayInPkColumnCreation );
		this.joins = entityBinder.getSecondaryTables();
		return join;
	}

	public void addProperty(Property prop) {
		if ( prop.getValue() instanceof Component ) {
			//TODO handle quote and non quote table comparison
			String tableName = prop.getValue().getTable().getName();
			if ( getJoinsPerRealTableName().containsKey( tableName ) ) {
				getJoinsPerRealTableName().get( tableName ).addProperty( prop );
			}
			else {
				persistentClass.addProperty( prop );
			}
		}
		else {
			persistentClass.addProperty( prop );
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
}
