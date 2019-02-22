/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.persistence.AttributeConverter;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.domain.NotYetResolvedException;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.domain.internal.NamedBasicTypeResolution;
import org.hibernate.boot.model.domain.internal.NamedConverterResolution;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.internal.EnumJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.internal.InferredBasicValueResolver;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
@SuppressWarnings("unused")
public class BasicValue
		extends SimpleValue
		implements org.hibernate.boot.model.domain.BasicValueMapping, SqlTypeDescriptorIndicators {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( BasicValue.class );

	private final TypeConfiguration typeConfiguration;
	private final int preferredJdbcTypeCodeForBoolean;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// incoming "configuration" values


	private Function<TypeConfiguration,BasicJavaDescriptor<?>> explicitJavaTypeAccess;
	private Function<TypeConfiguration,SqlTypeDescriptor> explicitSqlTypeAccess;

	private MutabilityPlan explicitMutabilityPlan;

	private boolean isNationalized;
	private boolean isLob;
	private EnumType enumType;
	private TemporalType temporalPrecision;

	private ConverterDescriptor attributeConverterDescriptor;

	private Class resolvedJavaClass;

	private String ownerName;
	private String propertyName;

	private Map explicitLocalTypeParams;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Resolution - resolved state; available after `#resolve`

	private Resolution resolution;
	private BasicValue dependentValue;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// remove these - they serve dual purpose (in/out) in original code.  Use
	//		`#resolution` instead for outputs

	private BasicJavaDescriptor javaTypeDescriptor;
	private JavaTypeMapping javaTypeMapping;
	private SqlTypeDescriptor sqlTypeDescriptor;


	public BasicValue(MetadataBuildingContext buildingContext, MappedTable table) {
		this( buildingContext, table, null );
	}

	public BasicValue(MetadataBuildingContext buildingContext, MappedTable table, BasicValue dependentValue) {
		super( buildingContext, table );

		this.typeConfiguration = buildingContext.getBootstrapContext().getTypeConfiguration();
		this.preferredJdbcTypeCodeForBoolean = buildingContext.getPreferredSqlTypeCodeForBoolean();

		// When this is provided, the value mapping resolution phase will be based on the resolution
		// performed by the specified dependentValue's resolution.
		this.dependentValue = dependentValue;

		this.javaTypeMapping = new BasicJavaTypeMapping( this );
		buildingContext.getMetadataCollector().registerValueMappingResolver( this::resolve );
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return javaTypeMapping;
	}

	@Override
	public Resolution getResolution() throws NotYetResolvedException {
		if ( resolution == null ) {
			throw new NotYetResolvedException( "BasicValue[" + toString() + "] not yet resolved" );
		}
		return resolution;
	}


	@Override
	@SuppressWarnings("unchecked")
	public Boolean resolve(ResolutionContext context) {
		if ( resolution != null ) {
			return true;
		}

		/*
		 * @Entity
		 * @TypeDef( name="mine", implClass="..", parameters={...} )
		 * class It {
		 *     ...
		 *     @Basic
		 *     @Type( name="mine", parameters={...} )
		 *     pubic Mine getMine() {...}
		 * }
		 */

		final String name = getTypeName();
		if ( name != null ) {
			resolution = interpretExplicitlyNamedType(
					name,
					explicitJavaTypeAccess,
					explicitSqlTypeAccess,
					attributeConverterDescriptor,
					explicitMutabilityPlan,
					explicitLocalTypeParams,
					this,
					typeConfiguration,
					context
			);

		}
		else {
			final ServiceRegistry serviceRegistry = typeConfiguration.getServiceRegistry();
			final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

			resolution = implicitlyResolveBasicType(
					explicitJavaTypeAccess,
					explicitSqlTypeAccess,
					attributeConverterDescriptor,
					this,
					() -> {
						if ( resolvedJavaClass != null ) {
							return (BasicJavaDescriptor) context.getBootstrapContext()
									.getTypeConfiguration()
									.getJavaTypeDescriptorRegistry()
									.getOrMakeJavaDescriptor( resolvedJavaClass );
						}
						else if ( ownerName != null && propertyName != null ) {
							final Class reflectedJavaType = ReflectHelper.reflectedPropertyClass(
									ownerName,
									propertyName,
									classLoaderService
							);
							return (BasicJavaDescriptor) context.getBootstrapContext()
									.getTypeConfiguration()
									.getJavaTypeDescriptorRegistry()
									.getOrMakeJavaDescriptor( reflectedJavaType );
						}
						else if ( dependentValue != null ) {
							// todo (6.0) - Can we just use the resolution directly?
							//		In 5.x we copied the typeName and its associated parameters for this use case.
							//		It would stand to reason we could just share the same Resolution instance
							//		instead of constructing this BasicValue's very own here?
							return dependentValue.getResolution().getDomainJavaDescriptor();
						}

						return null;
					},
					typeConfiguration
			);
		}

		assert resolution.getValueMapper() != null;
		assert resolution.getValueMapper().getSqlExpressableType() != null;
		assert resolution.getBasicType() != null;

		final MappedColumn column = getMappedColumn();

		// todo (6.0) : look at having this accept `Supplier<JavaTypeDescriptor>` instead
		column.setJavaTypeMapping(
				new JavaTypeMapping() {
					@Override
					public String getTypeName() {
						return name;
					}

					@Override
					public JavaTypeDescriptor getJavaTypeDescriptor() throws NotYetResolvedException {
						return resolution.getRelationalJavaDescriptor();
					}
				}
		);

		column.setSqlTypeDescriptorAccess( resolution::getRelationalSqlTypeDescriptor );

		return true;
	}


	@SuppressWarnings("unchecked")
	private static Resolution interpretExplicitlyNamedType(
			String name,
			Function<TypeConfiguration,BasicJavaDescriptor<?>> explicitJtdAccess,
			Function<TypeConfiguration,SqlTypeDescriptor> explicitStdAccess,
			ConverterDescriptor converterDescriptor,
			MutabilityPlan explicitMutabilityPlan,
			Map localTypeParams,
			SqlTypeDescriptorIndicators stdIndicators,
			TypeConfiguration typeConfiguration,
			ResolutionContext resolutionContext) {

		final ManagedBeanRegistry managedBeanRegistry = resolutionContext.getBootstrapContext()
				.getServiceRegistry()
				.getService( ManagedBeanRegistry.class );

		final JpaAttributeConverterCreationContext converterCreationContext = new JpaAttributeConverterCreationContext() {
			@Override
			public ManagedBeanRegistry getManagedBeanRegistry() {
				return managedBeanRegistry;
			}

			@Override
			public JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry() {
				return typeConfiguration.getJavaTypeDescriptorRegistry();
			}
		};


		// Name could refer to:
		//		1) a named converter - HBM support for JPA's AttributeConverter via its `type="..."` XML attribute
		//		2) basic type "resolution key"
		//		3) UserType or BasicType class name - directly, or through a TypeDefinition

		if ( name.startsWith( ConverterDescriptor.TYPE_NAME_PREFIX  ) ) {
			// atm this should never happen due to impl of `#setExplicitTypeName`
			return NamedConverterResolution.from(
					name,
					explicitJtdAccess,
					explicitStdAccess,
					converterCreationContext,
					explicitMutabilityPlan,
					stdIndicators,
					resolutionContext
			);
		}

		// see if it is a named basic type
		final BasicType basicTypeByName = typeConfiguration.getBasicTypeRegistry().getBasicTypeByName( name );
		if ( basicTypeByName != null ) {
			final BasicValueConverter valueConverter;
			final BasicJavaDescriptor domainJtd;
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
					explicitMutabilityPlan,
					resolutionContext
			);
		}

		// see if it is a named TypeDefinition
		final TypeDefinition typeDefinition = resolutionContext.getMetadataBuildingContext().resolveTypeDefinition( name );
		if ( typeDefinition != null ) {
			return typeDefinition.resolve(
					explicitJtdAccess.apply( typeConfiguration ),
					explicitStdAccess.apply( typeConfiguration ),
					localTypeParams,
					explicitMutabilityPlan,
					resolutionContext.getMetadataBuildingContext()
			);
		}


		// see if the name is a UserType or (API) BasicType impl class name
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
						Collections.emptyMap(),
						typeConfiguration
				);
				resolutionContext.getMetadataBuildingContext().addTypeDefinition( implicitDefinition );
				return implicitDefinition.resolve(
						explicitJtdAccess != null ? explicitJtdAccess.apply( typeConfiguration ) : null,
						explicitStdAccess != null ? explicitStdAccess.apply( typeConfiguration ) : null,
						Collections.emptyMap(),
						explicitMutabilityPlan,
						resolutionContext.getMetadataBuildingContext()
				);
			}

			return TypeDefinition.createLocalResolution(
					name,
					typeNamedClass,
					explicitJtdAccess.apply( typeConfiguration ),
					explicitStdAccess.apply( typeConfiguration ),
					explicitMutabilityPlan,
					localTypeParams,
					resolutionContext
			);
		}
		catch (ClassLoadingException ignore) {
			// allow the exception below to trigger
		}

		throw new NotYetResolvedException( "Could not resolve named type : " + name );
	}

	@SuppressWarnings("unchecked")
	private static Resolution implicitlyResolveBasicType(
			Function<TypeConfiguration,BasicJavaDescriptor<?>> explicitJtdAccess,
			Function<TypeConfiguration,SqlTypeDescriptor> explicitStdAccess,
			ConverterDescriptor attributeConverterDescriptor,
			SqlTypeDescriptorIndicators stdIndicators,
			Supplier<BasicJavaDescriptor> reflectedJtdResolver,
			TypeConfiguration typeConfiguration) {

		final InferredBasicValueResolver resolution = new InferredBasicValueResolver( explicitJtdAccess, explicitStdAccess, typeConfiguration );

		if ( attributeConverterDescriptor != null ) {
			// we have an attribute converter, use that to either:
			//		1) validate the explicit BasicJavaDescriptor/SqlTypeDescriptor
			//		2) use the converter Class parameters to infer the BasicJavaDescriptor/SqlTypeDescriptor

			final Class<?> converterDomainJavaType = attributeConverterDescriptor.getDomainValueResolvedType()
					.getErasedType();

			final JavaTypeDescriptor<?> converterDomainJtd = typeConfiguration.getJavaTypeDescriptorRegistry()
					.getDescriptor( converterDomainJavaType );

			if ( resolution.getDomainJtd() == null ) {
				resolution.setDomainJtd( (BasicJavaDescriptor) converterDomainJtd );
			}
			else {
				if ( !resolution.getDomainJtd().equals( converterDomainJtd ) ) {
					throw new HibernateException(
							"JavaTypeDescriptors did not match between BasicTypeParameters#getJavaTypeDescriptor and " +
									"BasicTypeParameters#getAttributeConverterDefinition#getDomainType"
					);
				}
			}

			final Class<?> converterRelationalJavaType = attributeConverterDescriptor.getRelationalValueResolvedType()
					.getErasedType();

			resolution.setRelationalJtd(
					(BasicJavaDescriptor<?>) typeConfiguration.getJavaTypeDescriptorRegistry()
							.getDescriptor( converterRelationalJavaType )
			);

			final SqlTypeDescriptor converterHintedStd = resolution.getRelationalJtd().getJdbcRecommendedSqlType( stdIndicators );

			if ( resolution.getRelationalStd() == null ) {
				resolution.setRelationalStd( converterHintedStd );
			}
			else {
				if ( !resolution.getRelationalStd().equals( converterHintedStd ) ) {
					throw new HibernateException(
							"SqlTypeDescriptors did not match between BasicTypeParameters#getSqlTypeDescriptor and " +
									"BasicTypeParameters#getAttributeConverterDefinition#getJdbcType"
					);
				}
			}

			resolution.setValueConverter(
					attributeConverterDescriptor.createJpaAttributeConverter(
							new JpaAttributeConverterCreationContext() {
								@Override
								public ManagedBeanRegistry getManagedBeanRegistry() {
									return typeConfiguration.getServiceRegistry().getService( ManagedBeanRegistry.class );
								}

								@Override
								public JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry() {
									return typeConfiguration.getJavaTypeDescriptorRegistry();
								}
							}
					)
			);
		}
		else {
			if ( resolution.getDomainJtd() == null ) {
				resolution.setDomainJtd( reflectedJtdResolver.get() );
			}

			if ( resolution.getDomainJtd() instanceof EnumJavaDescriptor ) {
				final EnumJavaDescriptor enumJavaDescriptor = (EnumJavaDescriptor) resolution.getDomainJtd();

				final EnumType enumType = stdIndicators.getEnumeratedType() != null
						? stdIndicators.getEnumeratedType()
						: typeConfiguration.getMetadataBuildingContext().getBuildingOptions().getImplicitEnumType();

				switch ( enumType ) {
					case STRING: {
						resolution.setRelationalJtd(
								(BasicJavaDescriptor<?>) typeConfiguration.getJavaTypeDescriptorRegistry()
										.getDescriptor( String.class )
						);
						resolution.setValueConverter(
								new NamedEnumValueConverter( enumJavaDescriptor, typeConfiguration )
						);
						break;
					}
					case ORDINAL: {
						resolution.setRelationalJtd(
								(BasicJavaDescriptor<?>) typeConfiguration.getJavaTypeDescriptorRegistry()
										.getDescriptor( Integer.class )
						);
						resolution.setValueConverter(
								new OrdinalEnumValueConverter( enumJavaDescriptor, typeConfiguration )
						);
						break;
					}
					default: {
						throw new HibernateException( "Unknown EnumType : " + enumType );
					}
				}
			}
			else if ( resolution.getDomainJtd() instanceof TemporalJavaDescriptor ) {
				if ( stdIndicators.getTemporalPrecision() != null ) {
					resolution.setDomainJtd(
							( (TemporalJavaDescriptor) resolution.getDomainJtd() ).resolveTypeForPrecision(
									stdIndicators.getTemporalPrecision(),
									typeConfiguration
							)
					);
				}
			}
		}


		if ( resolution.getRelationalStd() == null ) {
			if ( resolution.getRelationalJtd() == null ) {
				if ( resolution.getDomainJtd() == null ) {
					throw new IllegalArgumentException(
							"Could not determine JavaTypeDescriptor nor SqlTypeDescriptor to use"
					);
				}

				resolution.setRelationalJtd( resolution.getDomainJtd() );
			}

			resolution.setRelationalStd( resolution.getRelationalJtd().getJdbcRecommendedSqlType( stdIndicators ) );
		}
		else if ( resolution.getRelationalJtd() == null ) {
			resolution.setRelationalJtd(
					resolution.getRelationalStd().getJdbcRecommendedJavaTypeMapping( typeConfiguration )
			);
			if ( resolution.getDomainJtd() == null ) {
				resolution.setDomainJtd( resolution.getRelationalJtd() );
			}
		}

		return resolution.build();
	}

	public boolean isNationalized() {
		return isNationalized;
	}

	public boolean isLob() {
		return isLob;
	}

	public EnumType getEnumType() {
		return enumType;
	}

	public TemporalType getTemporalPrecision() {
		return temporalPrecision;
	}

	public SqlTypeDescriptor getExplicitSqlType() {
		return sqlTypeDescriptor;
	}

	public BasicJavaDescriptor getExplicitJavaTypeDescriptor(){
		return javaTypeDescriptor;
	}

	public void setJpaAttributeConverterDescriptor(ConverterDescriptor attributeConverterDescriptor) {
		this.attributeConverterDescriptor = attributeConverterDescriptor;
	}

	public void makeNationalized() {
		this.isNationalized = true;
	}

	public void makeLob() {
		this.isLob = true;
	}

	public void setEnumType(EnumType enumType) {
		this.enumType = enumType;
	}

	public void setTemporalPrecision(TemporalType temporalPrecision) {
		this.temporalPrecision = temporalPrecision;
	}

	public void setExplicitSqlTypeAccess(Function<TypeConfiguration, SqlTypeDescriptor> sqlTypeAccess) {
		this.explicitSqlTypeAccess = sqlTypeAccess;
	}

	public void setExplicitMutabilityPlan(MutabilityPlan explicitMutabilityPlan) {
		this.explicitMutabilityPlan = explicitMutabilityPlan;
	}

	@Override
	public void addColumn(Column column) {
		if ( getMappedColumns().size() > 0 && !getMappedColumn().equals( column ) ) {
			throw new MappingException( "Attempt to add additional MappedColumn to BasicValueMapping " + column.getName() );
		}
		super.addColumn( column );
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public void setJavaClass(Class resolvedJavaClass) {
		this.resolvedJavaClass = resolvedJavaClass;
	}

	@Override
	public void addFormula(Formula formula) {
		if ( getMappedColumns().size() > 0 ) {
			throw new MappingException( "Attempt to add additional MappedColumn to BasicValueMapping" );
		}
		super.addFormula( formula );
	}

	public void setExplicitTypeName(String typeName) {
		if ( typeName != null && typeName.startsWith( ConverterDescriptor.TYPE_NAME_PREFIX ) ) {
			final String converterClassName = typeName.substring( ConverterDescriptor.TYPE_NAME_PREFIX.length() );
			final ClassLoaderService cls = getMetadataBuildingContext()
					.getMetadataCollector()
					.getMetadataBuildingOptions()
					.getServiceRegistry()
					.getService( ClassLoaderService.class );
			try {
				final Class<AttributeConverter> converterClass = cls.classForName( converterClassName );
				attributeConverterDescriptor = new ClassBasedConverterDescriptor(
						converterClass,
						false,
						getMetadataBuildingContext().getBootstrapContext().getClassmateContext()
				);
				return;
			}
			catch (Exception e) {
				log.logBadHbmAttributeConverterType( typeName, e.getMessage() );
			}
		}

		super.setExplicitTypeName( typeName );
	}

	@Override
	public boolean isTypeSpecified() {
		// We mandate a BasicTypeResolver, so this is always true.
		return true;
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		this.ownerName = className;
		this.propertyName = propertyName;
	}

	public <T> void setJavaTypeDescriptor(BasicJavaDescriptor<T> javaDescriptor) {
		this.javaTypeDescriptor = javaDescriptor;
	}

	@SuppressWarnings("unchecked")
	public <T> void setExplicitJavaTypeAccess(Function<TypeConfiguration,BasicJavaDescriptor<T>> access) {
		this.explicitJavaTypeAccess = (Function) access;
	}

	class ValueConverterCollector implements Consumer<BasicValueConverter> {
		private BasicValueConverter basicValueConverter;

		public BasicValueConverter getBasicValueConverter() {
			return basicValueConverter;
		}

		public void setBasicValueConverter(BasicValueConverter basicValueConverter) {
			this.basicValueConverter = basicValueConverter;
		}

		@Override
		public void accept(BasicValueConverter valueConverter) {
			setBasicValueConverter( valueConverter );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlTypeDescriptorIndicators

	@Override
	public EnumType getEnumeratedType() {
		return enumType;
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JavaTypeMapping

	static class BasicJavaTypeMapping implements JavaTypeMapping {
		private final BasicValue basicValue;

		BasicJavaTypeMapping(BasicValue basicValue) {
			this.basicValue = basicValue;
		}

		public Class getJavaClass() {
			return getJavaTypeDescriptor().getClass();
		}

		@Override
		public String getTypeName() {
			String typeName = basicValue.getTypeName();
			if ( typeName != null ) {
				return typeName;
			}
			return getJavaTypeDescriptor().getTypeName();
		}

		@Override
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			if ( basicValue.resolution == null ) {
				throw new NotYetResolvedException( "JavaTypeDescriptor not yet resolved" );
			}
			return basicValue.resolution.getDomainJavaDescriptor();
		}
	}

}
