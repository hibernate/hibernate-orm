/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.AnyType;
import org.hibernate.type.MappingContext;
import org.hibernate.type.MetaType;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

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
	private Map<DiscriminatorValue,String> metaValueToEntityNameMap;
	private String implicitValueStrategyClassName;
	private transient ImplicitDiscriminatorStrategy implicitValueStrategy;
	private boolean lazy = true;
	private transient TypeConfiguration typeConfiguration;

	private transient AnyType resolvedType;

	public Any(MetadataBuildingContext buildingContext, Table table) {
		this( buildingContext, table, false );
	}

	public Any(MetadataBuildingContext buildingContext, Table table, boolean annotations) {
		super( buildingContext, table );
		this.typeConfiguration = buildingContext.getTypeConfiguration();
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
		this.typeConfiguration = original.typeConfiguration;

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
		this.implicitValueStrategyClassName = original.implicitValueStrategyClassName;
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
			resolvedType = new AnyType( typeConfiguration, metaType, identifierType, isLazy() );
		}
		return resolvedType;
	}

	public void reattachTypeConfiguration(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
		this.resolvedType = null;
		if ( metaMapping != null ) {
			metaMapping.reattachTypeConfiguration( typeConfiguration );
		}
		if ( keyMapping instanceof KeyValue keyValue ) {
			keyValue.reattachTypeConfiguration( typeConfiguration );
		}
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
		if ( discriminatorDescriptor == null && !hasColumns() ) {
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

	public Map<DiscriminatorValue,String> getMetaValues() {
		return metaValueToEntityNameMap;
	}

	public void setMetaValues(Map<DiscriminatorValue,String> metaValueToEntityNameMap) {
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
		implicitValueStrategyClassName = implicitValueStrategy == null
				? null
				: implicitValueStrategy.getClass().getName();
	}

	@SuppressWarnings("unchecked")
	public void reattachImplicitDiscriminatorStrategy(
			ClassLoaderAccess classLoaderAccess,
			ManagedBeanRegistry managedBeanRegistry) {
		if ( implicitValueStrategyClassName != null ) {
			final Class<? extends ImplicitDiscriminatorStrategy> strategyClass =
					(Class<? extends ImplicitDiscriminatorStrategy>) (Class<?>)
							classLoaderAccess.classForName( implicitValueStrategyClassName );
			implicitValueStrategy = managedBeanRegistry.getBean( strategyClass ).getBeanInstance();
		}
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	@Override
	public void setTypeUsingReflection(
			String className,
			String propertyName,
			MetadataBuildingContext buildingContext) {
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

	private static String columnName(Column column, Database database) {
		return column.getQuotedName( database.getDialect() );
	}

	public void setDiscriminator(BasicValue discriminatorDescriptor) {
		this.discriminatorDescriptor = discriminatorDescriptor;
		if ( getMappingRole() != null ) {
			discriminatorDescriptor.setMappingRole(
					getMappingRole().append( MappingRole.PartKind.DISCRIMINATOR )
			);
		}
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

	public void setDiscriminatorValueMappings(Map<DiscriminatorValue, Class<?>> discriminatorValueMappings) {
		metaValueToEntityNameMap = new HashMap<>();
		discriminatorValueMappings.forEach( (value, entity) -> metaValueToEntityNameMap.put( value, entity.getName() ) );
	}

	public void setKey(BasicValue keyDescriptor) {
		this.keyDescriptor = keyDescriptor;
		if ( getMappingRole() != null ) {
			keyDescriptor.setMappingRole( getMappingRole().append( MappingRole.PartKind.KEY ) );
		}
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

	@Override
	public void setMappingRole(MappingRole mappingRole) {
		super.setMappingRole( mappingRole );
		if ( metaMapping != null ) {
			metaMapping.setMappingRole(
					mappingRole == null ? null : mappingRole.append( MappingRole.PartKind.DISCRIMINATOR )
			);
		}
		if ( keyMapping != null ) {
			keyMapping.setMappingRole(
					mappingRole == null ? null : mappingRole.append( MappingRole.PartKind.KEY )
			);
		}
		if ( discriminatorDescriptor != null ) {
			discriminatorDescriptor.setMappingRole(
					mappingRole == null ? null : mappingRole.append( MappingRole.PartKind.DISCRIMINATOR )
			);
		}
		if ( keyDescriptor != null ) {
			keyDescriptor.setMappingRole(
					mappingRole == null ? null : mappingRole.append( MappingRole.PartKind.KEY )
			);
		}
	}

	/**
	 * The discriminator {@linkplain Value}
	 */
	public static class MetaValue extends SimpleValue {
		private String typeName;
		private String columnName;
		private final Database database;
		private transient TypeConfiguration typeConfiguration;

		private transient Consumer<Selectable> selectableConsumer;

		public MetaValue(
				Consumer<Selectable> selectableConsumer,
				MetadataBuildingContext buildingContext) {
			super( buildingContext );
			this.database = buildingContext.getMetadataCollector().getDatabase();
			this.typeConfiguration = buildingContext.getTypeConfiguration();
			this.selectableConsumer = selectableConsumer;
		}

		public MetaValue(
				Consumer<Selectable> selectableConsumer,
				MetadataBuildingContext buildingContext,
				Table table) {
			super( buildingContext, table );
			this.database = buildingContext.getMetadataCollector().getDatabase();
			this.typeConfiguration = buildingContext.getTypeConfiguration();
			this.selectableConsumer = selectableConsumer;
		}

		private MetaValue(MetaValue original) {
			super( original );
			this.typeName = original.typeName;
			this.columnName = original.columnName;
			this.database = original.database;
			this.typeConfiguration = original.typeConfiguration;
			this.selectableConsumer = original.selectableConsumer;
		}

		@Override
		public MetaValue copy() {
			return new MetaValue( this );
		}

		@Override
		public Type getType() throws MappingException {
			return typeConfiguration.getBasicTypeRegistry().getRegisteredType( typeName );
		}

		private void reattachTypeConfiguration(TypeConfiguration typeConfiguration) {
			this.typeConfiguration = typeConfiguration;
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
			this.columnName = columnName( column, database );
			selectableConsumer.accept( column );
			column.setValue( this );
		}

		@Override
		public void addColumn(Column column, boolean isInsertable, boolean isUpdatable) {
			if ( columnName != null ) {
				throw new MappingException( "ANY discriminator already contained column" );
			}
			super.addColumn( column, isInsertable, isUpdatable );
			this.columnName = columnName( column, database );
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
		private transient TypeConfiguration typeConfiguration;

		private transient Consumer<Selectable> selectableConsumer;

		public KeyValue(
				Consumer<Selectable> selectableConsumer,
				MetadataBuildingContext buildingContext) {
			super( buildingContext );
			this.typeConfiguration = buildingContext.getTypeConfiguration();
			this.selectableConsumer = selectableConsumer;
		}

		public KeyValue(
				Consumer<Selectable> selectableConsumer,
				MetadataBuildingContext buildingContext,
				Table table) {
			super( buildingContext, table );
			this.typeConfiguration = buildingContext.getTypeConfiguration();
			this.selectableConsumer = selectableConsumer;
		}

		private KeyValue(KeyValue original) {
			super( original );
			this.typeName = original.typeName;
			this.typeConfiguration = original.typeConfiguration;
			this.selectableConsumer = original.selectableConsumer;
		}

		@Override
		public KeyValue copy() {
			return new KeyValue( this );
		}

		@Override
		public Type getType() throws MappingException {
			return typeConfiguration.getBasicTypeRegistry().getRegisteredType( typeName );
		}

		private void reattachTypeConfiguration(TypeConfiguration typeConfiguration) {
			this.typeConfiguration = typeConfiguration;
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
