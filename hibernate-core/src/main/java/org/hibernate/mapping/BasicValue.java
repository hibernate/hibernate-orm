/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.mapping.internal.context.MappingResolutionState;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.ReflectHelper.reflectedPropertyType;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * @author Steve Ebersole
 * @author Yanming Zhou
 */
public class BasicValue extends SimpleValue {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// incoming "configuration" values

	private String explicitTypeName;
	private Map<String,String> explicitLocalTypeParams;
	private Class<? extends UserType<?>> explicitCustomType;

	private SourceJavaType implicitSourceJavaType;

	private EnumType enumerationStyle;
	@SuppressWarnings("deprecation")
	private TemporalType temporalPrecision;
	private TimeZoneStorageType timeZoneStorageType;
	private boolean isSoftDelete;
	private SoftDeleteType softDeleteStrategy;

	private java.lang.reflect.Type resolvedJavaType;

	private String ownerName;
	private String propertyName;
	private AggregateColumn aggregateColumn;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Resolved state - available after `#resolve`
	private transient Resolution<?> resolution;
	private transient boolean resolutionFinalized;
	private transient Integer jdbcTypeCode;


	/**
	 * Creates a plain {@code BasicValue}.
	 * <p>
	 * The value is not registered for collector-driven self-resolution.  New
	 * bootstrap paths should apply an explicit {@link Resolution} when the
	 * value's type has been resolved.
	 */
	public BasicValue(MetadataBuildingContext buildingContext) {
		super( buildingContext );
	}

