/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import javax.persistence.AttributeConverter;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.MappingException;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.TypeDefinitionRegistry;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.process.internal.ConvertedBasicTypeResolution;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolver;
import org.hibernate.boot.model.process.internal.NamedBasicTypeResolution;
import org.hibernate.boot.model.process.internal.NamedConverterResolution;
import org.hibernate.boot.model.process.internal.VersionResolution;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.TemporalJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BasicValue extends SimpleValue implements JdbcTypeDescriptorIndicators, Resolvable {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( BasicValue.class );

	private final TypeConfiguration typeConfiguration;
	private final int preferredJdbcTypeCodeForBoolean;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// incoming "configuration" values

	private String explicitTypeName;
	private Map explicitLocalTypeParams;

	private Function<TypeConfiguration, BasicJavaDescriptor> explicitJavaTypeAccess;
	private Function<TypeConfiguration, JdbcTypeDescriptor> explicitSqlTypeAccess;
	private Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess;
	private Function<TypeConfiguration, java.lang.reflect.Type> implicitJavaTypeAccess;

	private EnumType enumerationStyle;
	private TemporalType temporalPrecision;

	private java.lang.reflect.Type resolvedJavaType;

	private String ownerName;
	private String propertyName;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Resolved state - available after `#resolve`
	private Resolution<?> resolution;


	public BasicValue(MetadataBuildingContext buildingContext) {
		this( buildingContext, null );
	}

	public BasicValue(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );

		this.typeConfiguration = buildingContext.getBootstrapContext().getTypeConfiguration();
		this.preferredJdbcTypeCodeForBoolean = buildingContext.getPreferredSqlTypeCodeForBoolean();

		buildingContext.getMetadataCollector().registerValueMappingResolver( this::resolve );
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

	@SuppressWarnings("WeakerAccess")
	public EnumType getEnumerationStyle() {
		return enumerationStyle;
	}


	public void setJpaAttributeConverterDescriptor(ConverterDescriptor descriptor) {
		setAttributeConverterDescriptor( descriptor );

		super.setJpaAttributeConverterDescriptor( descriptor );
	}

	@SuppressWarnings({"rawtypes"})
	public void setExplicitJavaTypeAccess(Function<TypeConfiguration, BasicJavaDescriptor> explicitJavaTypeAccess) {
		this.explicitJavaTypeAccess = explicitJavaTypeAccess;
	}

	public void setExplicitSqlTypeAccess(Function<TypeConfiguration, JdbcTypeDescriptor> sqlTypeAccess) {
		this.explicitSqlTypeAccess = sqlTypeAccess;
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
		final Selectable column = getColumn();
		if ( column != null && column instanceof Column ) {
			final Long length = ( (Column) column ).getLength();
			return length == null ? NO_COLUMN_LENGTH : length;
		}
		else {
			return NO_COLUMN_LENGTH;
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
			this.resolution = basicValue.resolution;
			this.implicitJavaTypeAccess = (typeConfiguration) -> basicValue.implicitJavaTypeAccess.apply( typeConfiguration );
		}
	}

	private void checkSelectable(Selectable incomingColumn) {
		if ( incomingColumn == null ) {
			throw new IllegalArgumentException( "Incoming column was null" );
		}

		final Selectable column = getColumn();
		if ( column == incomingColumn || column.getText().equals( incomingColumn.getText() ) ) {
			log.debugf( "Skipping column re-registration: %s.%s", getTable().getName(), column.getText() );
			return;
		}

		if ( column != null ) {
			throw new IllegalStateException(
					"BasicValue [" + ownerName + "." + propertyName +
							"] already had column associated: `" + column.getText() +
							"` -> `" + incomingColumn.getText() + "`"
			);
		}
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

		final Selectable column = getColumn();
		if ( column instanceof Column && resolution.getValueConverter() == null ) {
			final Column physicalColumn = (Column) column;
			if ( physicalColumn.getSqlTypeCode() == null ) {
				physicalColumn.setSqlTypeCode( resolution.getJdbcTypeDescriptor().getDefaultSqlTypeCode() );
			}

			final BasicType<?> basicType = resolution.getLegacyResolvedBasicType();
			final Dialect dialect = getServiceRegistry().getService( JdbcServices.class ).getDialect();
			final String checkConstraint = physicalColumn.getCheckConstraint();
			if ( checkConstraint == null && dialect.supportsColumnCheck() ) {
				physicalColumn.setCheckConstraint(
						basicType.getJavaTypeDescriptor().getCheckCondition(
								physicalColumn.getQuotedName( dialect ),
								basicType.getJdbcTypeDescriptor(),
								dialect
						)
				);
			}
		}

		return resolution;
	}

	protected Resolution<?> buildResolution() {
		Properties typeParameters = getTypeParameters();
		if ( typeParameters != null
				&& Boolean.parseBoolean( typeParameters.getProperty( DynamicParameterizedType.IS_DYNAMIC ) )
				&& typeParameters.get( DynamicParameterizedType.PARAMETER_TYPE ) == null ) {
			createParameterImpl();
		}
		if ( explicitTypeName != null ) {
			return interpretExplicitlyNamedType(
					explicitTypeName,
					enumerationStyle,
					implicitJavaTypeAccess,
					explicitJavaTypeAccess,
					explicitSqlTypeAccess,
					explicitMutabilityPlanAccess,
					getAttributeConverterDescriptor(),
					typeParameters,
					this,
					typeConfiguration,
					getBuildingContext()
			);
		}


		if ( isVersion() ) {
			return VersionResolution.from(
					implicitJavaTypeAccess,
					explicitJavaTypeAccess,
					explicitSqlTypeAccess,
					typeConfiguration,
					getBuildingContext()
			);
		}


		final ConverterDescriptor attributeConverterDescriptor = getAttributeConverterDescriptor();
		if ( attributeConverterDescriptor != null ) {
			final ManagedBeanRegistry managedBeanRegistry = getBuildingContext().getBootstrapContext()
					.getServiceRegistry()
					.getService( ManagedBeanRegistry.class );

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

			return NamedConverterResolution.from(
					attributeConverterDescriptor,
					explicitJavaTypeAccess,
					explicitSqlTypeAccess,
					explicitMutabilityPlanAccess,
					this,
					converterCreationContext,
					getBuildingContext()
			);
		}

		JavaTypeDescriptor jtd = null;

		// determine JTD if we can

		if ( explicitJavaTypeAccess != null ) {
			final BasicJavaDescriptor explicitJtd = explicitJavaTypeAccess.apply( typeConfiguration );
			if ( explicitJtd != null ) {
				jtd = explicitJtd;
			}
		}

		if ( jtd == null ) {
			if ( implicitJavaTypeAccess != null ) {
				final java.lang.reflect.Type implicitJtd = implicitJavaTypeAccess.apply( typeConfiguration );
				if ( implicitJtd != null ) {
					jtd = typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( implicitJtd );
				}
			}
		}

		if ( jtd == null ) {
			final JavaTypeDescriptor reflectedJtd = determineReflectedJavaTypeDescriptor();
			if ( reflectedJtd != null ) {
				jtd = reflectedJtd;
			}
		}

		final TypeDefinitionRegistry typeDefinitionRegistry = getBuildingContext().getTypeDefinitionRegistry();
		final TypeDefinition autoAppliedTypeDef = typeDefinitionRegistry.resolveAutoApplied( (BasicJavaDescriptor<?>) jtd );
		if ( autoAppliedTypeDef != null && ( !jtd.getJavaTypeClass().isEnum() || enumerationStyle == null ) ) {
			log.debug( "BasicValue resolution matched auto-applied type-definition" );
			return autoAppliedTypeDef.resolve(
					typeParameters,
					null,
					getBuildingContext(),
					this
			);
		}

		return InferredBasicValueResolver.from(
				explicitJavaTypeAccess,
				explicitSqlTypeAccess,
				resolvedJavaType,
				this::determineReflectedJavaTypeDescriptor,
				this,
				getTable(),
				getColumn(),
				ownerName,
				propertyName,
				typeConfiguration
		);

	}

	private JavaTypeDescriptor determineReflectedJavaTypeDescriptor() {
		final java.lang.reflect.Type impliedJavaType;

		if ( resolvedJavaType != null ) {
			impliedJavaType = resolvedJavaType;
		}
		else if ( implicitJavaTypeAccess != null ) {
			impliedJavaType = implicitJavaTypeAccess.apply( typeConfiguration );
		}
		else if ( ownerName != null && propertyName != null ) {
			final ServiceRegistry serviceRegistry = typeConfiguration.getServiceRegistry();
			final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

			impliedJavaType = ReflectHelper.reflectedPropertyType(
					ownerName,
					propertyName,
					classLoaderService
			);
		}
		else {
			return null;
		}

		resolvedJavaType = impliedJavaType;
		return typeConfiguration.getJavaTypeDescriptorRegistry().resolveDescriptor( impliedJavaType );
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Resolution interpretExplicitlyNamedType(
			String name,
			EnumType enumerationStyle,
			Function<TypeConfiguration, java.lang.reflect.Type> implicitJavaTypeAccess,
			Function<TypeConfiguration, BasicJavaDescriptor> explicitJtdAccess,
			Function<TypeConfiguration, JdbcTypeDescriptor> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			ConverterDescriptor converterDescriptor,
			Map localTypeParams,
			JdbcTypeDescriptorIndicators stdIndicators,
			TypeConfiguration typeConfiguration,
			MetadataBuildingContext context) {

		final ManagedBeanRegistry managedBeanRegistry = context.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

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
		//		2) basic type "resolution key"
		//		3) UserType or BasicType class name - directly, or through a TypeDefinition

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

		// see if it is a named basic type
		final BasicType basicTypeByName = typeConfiguration.getBasicTypeRegistry().getRegisteredType( name );
		if ( basicTypeByName != null ) {
			final BasicValueConverter valueConverter;
			final JavaTypeDescriptor<?> domainJtd;
			if ( converterDescriptor != null ) {
				valueConverter = converterDescriptor.createJpaAttributeConverter( converterCreationContext );
				domainJtd = valueConverter.getDomainJavaDescriptor();
			}
			else if ( basicTypeByName instanceof ConvertedBasicType ) {
				final ConvertedBasicType convertedType = (ConvertedBasicType) basicTypeByName;
				return new ConvertedBasicTypeResolution( convertedType, stdIndicators );
			}
			else {
				valueConverter = null;
				domainJtd = basicTypeByName.getJavaTypeDescriptor();
			}

			return new NamedBasicTypeResolution(
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
			return typeDefinition.resolve(
					localTypeParams,
					explicitMutabilityPlanAccess != null
							? explicitMutabilityPlanAccess.apply( typeConfiguration )
							: null,
					context,
					stdIndicators
			);
		}


		// see if the name is a UserType or BasicType implementor class name
		final ClassLoaderService cls = typeConfiguration.getServiceRegistry().getService( ClassLoaderService.class );
		try {
			final Class typeNamedClass = cls.classForName( name );

			// if there are no local config params, register an implicit TypeDefinition for this custom type .
			//  later uses may find it and re-use its cacheable reference...
			if ( CollectionHelper.isEmpty( localTypeParams ) ) {
				final TypeDefinition implicitDefinition = new TypeDefinition(
						name,
						typeNamedClass,
						null,
						null,
						typeConfiguration
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

			return TypeDefinition.createLocalResolution(
					name,
					typeNamedClass,
					explicitMutabilityPlanAccess != null
							? explicitMutabilityPlanAccess.apply( typeConfiguration )
							: null,
					localTypeParams,
					context
			);
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
		return preferredJdbcTypeCodeForBoolean;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public void setExplicitTypeParams(Map explicitLocalTypeParams) {
		this.explicitLocalTypeParams = explicitLocalTypeParams;
	}

	public void setExplicitTypeName(String typeName) {
		this.explicitTypeName = typeName;;
	}

	public void setTypeName(String typeName) {
		if ( StringHelper.isNotEmpty( typeName ) ) {
			if ( typeName.startsWith( ConverterDescriptor.TYPE_NAME_PREFIX ) ) {
				final String converterClassName = typeName.substring( ConverterDescriptor.TYPE_NAME_PREFIX.length() );
				final ClassLoaderService cls = getBuildingContext()
						.getMetadataCollector()
						.getMetadataBuildingOptions()
						.getServiceRegistry()
						.getService( ClassLoaderService.class );
				try {
					//noinspection rawtypes
					final Class<AttributeConverter> converterClass = cls.classForName( converterClassName );
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

	public void setTemporalPrecision(TemporalType temporalPrecision) {
		this.temporalPrecision = temporalPrecision;
	}

	@Override
	public TemporalType getTemporalPrecision() {
		return temporalPrecision;
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

		JdbcMapping getJdbcMapping();

		/**
		 * The JavaTypeDescriptor for the value as part of the domain model
		 */
		JavaTypeDescriptor<J> getDomainJavaDescriptor();

		/**
		 * The JavaTypeDescriptor for the relational value as part of
		 * the relational model (its JDBC representation)
		 */
		JavaTypeDescriptor<?> getRelationalJavaDescriptor();

		/**
		 * The JavaTypeDescriptor for the relational value as part of
		 * the relational model (its JDBC representation)
		 */
		JdbcTypeDescriptor getJdbcTypeDescriptor();

		/**
		 * Converter, if any, to convert values between the
		 * domain and relational JavaTypeDescriptor representations
		 */
		BasicValueConverter getValueConverter();

		/**
		 * The resolved MutabilityPlan
		 */
		MutabilityPlan<J> getMutabilityPlan();
	}
}
