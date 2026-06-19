/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.Alias;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A mapping model object representing some sort of auxiliary table, for
 * example, an {@linkplain jakarta.persistence.JoinTable association table},
 * a {@linkplain jakarta.persistence.SecondaryTable secondary table}, or a
 * table belonging to a {@linkplain jakarta.persistence.InheritanceType#JOINED
 * joined subclass}.
 *
 * @author Gavin King
 */
public class Join implements AttributeContainer, AuxiliaryTableHolder, Serializable {

	private static final Alias PK_ALIAS = new Alias(15, "PK");

	private final ArrayList<Property> properties = new ArrayList<>();
	private final ArrayList<Property> declaredProperties = new ArrayList<>();
	private Table table;
	private Table auxiliaryTable;
	private Map<String, Column> auxiliaryColumns;
	private KeyValue key;
	private PersistentClass persistentClass;
	private boolean inverse;
	private boolean optional;
	private boolean disableForeignKeyCreation;

	private CustomSqlMapping customSqlInsert;
	private CustomSqlMapping customSqlUpdate;
	private CustomSqlMapping customSqlDelete;

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

	@Override
	public Property getProperty(String propertyName) throws MappingException {
		throw new UnsupportedOperationException(); //TODO
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

	@Override
	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	@Override
	public Table getAuxiliaryTable() {
		return auxiliaryTable;
	}

	@Override
	public void setAuxiliaryTable(Table auxiliaryTable) {
		this.auxiliaryTable = auxiliaryTable;
	}

	@Override
	public Column getAuxiliaryColumn(String name) {
		return auxiliaryColumns == null ? null : auxiliaryColumns.get( name );
	}

	@Override
	public void addAuxiliaryColumn(String name, Column column) {
		if ( auxiliaryColumns == null ) {
			auxiliaryColumns = new HashMap<>();
		}
		auxiliaryColumns.put( name, column );
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

	/**
	 * @deprecated Hidden key creation is being replaced by explicit boot-time
	 * key materialization products.
	 */
	@Deprecated(since = "9.0", forRemoval = true)
	public void createForeignKey() {
		final var foreignKey = getKey().createForeignKeyOfEntity( persistentClass.getEntityName() );
		if ( foreignKey != null && disableForeignKeyCreation ) {
			foreignKey.disableCreation();
		}
	}

	/**
	 * @deprecated Use explicit boot-time table-key materialization instead.
	 */
	@Deprecated(since = "9.0", forRemoval = true)
	public void createPrimaryKey() {
		//Primary key constraint
		final var primaryKey = new PrimaryKey( table );
		primaryKey.setName( PK_ALIAS.toAliasString( table.getName() ) );
		table.setPrimaryKey(primaryKey);
		primaryKey.addColumns( getKey() );
	}

	public int getPropertySpan() {
		return properties.size();
	}

	public void setCustomSQLInsert(String customSQLInsert, boolean callable) {
		setCustomSqlInsert( new CustomSqlMapping( customSQLInsert, callable, null ) );
	}

	public void setCustomSqlInsert(CustomSqlMapping customSqlInsert) {
		this.customSqlInsert = customSqlInsert;
	}

	public CustomSqlMapping getCustomSqlInsert() {
		return customSqlInsert;
	}

	public String getCustomSQLInsert() {
		return customSqlInsert == null ? null : customSqlInsert.sql();
	}

	public boolean isCustomInsertCallable() {
		return customSqlInsert != null && customSqlInsert.callable();
	}

	public void setCustomSQLUpdate(String customSQLUpdate, boolean callable) {
		setCustomSqlUpdate( new CustomSqlMapping( customSQLUpdate, callable, null ) );
	}

	public void setCustomSqlUpdate(CustomSqlMapping customSqlUpdate) {
		this.customSqlUpdate = customSqlUpdate;
	}

	public CustomSqlMapping getCustomSqlUpdate() {
		return customSqlUpdate;
	}

	public String getCustomSQLUpdate() {
		return customSqlUpdate == null ? null : customSqlUpdate.sql();
	}

	public boolean isCustomUpdateCallable() {
		return customSqlUpdate != null && customSqlUpdate.callable();
	}

	public void setCustomSQLDelete(String customSQLDelete, boolean callable) {
		setCustomSqlDelete( new CustomSqlMapping( customSQLDelete, callable, null ) );
	}

	public void setCustomSqlDelete(CustomSqlMapping customSqlDelete) {
		this.customSqlDelete = customSqlDelete;
	}

	public CustomSqlMapping getCustomSqlDelete() {
		return customSqlDelete;
	}

	public String getCustomSQLDelete() {
		return customSqlDelete == null ? null : customSqlDelete.sql();
	}

	public boolean isCustomDeleteCallable() {
		return customSqlDelete != null && customSqlDelete.callable();
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
		return customSqlInsert == null ? null : customSqlInsert.expectation();
	}

	public void setInsertExpectation(Supplier<? extends Expectation> insertExpectation) {
		this.customSqlInsert = new CustomSqlMapping(
				getCustomSQLInsert(),
				isCustomInsertCallable(),
				insertExpectation
		);
	}

	public Supplier<? extends Expectation> getUpdateExpectation() {
		return customSqlUpdate == null ? null : customSqlUpdate.expectation();
	}

	public void setUpdateExpectation(Supplier<? extends Expectation> updateExpectation) {
		this.customSqlUpdate = new CustomSqlMapping(
				getCustomSQLUpdate(),
				isCustomUpdateCallable(),
				updateExpectation
		);
	}

	public Supplier<? extends Expectation> getDeleteExpectation() {
		return customSqlDelete == null ? null : customSqlDelete.expectation();
	}

	public void setDeleteExpectation(Supplier<? extends Expectation> deleteExpectation) {
		this.customSqlDelete = new CustomSqlMapping(
				getCustomSQLDelete(),
				isCustomDeleteCallable(),
				deleteExpectation
		);
	}
}
