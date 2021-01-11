/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import java.util.Map;
import java.util.function.Function;
import javax.persistence.AttributeConverter;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.MappingException;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
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
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.TemporalJavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BasicValue extends SimpleValue implements SqlTypeDescriptorIndicators, Resolvable {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( BasicValue.class );

	private final MetadataBuildingContext buildingContext;
	private final TypeConfiguration typeConfiguration;
	private final int preferredJdbcTypeCodeForBoolean;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// incoming "configuration" values

	private String explicitTypeName;
	private Map explicitLocalTypeParams;

	private Function<TypeConfiguration,BasicJavaDescriptor> explicitJavaTypeAccess;
	private Function<TypeConfiguration,SqlTypeDescriptor> explicitSqlTypeAccess;
	private Function<TypeConfiguration,MutabilityPlan> explicitMutabilityPlanAccess;
	private Function<TypeConfiguration,Class> implicitJavaTypeAccess;

	private boolean isNationalized;
	private boolean isLob;
	private EnumType enumerationStyle;
	private TemporalType temporalPrecision;

	private ConverterDescriptor attributeConverterDescriptor;

	private Class resolvedJavaClass;

	private String ownerName;
	private String propertyName;

	private Selectable column;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Resolved state - available after `#resolve`
	private Resolution<?> resolution;


	public BasicValue(MetadataBuildingContext buildingContext) {
		this( buildingContext, null );
	}

	public BasicValue(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );

		this.buildingContext = buildingContext;

		this.typeConfiguration = buildingContext.getBootstrapContext().getTypeConfiguration();
		this.preferredJdbcTypeCodeForBoolean = buildingContext.getPreferredSqlTypeCodeForBoolean();

		buildingContext.getMetadataCollector().registerValueMappingResolver( this::resolve );
	}

	public MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return buildingContext.getBootstrapContext().getServiceRegistry();
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

	public ConverterDescriptor getJpaAttributeConverterDescriptor() {
		return attributeConverterDescriptor;
	}

	public void setJpaAttributeConverterDescriptor(ConverterDescriptor descriptor) {
		this.attributeConverterDescriptor = descriptor;

		super.setJpaAttributeConverterDescriptor( descriptor );
	}

	@SuppressWarnings({"rawtypes"})
	public void setExplicitJavaTypeAccess(Function<TypeConfiguration, BasicJavaDescriptor> explicitJavaTypeAccess) {
		this.explicitJavaTypeAccess = explicitJavaTypeAccess;
	}

	public void setExplicitSqlTypeAccess(Function<TypeConfiguration, SqlTypeDescriptor> sqlTypeAccess) {
		this.explicitSqlTypeAccess = sqlTypeAccess;
	}

	public void setExplicitMutabilityPlanAccess(Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess) {
		this.explicitMutabilityPlanAccess = explicitMutabilityPlanAccess;
	}

	public void setImplicitJavaTypeAccess(Function<TypeConfiguration, Class> implicitJavaTypeAccess) {
		this.implicitJavaTypeAccess = implicitJavaTypeAccess;
	}

	public Selectable getColumn() {
		return column;
	}

	public Class getResolvedJavaClass() {
		return resolvedJavaClass;
	}

	@Override
	public long getColumnLength() {
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

		applySelectable( incomingColumn );
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

	private void applySelectable(Selectable incomingColumn) {
		if ( incomingColumn == null ) {
			throw new IllegalArgumentException( "Incoming column was null" );
		}

		if ( this.column == incomingColumn ) {
			log.debugf( "Skipping column re-registration: %s.%s", getTable().getName(), column.getText() );
			return;
		}

		if ( this.column != null ) {
			throw new IllegalStateException(
					"BasicValue [" + ownerName + "." + propertyName +
							"] already had column associated: `" + this.column.getText() +
							"` -> `" + incomingColumn.getText() + "`"
			);
		}

		this.column = incomingColumn;
	}

	@Override
	public void addColumn(Column incomingColumn, boolean isInsertable, boolean isUpdatable) {
		super.addColumn( incomingColumn, isInsertable, isUpdatable );

		applySelectable( incomingColumn );
	}

	@Override
	public void addFormula(Formula formula) {
		super.addFormula( formula );

		applySelectable( formula );
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

		if ( column instanceof Column && resolution.getValueConverter() == null ) {
			final Column physicalColumn = (Column) column;
			if ( physicalColumn.getSqlTypeCode() == null ) {
				physicalColumn.setSqlTypeCode( resolution.getRelationalSqlTypeDescriptor().getJdbcTypeCode() );
			}

			final BasicType<?> basicType = resolution.getLegacyResolvedBasicType();
			final Dialect dialect = getServiceRegistry().getService( JdbcServices.class ).getDialect();
			final String checkConstraint = physicalColumn.getCheckConstraint();
			if ( checkConstraint == null && dialect.supportsColumnCheck() ) {
				physicalColumn.setCheckConstraint(
						basicType.getJavaTypeDescriptor().getCheckCondition(
								physicalColumn.getQuotedName( dialect ),
								basicType.getSqlTypeDescriptor(),
								dialect
						)
				);
			}
		}

		return resolution;
	}

	protected Resolution<?> buildResolution() {
		if ( explicitTypeName != null ) {
			return interpretExplicitlyNamedType(
					explicitTypeName,
					enumerationStyle,
					implicitJavaTypeAccess,
					explicitJavaTypeAccess,
					explicitSqlTypeAccess,
					explicitMutabilityPlanAccess,
					attributeConverterDescriptor,
					explicitLocalTypeParams,
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
				final Class implicitJtd = implicitJavaTypeAccess.apply( typeConfiguration );
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


		// Use JTD if we know it to apply any specialized resolutions

		final TypeDefinition autoAppliedTypeDef = getBuildingContext().getTypeDefinitionRegistry()
				.resolveAutoApplied( (BasicJavaDescriptor<?>) jtd );
		if ( autoAppliedTypeDef != null ) {
			log.debug( "BasicValue resolution matched auto-applied type-definition" );
			return autoAppliedTypeDef.resolve( getTypeParameters(), null, getBuildingContext() );
		}

		if ( jtd instanceof EnumJavaTypeDescriptor ) {
			return InferredBasicValueResolver.fromEnum(
					(EnumJavaTypeDescriptor) jtd,
					explicitJavaTypeAccess.apply( typeConfiguration ),
					explicitSqlTypeAccess.apply( typeConfiguration ),
					this,
					typeConfiguration
			);
		}

		if ( jtd instanceof TemporalJavaTypeDescriptor ) {
			return InferredBasicValueResolver.fromTemporal(
					(TemporalJavaTypeDescriptor) jtd,
					explicitJavaTypeAccess,
					explicitSqlTypeAccess,
					this,
					typeConfiguration
			);
		}

		return InferredBasicValueResolver.from(
				explicitJavaTypeAccess,
				explicitSqlTypeAccess,
				this::determineReflectedJavaTypeDescriptor,
				this,
				getTable(),
				column,
				ownerName,
				propertyName,
				typeConfiguration
		);

	}

	private JavaTypeDescriptor determineReflectedJavaTypeDescriptor() {
		final Class impliedJavaType;

		if ( resolvedJavaClass != null ) {
			impliedJavaType = resolvedJavaClass;
		}
		else if ( implicitJavaTypeAccess != null ) {
			impliedJavaType = implicitJavaTypeAccess.apply( typeConfiguration );
		}
		else if ( ownerName != null && propertyName != null ) {
			final ServiceRegistry serviceRegistry = typeConfiguration.getServiceRegistry();
			final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

			impliedJavaType = ReflectHelper.reflectedPropertyClass(
					ownerName,
					propertyName,
					classLoaderService
			);
		}
		else {
			return null;
		}

		resolvedJavaClass = impliedJavaType;
		return typeConfiguration.getJavaTypeDescriptorRegistry().resolveDescriptor( impliedJavaType );
	}


	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Resolution interpretExplicitlyNamedType(
			String name,
			EnumType enumerationStyle,
			Function<TypeConfiguration, Class> implicitJavaTypeAccess,
			Function<TypeConfiguration, BasicJavaDescriptor> explicitJtdAccess,
			Function<TypeConfiguration, SqlTypeDescriptor> explicitStdAccess,
			Function<TypeConfiguration, MutabilityPlan> explicitMutabilityPlanAccess,
			ConverterDescriptor converterDescriptor,
			Map localTypeParams,
			SqlTypeDescriptorIndicators stdIndicators,
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
			if ( converterDescriptor == null ) {
				valueConverter = null;
				domainJtd = basicTypeByName.getJavaTypeDescriptor();
			}
			else {
				valueConverter = converterDescriptor.createJpaAttributeConverter( converterCreationContext );
				domainJtd = valueConverter.getDomainJavaDescriptor();
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
					context
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
						null,
						explicitMutabilityPlanAccess != null
								? explicitMutabilityPlanAccess.apply( typeConfiguration )
								: null,
						context
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

	public boolean isNationalized() {
		return isNationalized;
	}

	public boolean isLob() {
		return isLob;
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
					attributeConverterDescriptor = new ClassBasedConverterDescriptor(
							converterClass,
							false,
							getBuildingContext().getBootstrapContext().getClassmateContext()
					);
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

	public void makeLob() {
		this.isLob = true;
	}

	public void makeNationalized() {
		this.isNationalized = true;
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
		SqlTypeDescriptor getRelationalSqlTypeDescriptor();

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