	/**
	 * Creates a plain {@code BasicValue}.
	 * <p>
	 * The value is not registered for collector-driven self-resolution.  New
	 * bootstrap paths should apply an explicit {@link Resolution} when the
	 * value's type has been resolved.
	 */
	public BasicValue(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );
	}

	public static BasicValue unregistered(MetadataBuildingContext buildingContext) {
		return new BasicValue( buildingContext );
	}

	public static BasicValue unregistered(MetadataBuildingContext buildingContext, Table table) {
		return new BasicValue( buildingContext, table );
	}

	/**
	 * Create a {@code BasicValue} with a precomputed resolution.
	 * <p>
	 * The resolution is immediately finalized against the current selectable
	 * state.
	 */
	public BasicValue(MetadataBuildingContext buildingContext, Resolution<?> resolution) {
		this( buildingContext, null, resolution );
	}

	/**
	 * Create a {@code BasicValue} with a precomputed resolution.
	 * <p>
	 * The resolution is immediately finalized against the current selectable
	 * state.
	 */
	public BasicValue(MetadataBuildingContext buildingContext, Table table, Resolution<?> resolution) {
		this( buildingContext, table );
		applyResolution( resolution, MappingResolutionState.from( buildingContext ) );
	}

	public BasicValue(BasicValue original) {
		super( original );
		this.explicitTypeName = original.explicitTypeName;
		this.explicitLocalTypeParams =
				original.explicitLocalTypeParams == null ? null
						: new HashMap<>( original.explicitLocalTypeParams );
		this.implicitSourceJavaType = original.implicitSourceJavaType;
		this.enumerationStyle = original.enumerationStyle;
		this.temporalPrecision = original.temporalPrecision;
		this.timeZoneStorageType = original.timeZoneStorageType;
		this.resolvedJavaType = original.resolvedJavaType;
		this.ownerName = original.ownerName;
		this.propertyName = original.propertyName;
		this.isSoftDelete = original.isSoftDelete;
		this.softDeleteStrategy = original.softDeleteStrategy;
		this.aggregateColumn = original.aggregateColumn;
		this.jdbcTypeCode = original.jdbcTypeCode;
		this.resolution = original.resolution;
		this.resolutionFinalized = false;
	}

	@Override
	public BasicValue copy() {
		return new BasicValue( this );
	}

	public boolean isSoftDelete() {
		return isSoftDelete;
	}

	public SoftDeleteType getSoftDeleteStrategy() {
		return softDeleteStrategy;
	}

	public void makeSoftDelete(SoftDeleteType strategy) {
		isSoftDelete = true;
		softDeleteStrategy = strategy;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Setters - in preparation of resolution

	@Override
	public void setTypeUsingReflection(
			String className,
			String propertyName,
			MetadataBuildingContext buildingContext) throws MappingException {
		if ( resolution != null ) {
			throw new IllegalStateException( "BasicValue already resolved" );
		}
		this.ownerName = className;
		this.propertyName = propertyName;
		super.setTypeUsingReflection( className, propertyName, buildingContext );
	}

	public void setEnumerationStyle(EnumType enumerationStyle) {
		this.enumerationStyle = enumerationStyle;
	}

	public EnumType getEnumerationStyle() {
		return enumerationStyle;
	}

	public EnumType getEnumeratedType() {
		return getEnumerationStyle();
	}

	public TimeZoneStorageType getTimeZoneStorageType() {
		return timeZoneStorageType;
	}

	public void setTimeZoneStorageType(TimeZoneStorageType timeZoneStorageType) {
		this.timeZoneStorageType = timeZoneStorageType;
	}

	public void setJpaAttributeConverterDescriptor(ConverterDescriptor<?,?> descriptor) {
		setAttributeConverterDescriptor( descriptor );
		super.setJpaAttributeConverterDescriptor( descriptor );
	}

	public void setImplicitSourceJavaType(SourceJavaType implicitSourceJavaType) {
		this.implicitSourceJavaType = implicitSourceJavaType;
	}

	public SourceJavaType getImplicitSourceJavaType() {
		return implicitSourceJavaType;
	}

	public Selectable getColumn() {
		return hasColumns() ? getColumn( 0 ) : null;
	}

	public java.lang.reflect.Type getResolvedJavaType() {
		return resolvedJavaType;
	}

	public long getColumnLength() {
		if ( getColumn() instanceof Column column ) {
			final Long length = column.getLength();
			return length == null ? JdbcTypeIndicators.NO_COLUMN_LENGTH : length;
		}
		else {
			return JdbcTypeIndicators.NO_COLUMN_LENGTH;
		}
	}

	public int getColumnPrecision() {
		if ( getColumn() instanceof Column column ) {
			final Integer temporalPrecision = column.getTemporalPrecision();
			if ( temporalPrecision != null ) {
				return temporalPrecision;
			}
			else {
				final Integer precision = column.getPrecision();
				return precision == null ? JdbcTypeIndicators.NO_COLUMN_PRECISION : precision;
			}
		}
		else {
			return JdbcTypeIndicators.NO_COLUMN_PRECISION;
		}
	}

	public int getColumnScale() {
		if ( getColumn() instanceof Column column ) {
			final Integer scale = column.getScale();
			return scale == null ? JdbcTypeIndicators.NO_COLUMN_SCALE : scale;
		}
		else {
			return JdbcTypeIndicators.NO_COLUMN_SCALE;
		}
	}

	@Override
	public void addColumn(Column incomingColumn) {
		super.addColumn( incomingColumn );
		checkSelectable( incomingColumn );
	}

	@Override
	public void copyTypeFrom(SimpleValue sourceValue) {
		super.copyTypeFrom( sourceValue );
		if ( sourceValue instanceof BasicValue basicValue ) {
			resolution = basicValue.resolution;
			resolutionFinalized = false;
		}
	}

	private void checkSelectable(Selectable incomingColumn) {
		if ( incomingColumn == null ) {
			throw new IllegalArgumentException( "Incoming column was null" );
		}

//		final Selectable column = getColumn();
//		if ( column == incomingColumn || column.getText().equals( incomingColumn.getText() ) ) {
//			LOG.debugf( "Skipping column re-registration: %s.%s", getTable().getName(), column.getText() );
//		}
//		else {
//			throw new IllegalStateException(
//					"BasicValue [" + ownerName + "." + propertyName +
//							"] already had column associated: `" + column.getText() +
//							"` -> `" + incomingColumn.getText() + "`"
//			);
//		}
	}

	@Override
	public void addColumn(Column incomingColumn, boolean isInsertable, boolean isUpdatable) {
		super.addColumn( incomingColumn, isInsertable, isUpdatable );
		checkSelectable( incomingColumn );
	}

	@Override
	public void addFormula(Formula formula) {
		super.addFormula( formula );
		checkSelectable( formula );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Resolution

	@Override
	public Type getType() throws MappingException {
		return requireResolution().getLegacyResolvedBasicType();
	}

	public Resolution<?> getResolution() {
		return resolution;
	}

	public Resolution<?> requireResolution() {
		if ( resolution == null ) {
			throw new IllegalStateException( "BasicValue resolution has not been applied: " + this );
		}
		return resolution;
	}

	public void applyResolution(Resolution<?> resolution, MappingResolutionState state) {
		if ( resolution == null ) {
			throw new IllegalArgumentException( "BasicValue resolution must not be null" );
		}
		if ( this.resolution != null && this.resolution != resolution ) {
			throw new IllegalStateException( "BasicValue resolution already applied" );
		}
		if ( this.resolution == null ) {
			this.resolution = resolution;
			resolutionFinalized = false;
		}
		finalizeResolution( state );
	}

	public Resolution<?> resolve() {
		return requireResolution();
	}

	private void finalizeResolution(MappingResolutionState state) {
		if ( resolutionFinalized ) {
			return;
		}
		resolutionFinalized = true;

		final var metadataCollector = state.metadataCollector();
		final var database = state.database();
		final var dialect = database.getDialect();
		final Size size;
		if ( getColumn() instanceof Column column ) {
			resolveColumn( column, dialect );
			size = column.calculateColumnSize(
					dialect,
					metadataCollector,
					resolution.getLegacyResolvedBasicType()
			);
		}
		else {
			size = Size.nil();
		}
		resolution.getJdbcType().addAuxiliaryDatabaseObjects(
				resolution.getRelationalJavaType(),
				resolution.getValueConverter(),
				size,
				database
		);
	}

	@Override
	public String getExtraCreateTableInfo(org.hibernate.boot.model.relational.Database database) {
		return resolution.getJdbcType()
				.getExtraCreateTableInfo(
						resolution.getRelationalJavaType(),
						getColumn().getText(),
						getTable().getName(),
						database
				);
	}

	private void resolveColumn(Column column, Dialect dialect) {

		if ( column.getSqlTypeCode() == null ) {
			column.setSqlTypeCode( resolution.getJdbcType().getDdlTypeCode() );
		}

//		final String declaration = resolution.getLegacyResolvedBasicType().getSpecializedTypeDeclaration( dialect );
//		if ( declaration != null ) {
//			column.setSpecializedTypeDeclaration( declaration );
//		}

		if ( dialect.supportsColumnCheck() ) {
			final String checkCondition =
					resolution.getLegacyResolvedBasicType()
							.getCheckCondition( column.getQuotedName( dialect ), dialect );
			if ( checkCondition != null ) {
				column.addCheckConstraint( new CheckConstraint( checkCondition ) );
			}
		}
	}

	public AggregateColumn getAggregateColumn() {
		return aggregateColumn;
	}

	public void setAggregateColumn(AggregateColumn aggregateColumn) {
		this.aggregateColumn = aggregateColumn;
	}

	public SelectablePath createSelectablePath(String selectableName) {
		return aggregateColumn != null
				? aggregateColumn.getSelectablePath().append( selectableName )
				: new SelectablePath( selectableName );
	}

	@Incubating
	public java.lang.reflect.Type impliedJavaType(
			TypeConfiguration typeConfiguration,
			ClassLoaderService classLoaderService) {
		if ( resolvedJavaType != null ) {
			return resolvedJavaType;
		}
		else if ( implicitSourceJavaType != null ) {
			return implicitSourceJavaType.asReflectType();
		}
		else if ( ownerName != null && propertyName != null ) {
			return reflectedPropertyType( ownerName, propertyName, classLoaderService );
		}
		else {
			return null;
		}
	}

	public interface SourceJavaType {
		TypeDetails typeDetails();

		Class<?> rawJavaClass();

		java.lang.reflect.Type asReflectType();

		static SourceJavaType from(TypeDetails typeDetails, Class<?> explicitJavaType) {
			return new SourceJavaType() {
				@Override
				public TypeDetails typeDetails() {
					return typeDetails;
				}

				@Override
				public Class<?> rawJavaClass() {
					if ( explicitJavaType != null ) {
						return explicitJavaType;
					}
					if ( typeDetails == null ) {
						return null;
					}
					return typeDetails.determineRawClass().toJavaClass();
				}

				@Override
				public java.lang.reflect.Type asReflectType() {
					if ( explicitJavaType != null ) {
						return explicitJavaType;
					}
					if ( typeDetails == null ) {
						return null;
					}
					if ( typeDetails.getTypeKind() == TypeDetails.Kind.PARAMETERIZED_TYPE ) {
						return ParameterizedTypeImpl.from( typeDetails.asParameterizedType() );
					}
					return rawJavaClass();
				}
			};
		}
	}

	@Internal
	public static TimeZoneStorageStrategy timeZoneStorageStrategy(
			TimeZoneStorageType timeZoneStorageType,
			MetadataBuildingContext buildingContext) {
		return timeZoneStorageType == null
				? buildingContext.getBuildingPlan().getDefaultTimeZoneStorage()
				: switch ( timeZoneStorageType ) {
					case COLUMN -> TimeZoneStorageStrategy.COLUMN;
					case NATIVE -> TimeZoneStorageStrategy.NATIVE;
					case NORMALIZE -> TimeZoneStorageStrategy.NORMALIZE;
					case NORMALIZE_UTC -> TimeZoneStorageStrategy.NORMALIZE_UTC;
					case AUTO, DEFAULT -> buildingContext.getBuildingPlan().getDefaultTimeZoneStorage();
				};
	}

	public void setExplicitTypeParams(Map<String,String> explicitLocalTypeParams) {
		this.explicitLocalTypeParams = explicitLocalTypeParams;
	}

	public Map<String, String> getExplicitTypeParams() {
		return explicitLocalTypeParams;
	}

	public void setExplicitTypeName(String typeName) {
		this.explicitTypeName = typeName;
	}

	String getExplicitTypeName() {
		return explicitTypeName;
	}

	public void setTypeName(String typeName) {
		if ( isEmpty( typeName ) ) {
			super.setTypeName( typeName );
		}
		else {
			setExplicitTypeName( typeName );
			super.setTypeName( typeName );
		}
	}

	public void setExplicitCustomType(Class<? extends UserType<?>> explicitCustomType) {
		if ( explicitCustomType != null ) {
			if ( resolution != null ) {
				throw new UnsupportedOperationException( "Unsupported attempt to set an explicit-custom-type when value is already resolved" );
			}
			else {
				this.explicitCustomType = explicitCustomType;
			}
		}
	}

	public Class<? extends UserType<?>> getExplicitCustomType() {
		return explicitCustomType;
	}

	public Properties buildCustomTypeProperties() {
		final var properties = new Properties();
		if ( isNotEmpty( getTypeParameters() ) ) {
			properties.putAll( getTypeParameters() );
		}
		if ( isNotEmpty( explicitLocalTypeParams ) ) {
			properties.putAll( explicitLocalTypeParams );
		}
		return properties;
	}

	@SuppressWarnings("deprecation")
	public void setTemporalPrecision(TemporalType temporalPrecision) {
		this.temporalPrecision = temporalPrecision;
	}

	@SuppressWarnings("deprecation")
	public TemporalType getTemporalPrecision() {
		return temporalPrecision;
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Incubating
	public void setExplicitJdbcTypeCode(Integer jdbcTypeCode) {
		this.jdbcTypeCode = jdbcTypeCode;
	}

	public Integer getExplicitJdbcTypeCode() {
		return jdbcTypeCode;
	}

	public DynamicParameterizedType.ParameterType createResolutionParameterType(ClassLoaderService classLoaderService) {
		return createParameterType( classLoaderService );
	}

	void setResolvedJavaType(java.lang.reflect.Type resolvedJavaType) {
		this.resolvedJavaType = resolvedJavaType;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Resolved form of {@link BasicValue} as part of interpreting the
	 * boot-time model into the run-time model
	 */
	public interface Resolution<J> {
		/**
		 * The BasicType resolved using the pre-6.0 rules.  This is temporarily
		 * needed because of the split in extracting / binding
		 */
		BasicType<J> getLegacyResolvedBasicType();

		/**
		 * Get the collection of type-parameters collected both locally as well
		 * as from the applied type-def, if one
		 */
		default Properties getCombinedTypeParameters() {
			return null;
		}

		JdbcMapping getJdbcMapping();

		/**
		 * The JavaType for the value as part of the domain model
		 */
		JavaType<J> getDomainJavaType();

		/**
		 * The JavaType for the relational value as part of
		 * the relational model (its JDBC representation)
		 */
		JavaType<?> getRelationalJavaType();

		/**
		 * The JavaType for the relational value as part of
		 * the relational model (its JDBC representation)
		 */
		JdbcType getJdbcType();

		/**
		 * Converter, if any, to convert values between the
		 * domain and relational JavaType representations
		 */
		BasicValueConverter<J,?> getValueConverter();

		/**
		 * The resolved MutabilityPlan
		 */
		MutabilityPlan<J> getMutabilityPlan();

		default void updateResolution(BasicType<?> type) {
			throw new UnsupportedOperationException();
		}
	}
}
