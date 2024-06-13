/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.mapping.ValueVisitor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class ExportableColumn extends Column {

	public ExportableColumn(Database database, Table table, String name, BasicType<?> type) {
		this(
				database,
				table,
				name,
				type,
				database.getTypeConfiguration()
						.getDdlTypeRegistry()
						.getTypeName( type.getJdbcType().getDdlTypeCode(), database.getDialect() )
		);
	}

	public ExportableColumn(
			Database database,
			Table table,
			String name,
			BasicType<?> type,
			String dbTypeDeclaration) {
		super( name );
		setValue( new ValueImpl( this, table, type, database ) );
		setSqlType( dbTypeDeclaration );
	}

	public static class ValueImpl implements Value {
		private final ExportableColumn column;
		private final Table table;
		private final BasicType<?> type;
		private final Database database;

		public ValueImpl(ExportableColumn column, Table table, BasicType<?> type, Database database) {
			this.column = column;
			this.table = table;
			this.type = type;
			this.database = database;
		}

		@Override
		public Value copy() {
			return new ValueImpl( column, table, type, database );
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
		public Type getType() throws MappingException {
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
		public boolean isValid(Mapping mapping) throws MappingException {
			return false;
		}

		@Override
		public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		}

		@Override
		public Object accept(ValueVisitor visitor) {
			return null;
		}

		@Override
		public boolean isSame(Value value) {
			return false;
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
	}

}
