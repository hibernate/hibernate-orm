/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.annotation.Nonnull;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.model.internal.Constructors;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.TimeZoneStorageStrategy;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.process.internal.UserTypeResolution;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.usertype.AnnotationBasedUserType;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;
import org.hibernate.usertype.UserTypeCreationContext;

import static java.lang.Boolean.parseBoolean;
import static org.hibernate.boot.model.convert.spi.ConverterDescriptor.TYPE_NAME_PREFIX;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.internal.util.ReflectHelper.reflectedPropertyType;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.mapping.MappingHelper.injectParameters;

/**
 * @author Steve Ebersole
 * @author Yanming Zhou
 */
public class BasicValue extends SimpleValue
		implements JdbcTypeIndicators, Resolvable, JpaAttributeConverterCreationContext {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// incoming "configuration" values

	private String explicitTypeName;
	private Map<String,String> explicitLocalTypeParams;

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
		applyResolution( resolution );
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
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		if ( resolution != null ) {
			throw new IllegalStateException( "BasicValue already resolved" );
		}
		this.ownerName = className;
		this.propertyName = propertyName;
		super.setTypeUsingReflection( className, propertyName );
	}

	public void setEnumerationStyle(EnumType enumerationStyle) {
		this.enumerationStyle = enumerationStyle;
	}

	public EnumType getEnumerationStyle() {
		return enumerationStyle;
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

	@Override
	public long getColumnLength() {
		if ( getColumn() instanceof Column column ) {
			final Long length = column.getLength();
			return length == null ? NO_COLUMN_LENGTH : length;
		}
		else {
			return NO_COLUMN_LENGTH;
		}
	}

	@Override
	public int getColumnPrecision() {
		if ( getColumn() instanceof Column column ) {
			final Integer temporalPrecision = column.getTemporalPrecision();
			if ( temporalPrecision != null ) {
				return temporalPrecision;
			}
			else {
				final Integer precision = column.getPrecision();
				return precision == null ? NO_COLUMN_PRECISION : precision;
			}
		}
		else {
			return NO_COLUMN_PRECISION;
		}
	}

	@Override
	public int getColumnScale() {
		if ( getColumn() instanceof Column column ) {
			final Integer scale = column.getScale();
			return scale == null ? NO_COLUMN_SCALE : scale;
		}
		else {
			return NO_COLUMN_SCALE;
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
		resolve();
		final var resolution = getResolution();
		assert resolution != null;
		return resolution.getLegacyResolvedBasicType();
	}

	public Resolution<?> getResolution() {
		return resolution;
	}

	public void applyResolution(Resolution<?> resolution) {
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
		finalizeResolution();
	}

	@Override
	public boolean resolve(MetadataBuildingContext buildingContext) {
		resolve();
		return true;
	}

	@Override
	public Resolution<?> resolve() {
		if ( resolution == null ) {
			throw new IllegalStateException( "BasicValue resolution has not been applied: " + this );
		}

		finalizeResolution();
		return resolution;
	}

	private void finalizeResolution() {
		if ( resolutionFinalized ) {
			return;
		}
		resolutionFinalized = true;

		final Size size;
		if ( getColumn() instanceof Column column ) {
			resolveColumn( column, getDialect() );
			size = column.calculateColumnSize(
					getDialect(),
					getMetadataCollector(),
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
				getMetadataCollector().getDatabase(),
				this
		);
	}

	@Override
	public String getExtraCreateTableInfo() {
		return resolution.getJdbcType()
				.getExtraCreateTableInfo(
						resolution.getRelationalJavaType(),
						getColumn().getText(),
						getTable().getName(),
						getMetadataCollector().getDatabase()
				);
	}

	@Override
	public Dialect getDialect() {
		return getMetadata().getDatabase().getDialect();
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

	@Override
	public ManagedBeanRegistry getManagedBeanRegistry() {
		return getBuildingContext().getManagedBeanRegistry();
	}

	@Incubating
	public java.lang.reflect.Type impliedJavaType(TypeConfiguration typeConfiguration) {
		if ( resolvedJavaType != null ) {
			return resolvedJavaType;
		}
		else if ( implicitSourceJavaType != null ) {
			return implicitSourceJavaType.asReflectType();
		}
		else if ( ownerName != null && propertyName != null ) {
			return reflectedPropertyType( ownerName, propertyName, classLoaderService() );
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlTypeDescriptorIndicators

	@Override
	public EnumType getEnumeratedType() {
		return getEnumerationStyle();
	}


	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return resolveJdbcTypeCode( getBuildingContext().getBuildingPlan().getPreferredSqlTypeCodeForBoolean() );
	}

	@Override
	public int getPreferredSqlTypeCodeForDuration() {
		return resolveJdbcTypeCode( getBuildingContext().getBuildingPlan().getPreferredSqlTypeCodeForDuration() );
	}

	@Override
	public int getPreferredSqlTypeCodeForUuid() {
		return resolveJdbcTypeCode( getBuildingContext().getBuildingPlan().getPreferredSqlTypeCodeForUuid() );
	}

	@Override
	public int getPreferredSqlTypeCodeForInstant() {
		return resolveJdbcTypeCode( getBuildingContext().getBuildingPlan().getPreferredSqlTypeCodeForInstant() );
	}

	@Override
	public int getPreferredSqlTypeCodeForArray() {
		return resolveJdbcTypeCode( getBuildingContext().getBuildingPlan().getPreferredSqlTypeCodeForArray() );
	}

	@Override
	public int resolveJdbcTypeCode(int jdbcTypeCode) {
		return aggregateColumn == null
				? jdbcTypeCode
				: getDialect().getAggregateSupport()
						.aggregateComponentSqlTypeCode(
								aggregateColumn.getType().getJdbcType().getDefaultSqlTypeCode(),
								jdbcTypeCode
						);
	}

	@Override
	@Nonnull
	public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
		return timeZoneStorageStrategy( timeZoneStorageType, getBuildingContext() );
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
		else if ( typeName.startsWith( TYPE_NAME_PREFIX ) ) {
			setAttributeConverterDescriptor( typeName );
		}
		else {
			setExplicitTypeName( typeName );
			super.setTypeName( typeName );
		}
	}

	private static int COUNTER;

	public void setExplicitCustomType(Class<? extends UserType<?>> explicitCustomType) {
		if ( explicitCustomType != null ) {
			if ( resolution != null ) {
				throw new UnsupportedOperationException( "Unsupported attempt to set an explicit-custom-type when value is already resolved" );
			}
			else {
				final var parameters = buildCustomTypeProperties();
				resolution = new UserTypeResolution<>(
						new CustomType<>(
								getConfiguredUserTypeBean( explicitCustomType, getTypeAnnotation(), parameters ),
								getTypeConfiguration()
						),
						null,
						parameters
				);
			}
		}
	}

	private Properties buildCustomTypeProperties() {
		final var properties = new Properties();
		if ( isNotEmpty( getTypeParameters() ) ) {
			properties.putAll( getTypeParameters() );
		}
		if ( isNotEmpty( explicitLocalTypeParams ) ) {
			properties.putAll( explicitLocalTypeParams );
		}
		return properties;
	}

	private UserType<?> getConfiguredUserTypeBean(
			Class<? extends UserType<?>> explicitCustomType,
			Annotation typeAnnotation, Properties parameters) {
		final var typeInstance =
				createUserTypeInstance( explicitCustomType, parameters, typeAnnotation );
		if ( typeInstance instanceof TypeConfigurationAware configurationAware ) {
			configurationAware.setTypeConfiguration( getTypeConfiguration() );
		}
		addParameterType( parameters, typeInstance );
		injectParameters( typeInstance, parameters );
		// envers - grr
		setTypeParameters( parameters );
		return typeInstance;
	}

	private UserType<?> createUserTypeInstance(
			Class<? extends UserType<?>> customType,
			Properties parameters,
			Annotation typeAnnotation) {
		final var creationContext = new TypeCreationContext( parameters );
		final var typeInstance = instantiateUserType( customType, typeAnnotation, creationContext );
		if ( typeInstance instanceof AnnotationBasedUserType<?, ?> annotationBased ) {
			initializeAnnotationBasedUserType( typeAnnotation, annotationBased, creationContext );
		}
		return typeInstance;
	}

	private void addParameterType(Properties properties, UserType<?> typeInstance) {
		if ( typeInstance instanceof DynamicParameterizedType
				&& parseBoolean( properties.getProperty( DynamicParameterizedType.IS_DYNAMIC ) )
				&& properties.get( DynamicParameterizedType.PARAMETER_TYPE ) == null ) {
			properties.put( DynamicParameterizedType.PARAMETER_TYPE, createParameterType() );
		}
	}

	private <A extends Annotation> void initializeAnnotationBasedUserType(
			Annotation typeAnnotation,
			AnnotationBasedUserType<A, ?> annotationBased,
			UserTypeCreationContext creationContext) {
		if ( typeAnnotation == null ) {
			throw new AnnotationException( String.format(
					"Custom type '%s' implements 'AnnotationBasedUserType' but no custom type annotation is present",
					annotationBased.getClass().getName() ) );
		}
		annotationBased.initialize( castAnnotationType( typeAnnotation, annotationBased ), creationContext );
	}

	private class TypeCreationContext implements UserTypeCreationContext {
		private final Properties parameters;

		private TypeCreationContext(Properties parameters) {
			this.parameters = parameters;
		}

		@Override
		public MetadataBuildingContext getBuildingContext() {
			return BasicValue.this.getBuildingContext();
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return BasicValue.this.getServiceRegistry();
		}

		@Override
		public MemberDetails getMemberDetails() {
			return BasicValue.this.getMemberDetails();
		}

		@Override
		public Properties getParameters() {
			return parameters;
		}
	}

	private static <A extends Annotation> A castAnnotationType(
			Annotation typeAnnotation, AnnotationBasedUserType<A, ?> annotationBased ) {
		final var annotationType = annotationBased.getClass();
		for ( var iface: annotationType.getGenericInterfaces() ) {
			if ( iface instanceof ParameterizedType parameterizedType
					&& parameterizedType.getRawType() == AnnotationBasedUserType.class ) {
				final var typeArguments = parameterizedType.getActualTypeArguments();
				if ( typeArguments.length > 0
					&& typeArguments[0] instanceof Class<?> annotationClass ) {
					if ( !annotationClass.isInstance( typeAnnotation ) ) {
						throw new AnnotationException( String.format( "Annotation '%s' is not assignable to '%s'",
								annotationType.getName(), iface.getTypeName() ) );
					}
					@SuppressWarnings("unchecked") // safe, we just checked it
					final var castAnnotation = (A) typeAnnotation;
					return castAnnotation;
				}
			}
		}
		throw new AssertionFailure( "Could not find implementing interface" );
	}

	private <T extends UserType<?>, A extends Annotation> T instantiateUserType(
			Class<T> customType, A typeAnnotation,
			UserTypeCreationContext creationContext) {
		try {
			T userType;
			if ( typeAnnotation != null ) {
				@SuppressWarnings("unchecked") // totally safe
				final var annotationType = (Class<A>) typeAnnotation.annotationType();
				// attempt to instantiate it with the annotation and context object as constructor arguments
				userType =
						Constructors.construct( customType,
								annotationType, typeAnnotation,
								UserTypeCreationContext.class, creationContext );
				if ( userType != null ) {
					return userType;
				}
				// attempt to instantiate it with the annotation as a constructor argument
				userType = Constructors.construct( customType, annotationType, typeAnnotation );
				if ( userType != null ) {
					return userType;
				}
			}

			// attempt to instantiate it with the context object as a constructor argument
			userType = Constructors.construct( customType, UserTypeCreationContext.class, creationContext );
			if ( userType != null ) {
				return userType;
			}
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new org.hibernate.InstantiationException( "Could not instantiate custom type", customType, e );
		}

		return getBuildingContext().getBuildingPlan().isAllowExtensionsInCdi()
				? getUserTypeBean( customType, creationContext.getParameters() ).getBeanInstance()
				: FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( customType );
	}

	private <T> ManagedBean<? extends T> getUserTypeBean(Class<T> explicitCustomType, Properties properties) {
		final var producer = getBuildingContext().getCustomTypeProducer();
		final var managedBeanRegistry = getManagedBeanRegistry();
		if ( isNotEmpty( properties ) ) {
			final String name = explicitCustomType.getName() + COUNTER++;
			return managedBeanRegistry.getBean( name, explicitCustomType, producer );
		}
		else {
			return managedBeanRegistry.getBean( explicitCustomType, producer );
		}
	}

	@SuppressWarnings("deprecation")
	public void setTemporalPrecision(TemporalType temporalPrecision) {
		this.temporalPrecision = temporalPrecision;
	}

	@Override @SuppressWarnings("deprecation")
	public TemporalType getTemporalPrecision() {
		return temporalPrecision;
	}

	@Override
	public boolean isPreferJavaTimeJdbcTypesEnabled() {
		return getBuildingContext().getBuildingPlan().isPreferJavaTimeJdbcTypesEnabled();
	}

	@Override
	public boolean isPreferNativeEnumTypesEnabled() {
		return getBuildingContext().getBuildingPlan().isPreferNativeEnumTypesEnabled();
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Internal
	public boolean isDisallowedWrapperArray() {
		return getBuildingContext().getBuildingPlan().getWrapperArrayHandling() == WrapperArrayHandling.DISALLOW
			&& !isLob()
			&& isWrapperByteOrCharacterArray();
	}

	private boolean isWrapperByteOrCharacterArray() {
		final var javaTypeClass = resolve().getDomainJavaType().getJavaTypeClass();
		return javaTypeClass == Byte[].class || javaTypeClass == Character[].class;
	}

	@Incubating
	public void setExplicitJdbcTypeCode(Integer jdbcTypeCode) {
		this.jdbcTypeCode = jdbcTypeCode;
	}

	public Integer getExplicitJdbcTypeCode() {
		return jdbcTypeCode == null ? getPreferredSqlTypeCodeForArray() : jdbcTypeCode;
	}

	Integer getConfiguredJdbcTypeCode() {
		return jdbcTypeCode;
	}

	public DynamicParameterizedType.ParameterType createResolutionParameterType() {
		return createParameterType();
	}

	@Override
	public org.hibernate.boot.registry.classloading.spi.ClassLoaderService classLoaderService() {
		return super.classLoaderService();
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
