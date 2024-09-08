/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.TimeZoneStorageStrategy;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorBypassedImpl;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.internal.InstanceBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolution;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolver;
import org.hibernate.boot.model.process.internal.NamedBasicTypeResolution;
import org.hibernate.boot.model.process.internal.NamedConverterResolution;
import org.hibernate.boot.model.process.internal.UserTypeResolution;
import org.hibernate.boot.model.process.internal.VersionResolution;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.Type;
import org.hibernate.type.WrapperArrayHandling;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.java.spi.JsonJavaType;
import org.hibernate.type.descriptor.java.spi.RegistryHelper;
import org.hibernate.type.descriptor.java.spi.XmlJavaType;
import org.hibernate.type.descriptor.jdbc.BooleanJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import com.fasterxml.classmate.ResolvedType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

import static java.lang.Boolean.parseBoolean;
import static org.hibernate.internal.util.ReflectHelper.reflectedPropertyType;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.mapping.MappingHelper.injectParameters;

/**
 * @author Steve Ebersole
 */
public class BasicValue extends SimpleValue implements JdbcTypeIndicators, Resolvable, JpaAttributeConverterCreationContext {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( BasicValue.class );

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// incoming "configuration" values

	private String explicitTypeName;
	private Map<String,String> explicitLocalTypeParams;

	private Function<TypeConfiguration, BasicJavaType> explicitJavaTypeAccess;
	private Function<TypeConfiguration, JdbcType> explicitJdbcTypeAccess;
	private Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess;
	private Function<TypeConfiguration, java.lang.reflect.Type> implicitJavaTypeAccess;

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
	private Resolution<?> resolution;
	private Integer jdbcTypeCode;


	public BasicValue(MetadataBuildingContext buildingContext) {
		this( buildingContext, null );
	}

