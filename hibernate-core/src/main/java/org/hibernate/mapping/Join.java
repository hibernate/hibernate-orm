/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.Alias;

import static org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle.expectationConstructor;

/**
 * A mapping model object representing some sort of auxiliary table, for
 * example, an {@linkplain jakarta.persistence.JoinTable association table},
 * a {@linkplain jakarta.persistence.SecondaryTable secondary table}, or a
 * table belonging to a {@linkplain jakarta.persistence.InheritanceType#JOINED
 * joined subclass}.
 *
 * @author Gavin King
 */
public class Join implements AttributeContainer, Serializable {

	private static final Alias PK_ALIAS = new Alias(15, "PK");

	private final ArrayList<Property> properties = new ArrayList<>();
	private final ArrayList<Property> declaredProperties = new ArrayList<>();
	private Table table;
	private KeyValue key;
	private PersistentClass persistentClass;
	private boolean inverse;
	private boolean optional;
	private boolean disableForeignKeyCreation;

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

	private Supplier<? extends Expectation> insertExpectation;
	private Supplier<? extends Expectation> updateExpectation;
	private Supplier<? extends Expectation> deleteExpectation;

	@Override
	public void addProperty(Property property) {
		properties.add( property );
		declaredProperties.add( property );
		property.setPersistentClass( persistentClass );
	}

	@Override
	public boolean contains(Property property) {
		return properties.contains( property );
	}

	public void addMappedSuperclassProperty(Property property ) {
		properties.add( property );
		property.setPersistentClass( persistentClass );
	}

	public List<Property> getDeclaredProperties() {
		return declaredProperties;
	}

	public List<Property> getProperties() {
		return properties;
	}

	public boolean containsProperty(Property property) {
		return properties.contains( property );
	}

	@Deprecated(since = "6.0")
	public Iterator<Property> getPropertyIterator() {
		return properties.iterator();
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
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

	public void disableForeignKeyCreation() {
		disableForeignKeyCreation = true;
	}

	public void createForeignKey() {
		final ForeignKey foreignKey = getKey().createForeignKeyOfEntity( persistentClass.getEntityName() );
		if ( disableForeignKeyCreation ) {
			foreignKey.disableCreation();
		}
	}

	public void createPrimaryKey() {
		//Primary key constraint
		PrimaryKey primaryKey = new PrimaryKey( table );
		primaryKey.setName( PK_ALIAS.toAliasString( table.getName() ) );
		table.setPrimaryKey(primaryKey);

		primaryKey.addColumns( getKey() );
	}

	public int getPropertySpan() {
		return properties.size();
	}

	public void setCustomSQLInsert(String customSQLInsert, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLInsert = customSQLInsert;
		this.customInsertCallable = callable;
		this.insertCheckStyle = checkStyle;
		this.insertExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLInsert() {
		return customSQLInsert;
	}

	public boolean isCustomInsertCallable() {
		return customInsertCallable;
	}

	/**
	 * @deprecated use {@link #getInsertExpectation()}
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	public ExecuteUpdateResultCheckStyle getCustomSQLInsertCheckStyle() {
		return insertCheckStyle;
	}

	public void setCustomSQLUpdate(String customSQLUpdate, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLUpdate = customSQLUpdate;
		this.customUpdateCallable = callable;
		this.updateCheckStyle = checkStyle;
		this.updateExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLUpdate() {
		return customSQLUpdate;
	}

	public boolean isCustomUpdateCallable() {
		return customUpdateCallable;
	}

	/**
	 * @deprecated use {@link #getUpdateExpectation()}
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	public ExecuteUpdateResultCheckStyle getCustomSQLUpdateCheckStyle() {
		return updateCheckStyle;
	}

	public void setCustomSQLDelete(String customSQLDelete, boolean callable, ExecuteUpdateResultCheckStyle checkStyle) {
		this.customSQLDelete = customSQLDelete;
		this.customDeleteCallable = callable;
		this.deleteCheckStyle = checkStyle;
		this.deleteExpectation = expectationConstructor( checkStyle );
	}

	public String getCustomSQLDelete() {
		return customSQLDelete;
	}

	public boolean isCustomDeleteCallable() {
		return customDeleteCallable;
	}

	/**
	 * @deprecated use {@link #getDeleteExpectation()}
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	public ExecuteUpdateResultCheckStyle getCustomSQLDeleteCheckStyle() {
		return deleteCheckStyle;
	}

	public boolean isInverse() {
		return inverse;
	}

	public void setInverse(boolean leftJoin) {
		this.inverse = leftJoin;
	}

	public String toString() {
		return getClass().getSimpleName() + '(' + table.getName() + ')';
	}

	public boolean isLazy() {
		for ( Property property : properties ) {
			if ( !property.isLazy() ) {
				return false;
			}
		}
		return true;
	}

	public boolean isOptional() {
		return optional;
	}

	public void setOptional(boolean nullable) {
		this.optional = nullable;
	}

	public Supplier<? extends Expectation> getInsertExpectation() {
		return insertExpectation;
	}

	public void setInsertExpectation(Supplier<? extends Expectation> insertExpectation) {
		this.insertExpectation = insertExpectation;
	}

	public Supplier<? extends Expectation> getUpdateExpectation() {
		return updateExpectation;
	}

	public void setUpdateExpectation(Supplier<? extends Expectation> updateExpectation) {
		this.updateExpectation = updateExpectation;
	}

	public Supplier<? extends Expectation> getDeleteExpectation() {
		return deleteExpectation;
	}

	public void setDeleteExpectation(Supplier<? extends Expectation> deleteExpectation) {
		this.deleteExpectation = deleteExpectation;
	}
}
