/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.type.AnyType;
import org.hibernate.type.Type;

/**
 * A Hibernate "any" type (ie. polymorphic association to
 * one-of-several tables).
 * @author Gavin King
 */
public class Any extends SimpleValue {
	private final MetaValue metaMapping;
	private final SimpleValue keyMapping;

	private Map<Object,String> metaValueToEntityNameMap;
	private boolean lazy = true;

	private AnyType resolvedType;

	public Any(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );

		this.metaMapping = new MetaValue( this::applySelectableToSuper, buildingContext, table );
		this.keyMapping = new KeyValue( this::applySelectableToSuper, buildingContext, table );

		this.metaMapping.setTypeName( "string" );
	}

	private void applySelectableToSuper(Selectable selectable) {
		if ( selectable instanceof Column ) {
			super.justAddColumn( (Column) selectable );
		}
		else {
			assert selectable instanceof Formula;
			super.justAddFormula( (Formula) selectable );
		}
	}

	public MetaValue getMetaMapping() {
		return metaMapping;
	}

	public SimpleValue getKeyMapping() {
		return keyMapping;
	}

	public String getIdentifierType() {
		return keyMapping.getTypeName();
	}

	public void setIdentifierType(String identifierType) {
		this.keyMapping.setTypeName( identifierType );
	}

	@Override
	public AnyType getType() throws MappingException {
		if ( resolvedType == null ) {
			final Type metaType = metaMapping.getType();
			final Type identifierType = keyMapping.getType();

			resolvedType = MappingHelper.anyMapping(
					metaType,
					identifierType,
					metaValueToEntityNameMap,
					isLazy(),
					getBuildingContext()
			);
		}

		return resolvedType;
	}

	@Override
	public void addColumn(Column column) {
		applySelectableLocally( column );
	}

	@Override
	public void addColumn(Column column, boolean isInsertable, boolean isUpdatable) {
		applySelectableLocally( column );
	}

	@Override
	public void addFormula(Formula formula) {
		applySelectableLocally( formula );
	}

	private void applySelectableLocally(Selectable selectable) {
		// note: adding column to meta or key mapping ultimately calls back into `#applySelectableToSuper`
		//		to add the column to the ANY super.
		if ( getColumnSpan() == 0 ) {
			if ( selectable instanceof Column ) {
				metaMapping.addColumn( (Column) selectable );
			}
			else {
				assert selectable instanceof Formula;
				metaMapping.addFormula( (Formula) selectable );
			}
		}
		else {
			if ( selectable instanceof Column ) {
				keyMapping.addColumn( (Column) selectable );
			}
			else {
				assert selectable instanceof Formula;
				keyMapping.addFormula( (Formula) selectable );
			}
		}
	}

	public String getMetaType() {
		return metaMapping.typeName;
	}

	public void setMetaType(String type) {
		metaMapping.setTypeName( type );
	}

	public Map<Object,String> getMetaValues() {
		return metaValueToEntityNameMap;
	}

	@SuppressWarnings( "rawtypes" )
	public void setMetaValues(Map metaValueToEntityNameMap) {
		//noinspection unchecked
		this.metaValueToEntityNameMap = metaValueToEntityNameMap;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public void setTypeUsingReflection(String className, String propertyName)
		throws MappingException {
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof Any && isSame( (Any) other );
	}

	public boolean isSame(Any other) {
		return super.isSame( other )
				&& Objects.equals( keyMapping.getTypeName(), other.keyMapping.getTypeName() )
				&& Objects.equals( metaMapping.getTypeName(), other.keyMapping.getTypeName() )
				&& Objects.equals( metaValueToEntityNameMap, other.metaValueToEntityNameMap )
				&& lazy == other.lazy;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		return metaMapping.isValid( mapping ) && keyMapping.isValid( mapping );
	}

	private static String columnName(Column column, MetadataBuildingContext buildingContext) {
		final JdbcServices jdbcServices = buildingContext
				.getBootstrapContext()
				.getServiceRegistry()
				.getService( JdbcServices.class );

		return column.getQuotedName( jdbcServices .getDialect() );
	}

	public static class MetaValue extends SimpleValue {
		private String typeName;
		private String columnName;

		private final Consumer<Selectable> selectableConsumer;

		public MetaValue(
				Consumer<Selectable> selectableConsumer,
				MetadataBuildingContext buildingContext) {
			super( buildingContext );
			this.selectableConsumer = selectableConsumer;
		}

		public MetaValue(
				Consumer<Selectable> selectableConsumer,
				MetadataBuildingContext buildingContext,
				Table table) {
			super( buildingContext, table );
			this.selectableConsumer = selectableConsumer;
		}

		@Override
		public Type getType() throws MappingException {
			return getMetadata().getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( typeName );
		}

		@Override
		public String getTypeName() {
			return typeName;
		}

		@Override
		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}

		public String getColumnName() {
			return columnName;
		}

		@Override
		public void addColumn(Column column) {
			if ( columnName != null ) {
				throw new MappingException( "ANY discriminator already contained column" );
			}

			super.addColumn( column );
			this.columnName = columnName( column, getBuildingContext() );

			selectableConsumer.accept( column );
			column.setValue( this );
		}

		@Override
		public void addColumn(Column column, boolean isInsertable, boolean isUpdatable) {
			if ( columnName != null ) {
				throw new MappingException( "ANY discriminator already contained column" );
			}

			super.addColumn( column, isInsertable, isUpdatable );
			this.columnName = columnName( column, getBuildingContext() );

			selectableConsumer.accept( column );
			column.setValue( this );
		}

		@Override
		public void addFormula(Formula formula) {
			if ( columnName != null ) {
				throw new MappingException( "ANY discriminator already contained column" );
			}

			super.addFormula( formula );
			columnName = formula.getFormula();

			selectableConsumer.accept( formula );
		}

		@Override
		public boolean isValid(Mapping mapping) {
			return columnName != null
					&& getType().getColumnSpan( mapping ) == 1;
		}
	}

	public static class KeyValue extends SimpleValue {
		private String typeName;

		private final Consumer<Selectable> selectableConsumer;

		public KeyValue(
				Consumer<Selectable> selectableConsumer,
				MetadataBuildingContext buildingContext) {
			super( buildingContext );
			this.selectableConsumer = selectableConsumer;
		}

		public KeyValue(
				Consumer<Selectable> selectableConsumer,
				MetadataBuildingContext buildingContext,
				Table table) {
			super( buildingContext, table );
			this.selectableConsumer = selectableConsumer;
		}

		@Override
		public Type getType() throws MappingException {
			return getMetadata().getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( typeName );
		}

		@Override
		public String getTypeName() {
			return typeName;
		}

		@Override
		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}

		@Override
		public void addColumn(Column column) {
			super.addColumn( column );

			selectableConsumer.accept( column );
		}

		@Override
		public void addColumn(Column column, boolean isInsertable, boolean isUpdatable) {
			super.addColumn( column, isInsertable, isUpdatable );

			selectableConsumer.accept( column );
		}

		@Override
		public void addFormula(Formula formula) {
			super.addFormula( formula );

			selectableConsumer.accept( formula );
		}

		@Override
		public boolean isValid(Mapping mapping) throws MappingException {
			return super.isValid( mapping );
		}
	}
}