	public BasicValue(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );
		buildingContext.getMetadataCollector().registerValueMappingResolver( this::resolve );
	}

	public BasicValue(BasicValue original) {
		super( original );
		this.explicitTypeName = original.explicitTypeName;
		this.explicitLocalTypeParams = original.explicitLocalTypeParams == null
				? null
				: new HashMap<>(original.explicitLocalTypeParams);
		this.explicitJavaTypeAccess = original.explicitJavaTypeAccess;
		this.explicitJdbcTypeAccess = original.explicitJdbcTypeAccess;
		this.explicitMutabilityPlanAccess = original.explicitMutabilityPlanAccess;
		this.implicitJavaTypeAccess = original.implicitJavaTypeAccess;
		this.enumerationStyle = original.enumerationStyle;
		this.temporalPrecision = original.temporalPrecision;
		this.timeZoneStorageType = original.timeZoneStorageType;
		this.resolvedJavaType = original.resolvedJavaType;
		this.ownerName = original.ownerName;
		this.propertyName = original.propertyName;
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

	public void setJpaAttributeConverterDescriptor(ConverterDescriptor descriptor) {
		setAttributeConverterDescriptor( descriptor );

		super.setJpaAttributeConverterDescriptor( descriptor );
	}

	@SuppressWarnings("rawtypes")
	public void setExplicitJavaTypeAccess(Function<TypeConfiguration, BasicJavaType> explicitJavaTypeAccess) {
		this.explicitJavaTypeAccess = explicitJavaTypeAccess;
	}

	public void setExplicitJdbcTypeAccess(Function<TypeConfiguration, JdbcType> jdbcTypeAccess) {
		this.explicitJdbcTypeAccess = jdbcTypeAccess;
	}

	public void setExplicitMutabilityPlanAccess(Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess) {
		this.explicitMutabilityPlanAccess = explicitMutabilityPlanAccess;
	}

	public void setImplicitJavaTypeAccess(Function<TypeConfiguration, java.lang.reflect.Type> implicitJavaTypeAccess) {
		this.implicitJavaTypeAccess = implicitJavaTypeAccess;
	}

	public Selectable getColumn() {
		if ( getColumnSpan() == 0 ) {
			return null;
		}
		return getColumn( 0 );
	}

	public java.lang.reflect.Type getResolvedJavaType() {
		return resolvedJavaType;
	}

	@Override
	public long getColumnLength() {
		final Selectable selectable = getColumn();
		if ( selectable instanceof Column ) {
			final Long length = ( (Column) selectable ).getLength();
			return length == null ? NO_COLUMN_LENGTH : length;
		}
		else {
			return NO_COLUMN_LENGTH;
		}
	}

	@Override
	public int getColumnPrecision() {
		final Selectable selectable = getColumn();
		if ( selectable instanceof Column ) {
			final Column column = (Column) selectable;
			final Integer temporalPrecision = column.getTemporalPrecision();
			if ( temporalPrecision != null ) {
				return temporalPrecision;
			}
			final Integer precision = column.getPrecision();
			return precision == null ? NO_COLUMN_PRECISION : precision;
		}
		else {
			return NO_COLUMN_PRECISION;
		}
	}

	@Override
	public int getColumnScale() {
		final Selectable selectable = getColumn();
		if ( selectable instanceof Column ) {
			final Integer scale = ( (Column) selectable ).getScale();
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
		if ( sourceValue instanceof BasicValue ) {
			final BasicValue basicValue = (BasicValue) sourceValue;
			resolution = basicValue.resolution;
			implicitJavaTypeAccess =
					typeConfiguration -> basicValue.implicitJavaTypeAccess.apply( typeConfiguration );
		}
	}

	private void checkSelectable(Selectable incomingColumn) {
		if ( incomingColumn == null ) {
			throw new IllegalArgumentException( "Incoming column was null" );
		}

		final Selectable column = getColumn();
		if ( column == incomingColumn || column.getText().equals( incomingColumn.getText() ) ) {
			log.debugf( "Skipping column re-registration: %s.%s", getTable().getName(), column.getText() );
		}
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
		assert getResolution() != null;

		return getResolution().getLegacyResolvedBasicType();
	}

	public Resolution<?> getResolution() {
		return resolution;
	}

	@Override
	public boolean resolve(MetadataBuildingContext buildingContext) {
		resolve();
		return true;
	}

	@Override
	public Resolution<?> resolve() {
		if ( resolution != null ) {
			return resolution;
		}

		resolution = buildResolution();

		if ( resolution == null ) {
			throw new IllegalStateException( "Unable to resolve BasicValue : " + this );
		}

		final Selectable selectable = getColumn();
		final Size size;
		if ( selectable instanceof Column ) {
			Column column = (Column) selectable;
			resolveColumn( column, getDialect() );
			size = column.calculateColumnSize( getDialect(), getBuildingContext().getMetadataCollector() );
		}
		else {
			size = Size.nil();
		}

		resolution.getJdbcType()
				.addAuxiliaryDatabaseObjects(
						resolution.getRelationalJavaType(),
						size,
						getBuildingContext().getMetadataCollector().getDatabase(),
						this
				);

		return resolution;
	}

	@Override
	public String getExtraCreateTableInfo() {
		return resolution.getJdbcType()
				.getExtraCreateTableInfo(
						resolution.getRelationalJavaType(),
						getColumn().getText(),
						getTable().getName(),
						getBuildingContext().getMetadataCollector().getDatabase()
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
			final String checkCondition = resolution.getLegacyResolvedBasicType()
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
		if ( aggregateColumn != null ) {
			return aggregateColumn.getSelectablePath().append( selectableName );
		}
		return new SelectablePath( selectableName );
	}

	protected Resolution<?> buildResolution() {
		final Properties typeParameters = getTypeParameters();
		if ( typeParameters != null
				&& parseBoolean( typeParameters.getProperty(DynamicParameterizedType.IS_DYNAMIC) )
				&& typeParameters.get(DynamicParameterizedType.PARAMETER_TYPE) == null ) {
			createParameterImpl();
		}

		if ( explicitTypeName != null ) {
			return interpretExplicitlyNamedType(
					explicitTypeName,
					explicitJavaTypeAccess,
					explicitJdbcTypeAccess,
					explicitMutabilityPlanAccess,
					getAttributeConverterDescriptor(),
					typeParameters,
					this::setTypeParameters,
					this,
					getBuildingContext()
			);
		}
		else if ( isVersion() ) {
			return VersionResolution.from( implicitJavaTypeAccess, timeZoneStorageType, getBuildingContext() );
		}
		else {
			// determine JavaType if we can
			final BasicJavaType<?> explicitJavaType = getExplicitJavaType();
			final JavaType<?> javaType = determineJavaType( explicitJavaType );
			final ConverterDescriptor converterDescriptor = getConverterDescriptor( javaType );
			return converterDescriptor != null
					? converterResolution( javaType, converterDescriptor )
					: resolution( explicitJavaType, javaType );
		}
	}

	private BasicJavaType<?> getExplicitJavaType() {
		return explicitJavaTypeAccess == null ? null
				: explicitJavaTypeAccess.apply( getTypeConfiguration() );
	}

	private ConverterDescriptor getConverterDescriptor(JavaType<?> javaType) {
		final ConverterDescriptor converterDescriptor = getAttributeConverterDescriptor();
		if ( isSoftDelete() ) {
			assert converterDescriptor != null;
			final ConverterDescriptor softDeleteConverterDescriptor =
					getSoftDeleteConverterDescriptor( converterDescriptor, javaType);
			return getSoftDeleteStrategy() == SoftDeleteType.ACTIVE
					? new ReversedConverterDescriptor<>( softDeleteConverterDescriptor )
					: softDeleteConverterDescriptor;
		}
		else {
			return converterDescriptor;
		}
	}

	private ConverterDescriptor getSoftDeleteConverterDescriptor(
			ConverterDescriptor attributeConverterDescriptor, JavaType<?> javaType) {
		final boolean conversionWasUnspecified =
				SoftDelete.UnspecifiedConversion.class.equals( attributeConverterDescriptor.getAttributeConverterClass() );
		if ( conversionWasUnspecified ) {
			final JdbcType jdbcType = BooleanJdbcType.INSTANCE.resolveIndicatedType( this, javaType);
			if ( jdbcType.isNumber() ) {
				return new InstanceBasedConverterDescriptor(
						NumericBooleanConverter.INSTANCE,
						getBuildingContext().getBootstrapContext().getClassmateContext()
				);
			}
			else if ( jdbcType.isString() ) {
				// here we pick 'T' / 'F' storage, though 'Y' / 'N' is equally valid - its 50/50
				return new InstanceBasedConverterDescriptor(
						TrueFalseConverter.INSTANCE,
						getBuildingContext().getBootstrapContext().getClassmateContext()
				);
			}
			else {
				// should indicate BIT or BOOLEAN == no conversion needed
				//		- we still create the converter to properly set up JDBC type, etc
				return new InstanceBasedConverterDescriptor(
						PassThruSoftDeleteConverter.INSTANCE,
						getBuildingContext().getBootstrapContext().getClassmateContext()
				);
			}
		}
		else {
			return attributeConverterDescriptor;
		}
	}

	private static class ReversedConverterDescriptor<R> implements ConverterDescriptor {
		private final ConverterDescriptor underlyingDescriptor;

		public ReversedConverterDescriptor(ConverterDescriptor underlyingDescriptor) {
			this.underlyingDescriptor = underlyingDescriptor;
		}

		@Override
		public Class<? extends AttributeConverter<Boolean,R>> getAttributeConverterClass() {
			//noinspection unchecked
			return (Class<? extends AttributeConverter<Boolean, R>>) getClass();
		}

		@Override
		public ResolvedType getDomainValueResolvedType() {
			return underlyingDescriptor.getDomainValueResolvedType();
		}

		@Override
		public ResolvedType getRelationalValueResolvedType() {
			return underlyingDescriptor.getRelationalValueResolvedType();
		}

		@Override
		public AutoApplicableConverterDescriptor getAutoApplyDescriptor() {
			return AutoApplicableConverterDescriptorBypassedImpl.INSTANCE;
		}

		@Override
		public JpaAttributeConverter<Boolean,R> createJpaAttributeConverter(JpaAttributeConverterCreationContext context) {
			//noinspection unchecked
			return new ReversedJpaAttributeConverter<>(
					(JpaAttributeConverter<Boolean, R>) underlyingDescriptor.createJpaAttributeConverter( context ),
					context.getJavaTypeRegistry().getDescriptor( ReversedJpaAttributeConverter.class )
			);
		}
	}

	private static class ReversedJpaAttributeConverter<R, B extends AttributeConverter<Boolean, R>>
			implements JpaAttributeConverter<Boolean,R>, AttributeConverter<Boolean,R>, ManagedBean<B> {
		private final JpaAttributeConverter<Boolean,R> underlyingJpaConverter;
		private final JavaType<ReversedJpaAttributeConverter<R,B>> converterJavaType;

		public ReversedJpaAttributeConverter(
				JpaAttributeConverter<Boolean, R> underlyingJpaConverter,
				JavaType<ReversedJpaAttributeConverter<R,B>> converterJavaType) {
			this.underlyingJpaConverter = underlyingJpaConverter;
			this.converterJavaType = converterJavaType;
		}

		@Override
		public Boolean toDomainValue(R relationalValue) {
			return !underlyingJpaConverter.toDomainValue( relationalValue );
		}

		@Override
		public R toRelationalValue(Boolean domainValue) {
			return underlyingJpaConverter.toRelationalValue( domainValue != null ? !domainValue : null );
		}

		@Override
		public Boolean convertToEntityAttribute(R relationalValue) {
			return toDomainValue( relationalValue );
		}

		@Override
		public R convertToDatabaseColumn(Boolean domainValue) {
			return toRelationalValue( domainValue );
		}

		@Override
		public JavaType<Boolean> getDomainJavaType() {
			return underlyingJpaConverter.getDomainJavaType();
		}

		@Override
		public JavaType<R> getRelationalJavaType() {
			return underlyingJpaConverter.getRelationalJavaType();
		}

		@Override
		public JavaType<? extends AttributeConverter<Boolean, R>> getConverterJavaType() {
			return converterJavaType;
		}

		@Override
		public ManagedBean<? extends AttributeConverter<Boolean, R>> getConverterBean() {
			return this;
		}

		@Override
		public Class<B> getBeanClass() {
			//noinspection unchecked
			return (Class<B>) getClass();
		}

		@Override
		public B getBeanInstance() {
			//noinspection unchecked
			return (B) this;
		}
	}

	private static class PassThruSoftDeleteConverter implements AttributeConverter<Boolean,Boolean> {
		private static final PassThruSoftDeleteConverter INSTANCE = new PassThruSoftDeleteConverter();

		@Override
		public Boolean convertToDatabaseColumn(Boolean domainValue) {
			return domainValue;
		}

		@Override
		public Boolean convertToEntityAttribute(Boolean relationalValue) {
			return relationalValue;
		}
	}

	private Resolution<?> resolution(BasicJavaType explicitJavaType, JavaType<?> javaType) {
		final JavaType<?> basicJavaType;
		final JdbcType jdbcType;
		if ( explicitJdbcTypeAccess != null ) {
			final TypeConfiguration typeConfiguration = getTypeConfiguration();
			jdbcType = explicitJdbcTypeAccess.apply( typeConfiguration );
			basicJavaType = javaType == null && jdbcType != null
					? jdbcType.getJdbcRecommendedJavaTypeMapping(null, null, typeConfiguration)
					: javaType;
		}
		else {
			jdbcType = null;
			basicJavaType = javaType;
		}
		if ( basicJavaType == null ) {
			throw new MappingException( "Unable to determine JavaType to use : " + this );
		}

		if ( basicJavaType instanceof BasicJavaType<?>
				&& ( !basicJavaType.getJavaTypeClass().isEnum() || enumerationStyle == null ) ) {
			final TypeDefinition autoAppliedTypeDef =
					getBuildingContext().getTypeDefinitionRegistry()
							.resolveAutoApplied( (BasicJavaType<?>) basicJavaType );
			if ( autoAppliedTypeDef != null ) {
				log.debug("BasicValue resolution matched auto-applied type-definition");
				return autoAppliedTypeDef.resolve( getTypeParameters(), null, getBuildingContext(), this );
			}
		}

		return InferredBasicValueResolver.from(
				explicitJavaType,
				jdbcType,
				resolvedJavaType,
				this::determineReflectedJavaType,
				explicitMutabilityPlanAccess,
				this,
				getTable(),
				getColumn(),
				ownerName,
				propertyName,
				getBuildingContext()
		);
	}

	@Override
	public ManagedBeanRegistry getManagedBeanRegistry() {
		return getServiceRegistry().requireService( ManagedBeanRegistry.class );
	}

	private Resolution<?> converterResolution(JavaType<?> javaType, ConverterDescriptor attributeConverterDescriptor) {
		final NamedConverterResolution<?> converterResolution = NamedConverterResolution.from(
				attributeConverterDescriptor,
				explicitJavaTypeAccess,
				explicitJdbcTypeAccess,
				explicitMutabilityPlanAccess,
				resolvedJavaType,
				this,
				this,
				getBuildingContext()
		);

		if ( javaType instanceof BasicPluralJavaType<?>
				&& !attributeConverterDescriptor.getDomainValueResolvedType().getErasedType()
						.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
			// In this case, the converter applies to the element of a BasicPluralJavaType
			final BasicType registeredElementType = converterResolution.getLegacyResolvedBasicType();
			final Selectable column = getColumn();
			final BasicType<?> registeredType = registeredElementType == null ? null
					: ( (BasicPluralJavaType<?>) javaType ).resolveType(
							getTypeConfiguration(),
							getDialect(),
							registeredElementType,
							column instanceof ColumnTypeInformation ? (ColumnTypeInformation) column : null,
							this
			);
			if ( registeredType != null ) {
				getTypeConfiguration().getBasicTypeRegistry().register( registeredType );
				return new InferredBasicValueResolution(
						registeredType,
						registeredType.getJavaTypeDescriptor(),
						registeredType.getJavaTypeDescriptor(),
						registeredType.getJdbcType(),
						registeredType,
						null
				);
			}
		}

		return converterResolution;
	}

	private JavaType<?> determineJavaType(JavaType<?> explicitJavaType) {
		JavaType<?> javaType = explicitJavaType;
//
//		if ( javaType == null ) {
//			if ( implicitJavaTypeAccess != null ) {
//				final java.lang.reflect.Type implicitJtd = implicitJavaTypeAccess.apply( getTypeConfiguration() );
//				if ( implicitJtd != null ) {
//					javaType = getTypeConfiguration().getJavaTypeRegistry().getDescriptor( implicitJtd );
//				}
//			}
//		}

		if ( javaType == null ) {
			final JavaType<?> reflectedJtd = determineReflectedJavaType();
			if ( reflectedJtd != null ) {
				javaType = reflectedJtd;
			}
		}

		return javaType;
	}

	private JavaType<?> determineReflectedJavaType() {
		final TypeConfiguration typeConfiguration = getTypeConfiguration();
		final java.lang.reflect.Type impliedJavaType = impliedJavaType( typeConfiguration );
		if ( impliedJavaType == null ) {
			return null;
		}
		else {
			resolvedJavaType = impliedJavaType;
			return javaType( typeConfiguration, impliedJavaType );
		}
	}

	private java.lang.reflect.Type impliedJavaType(TypeConfiguration typeConfiguration) {
		if ( resolvedJavaType != null ) {
			return resolvedJavaType;
		}
		else if ( implicitJavaTypeAccess != null ) {
			return implicitJavaTypeAccess.apply(typeConfiguration);
		}
		else if ( ownerName != null && propertyName != null ) {
			return reflectedPropertyType( ownerName, propertyName,
					getServiceRegistry().requireService( ClassLoaderService.class ) );
		}
		else {
			return null;
		}
	}

	private JavaType<Object> javaType(TypeConfiguration typeConfiguration, java.lang.reflect.Type impliedJavaType) {
		final JavaType<Object> javaType = typeConfiguration.getJavaTypeRegistry().findDescriptor( impliedJavaType );
		return javaType == null ? specialJavaType( typeConfiguration, impliedJavaType ) : javaType;
	}

	private JavaType<Object> specialJavaType(
			TypeConfiguration typeConfiguration,
			java.lang.reflect.Type impliedJavaType) {
		final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
		if ( jdbcTypeCode != null ) {
			// Construct special JavaType instances for JSON/XML types which can report recommended JDBC types
			// and implement toString/fromString as well as copying based on FormatMapper operations
			switch ( jdbcTypeCode ) {
				case SqlTypes.JSON:
					final JavaType<Object> jsonJavaType =
							new JsonJavaType<>( impliedJavaType,
									mutabilityPlan( typeConfiguration, impliedJavaType ),
									typeConfiguration );
					javaTypeRegistry.addDescriptor( jsonJavaType );
					return jsonJavaType;
				case SqlTypes.SQLXML:
					final JavaType<Object> xmlJavaType =
							new XmlJavaType<>( impliedJavaType,
									mutabilityPlan( typeConfiguration, impliedJavaType ),
									typeConfiguration );
					javaTypeRegistry.addDescriptor( xmlJavaType );
					return xmlJavaType;
			}
		}
		return javaTypeRegistry.resolveDescriptor( impliedJavaType );
	}

	private MutabilityPlan<Object> mutabilityPlan(
			TypeConfiguration typeConfiguration, java.lang.reflect.Type impliedJavaType) {
		final MutabilityPlan<Object> explicitMutabilityPlan = getExplicitMutabilityPlan();
		return explicitMutabilityPlan != null
				? explicitMutabilityPlan
				: RegistryHelper.INSTANCE.determineMutabilityPlan( impliedJavaType, typeConfiguration );
	}

	private MutabilityPlan<Object> getExplicitMutabilityPlan() {
		return explicitMutabilityPlanAccess == null ? null
				: explicitMutabilityPlanAccess.apply( getTypeConfiguration()  );
	}

	private static Resolution<?> interpretExplicitlyNamedType(
			String name,
			Function<TypeConfiguration, BasicJavaType> explicitJtdAccess,
			Function<TypeConfiguration, JdbcType> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			ConverterDescriptor converterDescriptor,
			Map<Object,Object> localTypeParams,
			Consumer<Properties> combinedParameterConsumer,
			JdbcTypeIndicators stdIndicators,
			MetadataBuildingContext context) {

		final StandardServiceRegistry serviceRegistry = context.getBootstrapContext().getServiceRegistry();
		final ManagedBeanRegistry managedBeanRegistry = serviceRegistry.requireService( ManagedBeanRegistry.class );
		final TypeConfiguration typeConfiguration = context.getBootstrapContext().getTypeConfiguration();

		final JpaAttributeConverterCreationContext converterCreationContext = new JpaAttributeConverterCreationContext() {
			@Override
			public ManagedBeanRegistry getManagedBeanRegistry() {
				return managedBeanRegistry;
			}

			@Override
			public TypeConfiguration getTypeConfiguration() {
				return typeConfiguration;
			}
		};

		// Name could refer to:
		//		1) a named converter - HBM support for JPA's AttributeConverter via its `type="..."` XML attribute
		//		2) a "named composed" mapping - like (1), this is mainly to support envers since it tells
		//			Hibernate the mappings via DOM.  See `org.hibernate.type.internal.BasicTypeImpl`
		//		3) basic type "resolution key"
		//		4) UserType or BasicType class name - directly, or through a TypeDefinition

		if ( name.startsWith( ConverterDescriptor.TYPE_NAME_PREFIX  ) ) {
			return NamedConverterResolution.from(
					name,
					explicitJtdAccess,
					explicitStdAccess,
					explicitMutabilityPlanAccess,
					stdIndicators,
					converterCreationContext,
					context
			);
		}

//		if ( name.startsWith( EnumeratedValueResolution.PREFIX ) ) {
//			return EnumeratedValueResolution.fromName( name, stdIndicators, context );
//		}

		if ( name.startsWith( BasicTypeImpl.EXTERNALIZED_PREFIX ) ) {
			final BasicType<Object> basicType = context.getBootstrapContext().resolveAdHocBasicType( name );
			return new NamedBasicTypeResolution<>(
					basicType.getJavaTypeDescriptor(),
					basicType,
					null,
					explicitMutabilityPlanAccess,
					context
			);
		}

		// see if it is a named basic type
		final BasicType<?> basicTypeByName = typeConfiguration.getBasicTypeRegistry().getRegisteredType( name );
		if ( basicTypeByName != null ) {
			final BasicValueConverter<?,?> valueConverter;
			final JavaType<?> domainJtd;
			if ( converterDescriptor != null ) {
				valueConverter = converterDescriptor.createJpaAttributeConverter( converterCreationContext );
				domainJtd = valueConverter.getDomainJavaType();
			}
			else {
				valueConverter = basicTypeByName.getValueConverter();
				domainJtd = basicTypeByName.getJavaTypeDescriptor();
			}

			return new NamedBasicTypeResolution<>(
					domainJtd,
					basicTypeByName,
					valueConverter,
					explicitMutabilityPlanAccess,
					context
			);
		}

		// see if it is a named TypeDefinition
		final TypeDefinition typeDefinition = context.getTypeDefinitionRegistry().resolve( name );
		if ( typeDefinition != null ) {
			final Resolution<?> resolution = typeDefinition.resolve(
					localTypeParams,
					explicitMutabilityPlanAccess != null
							? explicitMutabilityPlanAccess.apply( typeConfiguration )
							: null,
					context,
					stdIndicators
			);
			combinedParameterConsumer.accept( resolution.getCombinedTypeParameters() );
			return resolution;
		}


		// see if the name is a UserType or BasicType implementor class name
		final ClassLoaderService cls = serviceRegistry.requireService( ClassLoaderService.class );
		try {
			final Class<?> typeNamedClass = cls.classForName( name );

			// if there are no local config params, register an implicit TypeDefinition for this custom type .
			//  later uses may find it and re-use its cacheable reference...
			if ( CollectionHelper.isEmpty( localTypeParams ) ) {
				final TypeDefinition implicitDefinition = new TypeDefinition(
						name,
						typeNamedClass,
						null,
						null
				);
				context.getTypeDefinitionRegistry().register( implicitDefinition );
				return implicitDefinition.resolve(
						localTypeParams,
						explicitMutabilityPlanAccess != null
								? explicitMutabilityPlanAccess.apply( typeConfiguration )
								: null,
						context,
						stdIndicators
				);
			}

			return TypeDefinition.createLocalResolution( name, typeNamedClass, localTypeParams, context );
		}
		catch (ClassLoadingException e) {
			// allow the exception below to trigger
			log.debugf( "Could not resolve type-name [%s] as Java type : %s", name, e );
		}

		throw new MappingException( "Could not resolve named type : " + name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlTypeDescriptorIndicators

	@Override
	public EnumType getEnumeratedType() {
		return getEnumerationStyle();
	}


	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return resolveJdbcTypeCode( getBuildingContext().getPreferredSqlTypeCodeForBoolean() );
	}

	@Override
	public int getPreferredSqlTypeCodeForDuration() {
		return resolveJdbcTypeCode( getBuildingContext().getPreferredSqlTypeCodeForDuration() );
	}

	@Override
	public int getPreferredSqlTypeCodeForUuid() {
		return resolveJdbcTypeCode( getBuildingContext().getPreferredSqlTypeCodeForUuid() );
	}

	@Override
	public int getPreferredSqlTypeCodeForInstant() {
		return resolveJdbcTypeCode( getBuildingContext().getPreferredSqlTypeCodeForInstant() );
	}

	@Override
	public int getPreferredSqlTypeCodeForArray() {
		return resolveJdbcTypeCode( getBuildingContext().getPreferredSqlTypeCodeForArray() );
	}

	@Override
	public int resolveJdbcTypeCode(int jdbcTypeCode) {
		return aggregateColumn == null
				? jdbcTypeCode
				: getDialect().getAggregateSupport()
				.aggregateComponentSqlTypeCode( aggregateColumn.getSqlTypeCode( getMetadata() ), jdbcTypeCode );
	}

	@Override
	public TimeZoneStorageStrategy getDefaultTimeZoneStorageStrategy() {
		return timeZoneStorageStrategy( timeZoneStorageType, getBuildingContext() );
	}

	@Internal
	public static TimeZoneStorageStrategy timeZoneStorageStrategy(
			TimeZoneStorageType timeZoneStorageType,
			MetadataBuildingContext buildingContext) {
		if ( timeZoneStorageType != null ) {
			switch ( timeZoneStorageType ) {
				case COLUMN:
					return TimeZoneStorageStrategy.COLUMN;
				case NATIVE:
					return TimeZoneStorageStrategy.NATIVE;
				case NORMALIZE:
					return TimeZoneStorageStrategy.NORMALIZE;
				case NORMALIZE_UTC:
					return TimeZoneStorageStrategy.NORMALIZE_UTC;
			}
		}
		return buildingContext.getBuildingOptions().getDefaultTimeZoneStorage();
	}

	public void setExplicitTypeParams(Map<String,String> explicitLocalTypeParams) {
		this.explicitLocalTypeParams = explicitLocalTypeParams;
	}

	public void setExplicitTypeName(String typeName) {
		this.explicitTypeName = typeName;
	}

	public void setTypeName(String typeName) {
		if ( StringHelper.isNotEmpty( typeName ) ) {
			if ( typeName.startsWith( ConverterDescriptor.TYPE_NAME_PREFIX ) ) {
				final String converterClassName = typeName.substring( ConverterDescriptor.TYPE_NAME_PREFIX.length() );
				final ClassLoaderService cls = getServiceRegistry().requireService( ClassLoaderService.class );
				try {
					final Class<AttributeConverter<?,?>> converterClass = cls.classForName( converterClassName );
					setAttributeConverterDescriptor( new ClassBasedConverterDescriptor(
							converterClass,
							false,
							getBuildingContext().getBootstrapContext().getClassmateContext()
					) );
					return;
				}
				catch (Exception e) {
					log.logBadHbmAttributeConverterType( typeName, e.getMessage() );
				}
			}
			else {
				setExplicitTypeName( typeName );
			}
		}

		super.setTypeName( typeName );
	}

	private static int COUNTER;

	public void setExplicitCustomType(Class<? extends UserType<?>> explicitCustomType) {
		if ( explicitCustomType != null ) {
			if ( resolution != null ) {
				throw new UnsupportedOperationException( "Unsupported attempt to set an explicit-custom-type when value is already resolved" );
			}
			else {
				resolution = new UserTypeResolution<>(
						new CustomType<>(
								getConfiguredUserTypeBean( explicitCustomType, getCustomTypeProperties() ),
								getTypeConfiguration()
						),
						null,
						getCustomTypeProperties()
				);
			}
		}
	}

	private Properties getCustomTypeProperties() {
		final Properties properties = new Properties();
		if ( isNotEmpty( getTypeParameters() ) ) {
			properties.putAll( getTypeParameters() );
		}
		if ( isNotEmpty( explicitLocalTypeParams ) ) {
			properties.putAll( explicitLocalTypeParams );
		}
		return properties;
	}

	private UserType<?> getConfiguredUserTypeBean(Class<? extends UserType<?>> explicitCustomType, Properties properties) {
		final UserType<?> typeInstance =
				!getBuildingContext().getBuildingOptions().isAllowExtensionsInCdi()
						? FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( explicitCustomType )
						: getUserTypeBean( explicitCustomType, properties ).getBeanInstance();

		if ( typeInstance instanceof TypeConfigurationAware ) {
			( (TypeConfigurationAware) typeInstance ).setTypeConfiguration( getTypeConfiguration() );
		}

		if ( typeInstance instanceof DynamicParameterizedType ) {
			if ( parseBoolean( properties.getProperty( DynamicParameterizedType.IS_DYNAMIC ) ) ) {
				if ( properties.get( DynamicParameterizedType.PARAMETER_TYPE ) == null ) {
					properties.put( DynamicParameterizedType.PARAMETER_TYPE, makeParameterImpl() );
				}
			}
		}

		injectParameters( typeInstance, properties);
		// envers - grr
		setTypeParameters( properties );

		return typeInstance;
	}

	private <T> ManagedBean<T> getUserTypeBean(Class<T> explicitCustomType, Properties properties) {
		final BeanInstanceProducer producer = getBuildingContext().getBootstrapContext().getCustomTypeProducer();
		final ManagedBeanRegistry registry = getServiceRegistry().requireService( ManagedBeanRegistry.class );
		if ( isNotEmpty( properties ) ) {
			final String name = explicitCustomType.getName() + COUNTER++;
			return registry.getBean( name, explicitCustomType, producer );
		}
		else {
			return registry.getBean( explicitCustomType, producer );
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
		return getBuildingContext().isPreferJavaTimeJdbcTypesEnabled();
	}

	@Override
	public boolean isPreferNativeEnumTypesEnabled() {
		return getBuildingContext().isPreferNativeEnumTypesEnabled();
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Internal
	public boolean isDisallowedWrapperArray() {
		return getBuildingContext().getBuildingOptions().getWrapperArrayHandling() == WrapperArrayHandling.DISALLOW
			&& !isLob()
			&& ( explicitJavaTypeAccess == null || explicitJavaTypeAccess.apply( getTypeConfiguration() ) == null )
			&& isWrapperByteOrCharacterArray();
	}

	private boolean isWrapperByteOrCharacterArray() {
		final Class<?> javaTypeClass = getResolution().getDomainJavaType().getJavaTypeClass();
		return javaTypeClass == Byte[].class || javaTypeClass == Character[].class;
	}

	@Incubating
	public void setExplicitJdbcTypeCode(Integer jdbcTypeCode) {
		this.jdbcTypeCode = jdbcTypeCode;
	}

	@Override
	public Integer getExplicitJdbcTypeCode() {
		return jdbcTypeCode == null ? getPreferredSqlTypeCodeForArray() : jdbcTypeCode;
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
