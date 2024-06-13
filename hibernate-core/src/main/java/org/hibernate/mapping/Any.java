/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.HashMap;
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
 * A mapping model object representing a {@linkplain org.hibernate.annotations.Any polymorphic association}
 * to one of several tables.
 *
 * @author Gavin King
 */
public class Any extends SimpleValue {
	// hbm.xml mapping
	private final MetaValue metaMapping;
	private final SimpleValue keyMapping;

	// annotations
	private BasicValue discriminatorDescriptor;
	private BasicValue keyDescriptor;

	// common
	private Map<Object,String> metaValueToEntityNameMap;
	private boolean lazy = true;

	private AnyType resolvedType;

	public Any(MetadataBuildingContext buildingContext, Table table) {
		this( buildingContext, table, false );
	}

	public Any(MetadataBuildingContext buildingContext, Table table, boolean annotations) {
		super( buildingContext, table );

		if ( ! annotations ) {
			metaMapping = new MetaValue( this::applySelectableToSuper, buildingContext, table );
			metaMapping.setTypeName( "string" );
			keyMapping = new KeyValue( this::applySelectableToSuper, buildingContext, table );
		}
		else {
			metaMapping = null;
			keyMapping = null;
		}

	}

	public Any(Any original) {
		super( original );

		this.metaMapping = original.metaMapping == null ? null : original.metaMapping.copy();
		this.keyMapping = original.keyMapping == null ? null : (SimpleValue) original.keyMapping.copy();

		// annotations
		this.discriminatorDescriptor = original.discriminatorDescriptor == null
				? null
				: original.discriminatorDescriptor.copy();
		this.keyDescriptor = original.keyDescriptor == null ? null : original.keyDescriptor.copy();

		// common
		this.metaValueToEntityNameMap = original.metaValueToEntityNameMap == null
				? null
				: new HashMap<>(original.metaValueToEntityNameMap);
		this.lazy = original.lazy;
	}

	@Override
	public Any copy() {
		return new Any( this );
	}

	public void addSelectable(Selectable selectable) {
		if ( selectable == null ) {
			return;
		}

		if ( selectable instanceof Column ) {
			super.justAddColumn( (Column) selectable );
		}
		else {
			super.justAddFormula( (Formula) selectable );
		}
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

	public BasicValue getDiscriminatorDescriptor() {
		return discriminatorDescriptor;
	}

	public BasicValue getKeyDescriptor() {
		return keyDescriptor;
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
			final Type discriminatorType;
			if ( discriminatorDescriptor != null ) {
				discriminatorType = discriminatorDescriptor.getType();
			}
			else {
				discriminatorType = metaMapping.getType();
			}

			final Type identifierType;
			if ( keyDescriptor != null ) {
				identifierType = keyDescriptor.getType();
			}
			else {
				identifierType = keyMapping.getType();
			}

			resolvedType = MappingHelper.anyMapping(
					discriminatorType,
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

		if ( discriminatorDescriptor == null && getColumnSpan() == 0 ) {
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
				&& Objects.equals( getTypeNameOrNull( keyMapping ), getTypeNameOrNull( other.keyMapping ) )
				&& Objects.equals( getTypeNameOrNull( metaMapping ), getTypeNameOrNull( other.metaMapping ) )
				&& Objects.equals( metaValueToEntityNameMap, other.metaValueToEntityNameMap )
				&& lazy == other.lazy;
	}

	private String getTypeNameOrNull(SimpleValue simpleValue) {
		return simpleValue != null ? simpleValue.getTypeName() : null;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		if ( discriminatorDescriptor != null ) {
			return discriminatorDescriptor.isValid( mapping ) && keyDescriptor.isValid( mapping );
		}
		return metaMapping.isValid( mapping ) && keyMapping.isValid( mapping );
	}

	private static String columnName(Column column, MetadataBuildingContext buildingContext) {
		final JdbcServices jdbcServices = buildingContext
				.getBootstrapContext()
				.getServiceRegistry()
				.requireService( JdbcServices.class );
		return column.getQuotedName( jdbcServices.getDialect() );
	}

	public void setDiscriminator(BasicValue discriminatorDescriptor) {
		this.discriminatorDescriptor = discriminatorDescriptor;
		if ( discriminatorDescriptor.getColumn() instanceof Column ) {
			justAddColumn(
					(Column) discriminatorDescriptor.getColumn(),
					discriminatorDescriptor.isColumnInsertable( 0 ),
					discriminatorDescriptor.isColumnUpdateable( 0 )
			);
		}
		else {
			justAddFormula( (Formula) discriminatorDescriptor.getColumn() );
		}
	}

	public void setDiscriminatorValueMappings(Map<Object, Class<?>> discriminatorValueMappings) {
		metaValueToEntityNameMap = new HashMap<>();
		discriminatorValueMappings.forEach( (value, entity) -> {
			metaValueToEntityNameMap.put( value, entity.getName() );
		} );
	}

	public void setKey(BasicValue keyDescriptor) {
		this.keyDescriptor = keyDescriptor;
		if ( keyDescriptor.getColumn() instanceof Column ) {
			justAddColumn(
					(Column) keyDescriptor.getColumn(),
					keyDescriptor.isColumnInsertable( 0 ),
					keyDescriptor.isColumnUpdateable( 0 )
			);
		}
		else {
			justAddFormula( (Formula) keyDescriptor.getColumn() );
		}
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

		private MetaValue(MetaValue original) {
			super( original );
			this.typeName = original.typeName;
			this.columnName = original.columnName;
			this.selectableConsumer = original.selectableConsumer;
		}

		@Override
		public MetaValue copy() {
			return new MetaValue( this );
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

		private KeyValue(KeyValue original) {
			super( original );
			this.typeName = original.typeName;
			this.selectableConsumer = original.selectableConsumer;
		}

		@Override
		public KeyValue copy() {
			return new KeyValue( this );
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
			// check
			return super.isValid( mapping );
		}
	}
}
