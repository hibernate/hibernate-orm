/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * A value which is "typed" by reference to some other
 * value (for example, a foreign key is typed by the
 * referenced primary key).
 *
 * @author Gavin King
 */
public class DependantValue extends BasicValue {
	private KeyValue wrappedValue;
	private boolean nullable;
	private boolean updateable;

	public DependantValue(MetadataBuildingContext buildingContext, MappedTable table, KeyValue prototype) {
		super( buildingContext, table );
		this.wrappedValue = prototype;
	}

	@Override
	protected void setSqlTypeDescriptorResolver(Column column) {
		column.setSqlTypeDescriptorResolver( new DependantValueSqlTypeDescriptorResolver( columns.size() - 1 ) );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return wrappedValue.getJavaTypeDescriptor();
	}

	public class DependantValueSqlTypeDescriptorResolver implements SqlTypeDescriptorResolver {
		private int index;

		public DependantValueSqlTypeDescriptorResolver(int index) {
			this.index = index;
		}

		@Override
		public SqlTypeDescriptor resolveSqlTypeDescriptor() {
			return ( (Column) wrappedValue.getMappedColumns().get( index ) ).getSqlTypeDescriptor();
		}
	}

	public void setTypeUsingReflection(String className, String propertyName) {}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public boolean isNullable() {
		return nullable;
	}
	
	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}
	
	public boolean isUpdateable() {
		return updateable;
	}
	
	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}
}
