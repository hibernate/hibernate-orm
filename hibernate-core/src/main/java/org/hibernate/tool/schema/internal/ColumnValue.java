/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.mapping.ValueVisitor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.MappingContext;
import org.hibernate.type.Type;

public class ColumnValue implements Value {

	private final Database database;
	private final Table table;
	private final Column column;
	private final Type type;

	public ColumnValue(Database database, Table table, Column column, Type type) {
		this.database = database;
		this.table = table;
		this.column = column;
		this.type = type;
	}

	@Override
	public Value copy() {
		return this;
	}

	@Override
	public int getColumnSpan() {
		return 1;
	}

	@Override
	public List<Selectable> getSelectables() {
		return List.of( column );
	}

	@Override
	public List<Column> getColumns() {
		return List.of( column );
	}

	@Override
	public boolean hasColumns() {
		return true;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public FetchMode getFetchMode() {
		return null;
	}

	@Override
	public Table getTable() {
		return table;
	}

	@Override
	public boolean hasFormula() {
		return false;
	}

	@Override
	public boolean isAlternateUniqueKey() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean[] getColumnInsertability() {
		return ArrayHelper.TRUE;
	}

	@Override
	public boolean hasAnyInsertableColumns() {
		return true;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		return ArrayHelper.TRUE;
	}

	@Override
	public boolean hasAnyUpdatableColumns() {
		return true;
	}

	@Override
	public void createForeignKey() {
	}

	@Override
	public void createUniqueKey(MetadataBuildingContext context) {
	}

	@Override
	public boolean isSimpleValue() {
		return true;
	}

	@Override
	public boolean isValid(MappingContext mappingContext) throws MappingException {
		return false;
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) {
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return null;
	}

	@Override
	public boolean isSame(Value value) {
		return this == value;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return database.getServiceRegistry();
	}

	@Override
	public boolean isColumnInsertable(int index) {
		return true;
	}

	@Override
	public boolean isColumnUpdateable(int index) {
		return true;
	}

	@Override
	public boolean isPartitionKey() {
		return false;
	}
}
