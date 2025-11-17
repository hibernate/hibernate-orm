/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.type.AnyType;
import org.hibernate.type.MappingContext;
import org.hibernate.type.MetaType;
import org.hibernate.type.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

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
	private ImplicitDiscriminatorStrategy implicitValueStrategy;
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
		this.implicitValueStrategy = original.implicitValueStrategy;
		this.lazy = original.lazy;
	}

	@Override
	public Any copy() {
		return new Any( this );
	}

	public void addSelectable(Selectable selectable) {
		if ( selectable != null ) {
			if ( selectable instanceof Column column ) {
				super.justAddColumn( column );
			}
			else if ( selectable instanceof Formula formula ) {
				super.justAddFormula( formula );
			}
		}
	}

	private void applySelectableToSuper(Selectable selectable) {
		if ( selectable instanceof Column column ) {
			super.justAddColumn( column );
		}
		else if ( selectable instanceof Formula formula ) {
			super.justAddFormula( formula );
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
			final Type discriminatorType =
					discriminatorDescriptor != null ? discriminatorDescriptor.getType() : metaMapping.getType();
			final Type identifierType = keyDescriptor != null ? keyDescriptor.getType() : keyMapping.getType();
			final MetaType metaType = new MetaType( discriminatorType, implicitValueStrategy, metaValueToEntityNameMap );
			resolvedType = new AnyType( getTypeConfiguration(), metaType, identifierType, isLazy() );
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
			if ( selectable instanceof Column column ) {
				metaMapping.addColumn( column );
			}
			else if ( selectable instanceof Formula formula ) {
				metaMapping.addFormula( formula );
			}
		}
		else {
			if ( selectable instanceof Column column ) {
				keyMapping.addColumn( column );
			}
			else if ( selectable instanceof Formula formula ) {
				keyMapping.addFormula( formula );
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

	public void setMetaValues(Map<Object,String> metaValueToEntityNameMap) {
		this.metaValueToEntityNameMap = metaValueToEntityNameMap;
	}

	/**
	 * Set the strategy for dealing with discriminator mappings which are not explicitly defined by
	 * {@linkplain org.hibernate.annotations.AnyDiscriminatorValue}.
	 *
	 * @apiNote {@code null} indicates to not allow implicit mappings.
	 *
	 * @since 7.0
	 */
	@Incubating
	public void setImplicitDiscriminatorValueStrategy(ImplicitDiscriminatorStrategy implicitValueStrategy) {
		this.implicitValueStrategy = implicitValueStrategy;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) {
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof Any any && isSame( any );
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

	@Override
	public boolean isValid(MappingContext mappingContext) throws MappingException {
		if ( discriminatorDescriptor != null ) {
			return discriminatorDescriptor.isValid( mappingContext ) && keyDescriptor.isValid( mappingContext );
		}
		return metaMapping.isValid( mappingContext ) && keyMapping.isValid( mappingContext );
	}

	private static String columnName(Column column, MetadataBuildingContext buildingContext) {
		final JdbcServices jdbcServices =
				buildingContext.getBootstrapContext().getServiceRegistry()
						.requireService( JdbcServices.class );
		return column.getQuotedName( jdbcServices.getDialect() );
	}

	public void setDiscriminator(BasicValue discriminatorDescriptor) {
		this.discriminatorDescriptor = discriminatorDescriptor;
		if ( discriminatorDescriptor.getColumn() instanceof Column column ) {
			justAddColumn(
					column,
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
		discriminatorValueMappings.forEach( (value, entity) -> metaValueToEntityNameMap.put( value, entity.getName() ) );
	}

	public void setKey(BasicValue keyDescriptor) {
		this.keyDescriptor = keyDescriptor;
		if ( keyDescriptor.getColumn() instanceof Column column ) {
			justAddColumn(
					column,
					keyDescriptor.isColumnInsertable( 0 ),
					keyDescriptor.isColumnUpdateable( 0 )
			);
		}
		else {
			justAddFormula( (Formula) keyDescriptor.getColumn() );
		}
	}

	/**
	 * The discriminator {@linkplain Value}
	 */
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
		public boolean isValid(MappingContext mappingContext) {
			return columnName != null
				&& getType().getColumnSpan( mappingContext ) == 1;
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
	}
}
