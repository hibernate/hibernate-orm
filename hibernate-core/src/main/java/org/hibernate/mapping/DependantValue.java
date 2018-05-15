/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * A value which is "typed" by reference to some other
 * value (for example, a foreign key is typed by the
 * referenced primary key).
 *
 * @author Gavin King
 */
public class DependantValue extends SimpleValue {
	private KeyValue wrappedValue;
	private boolean nullable;
	private boolean updateable;
	private boolean isNationalized;

	/**
	 * @deprecated since 6.0, use {@link #DependantValue(MetadataBuildingContext, MappedTable, KeyValue)}
	 */
	@Deprecated
	public DependantValue(MetadataBuildingContext metadata, Table table, KeyValue prototype) {
		this( metadata, (MappedTable) table, prototype );
	}

	public DependantValue(MetadataBuildingContext metadata, MappedTable table, KeyValue prototype) {
		super( metadata, table );
		registerResolver( metadata );
		this.wrappedValue = prototype;
	}

	private void registerResolver(MetadataBuildingContext metadata) {
		metadata.getMetadataCollector().registerValueMappingResolver( this::resolve );
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return wrappedValue.getJavaTypeMapping();
	}

	@Override
	public Boolean resolve(ResolutionContext context) {
		Iterator<MappedColumn> columnsIterator = columns.iterator();
		List<Column> wrappedValueColumns = wrappedValue.getMappedColumns();
		for ( Column wrappedValueColumn : wrappedValueColumns ) {
			if ( wrappedValueColumn.getJavaTypeMapping() == null ) {
				return false;
			}
			Column column = (Column) columnsIterator.next();
			column.setJavaTypeMapping( wrappedValueColumn.getJavaTypeMapping() );
			column.setSqlTypeDescriptorAccess( wrappedValueColumn::getSqlTypeDescriptor );
		}
		return true;
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

	public void makeNationalized() {
		this.isNationalized = true;
	}

	public boolean isNationalized() {
		return isNationalized;
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof DependantValue && isSame( (DependantValue) other );
	}

	public boolean isSame(DependantValue other) {
		return super.isSame( other )
				&& isSame( wrappedValue, other.wrappedValue );
	}

	@Override
	public ForeignKey createForeignKey() throws MappingException {
		return wrappedValue.createForeignKey();
	}
}
