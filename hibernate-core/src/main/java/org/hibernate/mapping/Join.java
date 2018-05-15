/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.boot.model.domain.MappedJoin;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.relational.MappedPrimaryKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.sql.Alias;

/**
 * @author Gavin King
 */
public class Join implements AttributeContainer, Serializable, MappedJoin {

	private static final Alias PK_ALIAS = new Alias(15, "PK");

	private TreeMap<String, PersistentAttributeMapping> declaredAttributeMappings;
	private TreeMap<String, PersistentAttributeMapping> attributeMappings;

	private MappedTable table;
	private KeyValue key;
	private PersistentClass persistentClass;
	private boolean sequentialSelect;
	private boolean inverse;
	private boolean optional;

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

	@Override
	public void addProperty(Property prop) {
		if(attributeMappings == null){
			attributeMappings = new TreeMap<>(  );
		}
		if ( declaredAttributeMappings == null ) {
			declaredAttributeMappings = new TreeMap<>(  );
		}
		attributeMappings.putIfAbsent( prop.getName(), prop );
		declaredAttributeMappings.putIfAbsent( prop.getName(), prop );
		prop.setPersistentClass( getPersistentClass() );
	}

	public void addMappedsuperclassProperty(Property prop) {
		if ( declaredAttributeMappings == null ) {
			declaredAttributeMappings = new TreeMap<>(  );
		}
		attributeMappings.put( prop.getName(), prop );
		prop.setPersistentClass( getPersistentClass() );
	}

	/**
	 * @deprecated since 6.0, use {@link #getDeclaredPersistentAttributes()}.
	 */
	@Deprecated
	public Iterator getDeclaredPropertyIterator() {
		return getDeclaredPersistentAttributes().iterator();
	}

	/**
	 * @deprecated since 6.0, use {@link #containsPersistentAttributeMapping(PersistentAttributeMapping)}.
	 */
	@Deprecated
	public boolean containsProperty(Property prop) {
		return containsPersistentAttributeMapping( prop );
	}

	public boolean containsPersistentAttributeMapping(PersistentAttributeMapping attributeMapping){
		return attributeMappings.containsKey( attributeMapping.getName() );
	}

	/**
	 * @deprecated since 6.0, use {@link #getPersistentAttributes()}.
	 */
	@Deprecated
	public Iterator getPropertyIterator() {
		return getPersistentAttributes().iterator();
	}

	@Override
	public java.util.List<PersistentAttributeMapping> getPersistentAttributes(){
		return attributeMappings == null
				? Collections.emptyList()
				: new ArrayList<>( attributeMappings.values() );
	}

	@Override
	public java.util.List<PersistentAttributeMapping> getDeclaredPersistentAttributes(){
		return declaredAttributeMappings == null
				? Collections.emptyList()
				: new ArrayList<>( declaredAttributeMappings.values() );
	}

	/**
	 * @deprecated since 6.0, use {@link #getPersistentAttributes()}.{@link List#size() size()}.
	 */
	@Deprecated
	public int getPropertySpan() {
		return getPersistentAttributes().size();
	}

	/**
	 * @deprecated since 6.0, use {@link #getMappedTable()}.
	 */
	@Deprecated
	public Table getTable() {
		return (Table) getMappedTable();
	}

	public void setTable(MappedTable table) {
		this.table = table;
	}

	@Override
	public MappedTable getMappedTable() {
		return table;
	}

	public KeyValue getKey() {
		return key;
	}

	public void setKey(KeyValue key) {
		this.key = key;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	private ForeignKey joinMapping;

	public void createForeignKey() {
		joinMapping = getKey().createForeignKeyOfEntity( persistentClass.getEntityName() );
	}

	@Override
	public ForeignKey getJoinMapping() {
		return joinMapping;
	}

	public void createPrimaryKey() {
		//Primary key constraint
		MappedPrimaryKey pk = new PrimaryKey( table );
		pk.setName( PK_ALIAS.toAliasString( table.getName() ) );
		table.setPrimaryKey(pk);

		pk.addColumns( getKey().getMappedColumns() );
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

	public boolean isSequentialSelect() {
		return sequentialSelect;
	}

	public void setSequentialSelect(boolean deferred) {
		this.sequentialSelect = deferred;
	}

	@Override
	public boolean isInverse() {
		return inverse;
	}

	public void setInverse(boolean leftJoin) {
		this.inverse = leftJoin;
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + table.toString() + ')';
	}

	public boolean isLazy() {
		Iterator iter = getPropertyIterator();
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			if ( !prop.isLazy() ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isOptional() {
		return optional;
	}

	public void setOptional(boolean nullable) {
		this.optional = nullable;
	}

	public ExecuteUpdateResultCheckStyle getUpdateResultCheckStyle() {
		String sql = getCustomSQLUpdate();
		boolean callable = sql != null && isCustomUpdateCallable();
		ExecuteUpdateResultCheckStyle checkStyle = getCustomSQLUpdateCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: getCustomSQLUpdateCheckStyle();
		return checkStyle;
	}
}
