/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

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
import org.hibernate.type.BasicType;
import org.hibernate.type.MappingContext;
import org.hibernate.type.Type;

import java.util.List;

class ExportableColumnHelper {

	static Column column(Database database, Table table, String segmentColumnName, BasicType<?> type, String typeName) {
		final var column = new Column( segmentColumnName );
		column.setSqlType( typeName );
		column.setValue( new Value() {
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
		} );
		return column;
	}
}
