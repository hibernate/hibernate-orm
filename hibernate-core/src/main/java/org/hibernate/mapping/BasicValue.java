/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolution;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolver;
import org.hibernate.boot.model.process.internal.NamedBasicTypeResolution;
import org.hibernate.boot.model.process.internal.NamedConverterResolution;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.RowVersionType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.RowVersionTypeDescriptor;
import org.hibernate.type.descriptor.java.TemporalJavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class BasicValue extends SimpleValue implements SqlTypeDescriptorIndicators {

	private final TypeConfiguration typeConfiguration;
	private final int preferredJdbcTypeCodeForBoolean;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// incoming "configuration" values


	private Function<TypeConfiguration,JavaTypeDescriptor<?>> explicitJavaTypeAccess;
	private Function<TypeConfiguration,SqlTypeDescriptor> explicitSqlTypeAccess;

	private MutabilityPlan explicitMutabilityPlan;

	private boolean isNationalized;
	private boolean isLob;
	private EnumType enumerationStyle;
	private TemporalType temporalPrecision;

	private ConverterDescriptor attributeConverterDescriptor;

	private Class resolvedJavaClass;

	private String ownerName;
	private String propertyName;

	private Properties explicitLocalTypeParams;

	private BasicValue dependentValue;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Resolved state - available after `#setTypeUsingReflection`

	private Resolution<?> resolution;

	public BasicValue(MetadataBuildingContext buildingContext) {
		super( buildingContext );

		this.typeConfiguration = buildingContext.getBootstrapContext().getTypeConfiguration();
		this.preferredJdbcTypeCodeForBoolean = buildingContext.getPreferredSqlTypeCodeForBoolean();
	}

	public BasicValue(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );

		this.typeConfiguration = buildingContext.getBootstrapContext().getTypeConfiguration();
		this.preferredJdbcTypeCodeForBoolean = buildingContext.getPreferredSqlTypeCodeForBoolean();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Setters - in preparation of resolution

	public void setJavaClass(Class resolvedJavaClass) {
		this.resolvedJavaClass = resolvedJavaClass;
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		if ( resolution != null ) {
			throw new IllegalStateException( "BasicValue already resolved" );
		}

		this.ownerName = className;
		this.propertyName = propertyName;
	}

	public void setEnumerationStyle(EnumType enumerationStyle) {
		this.enumerationStyle = enumerationStyle;
	}

	public EnumType getEnumerationStyle() {
		return enumerationStyle;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Resolution

	@Override
	public Type getType() throws MappingException {
		resolve();
		assert resolution != null;

		return resolution.getResolvedBasicType();
	}

	public Resolution<?> resolve() {
		if ( resolution != null ) {
			return resolution;
		}

		final String explicitTypeName = getTypeName();
		if ( explicitTypeName != null ) {
			resolution = interpretExplicitlyNamedType(
					explicitTypeName,
					explicitJavaTypeAccess,
					explicitSqlTypeAccess,
					attributeConverterDescriptor,
					explicitMutabilityPlan,
					explicitLocalTypeParams,
					this,
					typeConfiguration,
					getBuildingContext()
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
							return getBuildingContext().getBootstrapContext()
									.getTypeConfiguration()
									.getJavaTypeDescriptorRegistry()
									.resolveDescriptor( resolvedJavaClass );
						}
						else if ( ownerName != null && propertyName != null ) {
							final Class reflectedJavaType = ReflectHelper.reflectedPropertyClass(
									ownerName,
									propertyName,
									classLoaderService
							);

							// First resolve from the BasicTypeRegistry.
							// If it does resolve, we can use the JTD instead of delegating to the JTD Regsitry.
							final BasicType basicType = getBuildingContext().getBootstrapContext()
									.getTypeConfiguration()
									.getBasicTypeRegistry()
									.getRegisteredType( reflectedJavaType.getName() );
							if ( basicType != null ) {
								return basicType.getJavaTypeDescriptor();
							}

							return getBuildingContext().getBootstrapContext()
									.getTypeConfiguration()
									.getJavaTypeDescriptorRegistry()
									.resolveDescriptor( reflectedJavaType );
						}
						else if ( dependentValue != null ) {
							// todo (6.0) - Can we just use the resolution directly?
							//		In 5.x we copied the typeName and its associated parameters for this use case.
							//		It would stand to reason we could just share the same Resolution instance
							//		instead of constructing this BasicValue's very own here?
							return dependentValue.resolve().getDomainJavaDescriptor();
						}

						return null;
					},
					isVersion(),
					typeConfiguration
			);
		}

		return resolution;
	}



	@SuppressWarnings("unchecked")
	private static Resolution interpretExplicitlyNamedType(
			String name,
			Function<TypeConfiguration,JavaTypeDescriptor<?>> explicitJtdAccess,
			Function<TypeConfiguration,SqlTypeDescriptor> explicitStdAccess,
			ConverterDescriptor converterDescriptor,
			MutabilityPlan explicitMutabilityPlan,
			Properties localTypeParams,
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
			// atm this should never happen due to impl of `#setExplicitTypeName`
			return NamedConverterResolution.from(
					name,
					explicitJtdAccess,
					explicitStdAccess,
					converterCreationContext,
					explicitMutabilityPlan,
					stdIndicators,
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
					explicitMutabilityPlan,
					context
			);
		}

		// see if it is a named TypeDefinition
		final TypeDefinition typeDefinition = context.getTypeDefinitionRegistry().resolve( name );
		if ( typeDefinition != null ) {
			return typeDefinition.resolve(
					explicitJtdAccess.apply( typeConfiguration ),
					explicitStdAccess.apply( typeConfiguration ),
					localTypeParams,
					explicitMutabilityPlan,
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
						explicitJtdAccess != null ? explicitJtdAccess.apply( typeConfiguration ) : null,
						explicitStdAccess != null ? explicitStdAccess.apply( typeConfiguration ) : null,
						null,
						explicitMutabilityPlan,
						context
				);
			}

			return TypeDefinition.createLocalResolution(
					name,
					typeNamedClass,
					explicitJtdAccess.apply( typeConfiguration ),
					explicitStdAccess.apply( typeConfiguration ),
					explicitMutabilityPlan,
					localTypeParams,
					context
			);
		}
		catch (ClassLoadingException ignore) {
			// allow the exception below to trigger
		}

		throw new MappingException( "Could not resolve named type : " + name );
	}

	@SuppressWarnings("unchecked")
	private static Resolution implicitlyResolveBasicType(
			Function<TypeConfiguration,JavaTypeDescriptor<?>> explicitJtdAccess,
			Function<TypeConfiguration,SqlTypeDescriptor> explicitStdAccess,
			ConverterDescriptor attributeConverterDescriptor,
			SqlTypeDescriptorIndicators stdIndicators,
			Supplier<JavaTypeDescriptor> reflectedJtdResolver,
			boolean isVersion,
			TypeConfiguration typeConfiguration) {

		final InferredBasicValueResolver resolver = new InferredBasicValueResolver( explicitJtdAccess, explicitStdAccess, typeConfiguration );

		if ( attributeConverterDescriptor != null ) {
			assert !isVersion : "Version attribute cannot define AttributeConverter";

			// we have an attribute converter, use that to either:
			//		1) validate the explicit BasicJavaDescriptor/SqlTypeDescriptor
			//		2) use the converter Class parameters to infer the BasicJavaDescriptor/SqlTypeDescriptor

			final Class<?> converterDomainJavaType = attributeConverterDescriptor.getDomainValueResolvedType()
					.getErasedType();

			final JavaTypeDescriptor<?> converterDomainJtd = typeConfiguration.getJavaTypeDescriptorRegistry()
					.getDescriptor( converterDomainJavaType );

			if ( resolver.getDomainJtd() == null ) {
				resolver.setDomainJtd( converterDomainJtd );
			}
			else {
				if ( !resolver.getDomainJtd().equals( converterDomainJtd ) ) {
					throw new HibernateException(
							"JavaTypeDescriptors did not match between BasicTypeParameters#getJavaTypeDescriptor and " +
									"BasicTypeParameters#getAttributeConverterDefinition#getDomainType"
					);
				}
			}

			final Class<?> converterRelationalJavaType = attributeConverterDescriptor.getRelationalValueResolvedType()
					.getErasedType();

			resolver.setRelationalJtd(
					typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( converterRelationalJavaType )
			);

			final SqlTypeDescriptor converterHintedStd = resolver.getRelationalJtd().getJdbcRecommendedSqlType( stdIndicators );

			if ( resolver.getRelationalStd() == null ) {
				resolver.setRelationalStd( converterHintedStd );
			}
			else {
				if ( !resolver.getRelationalStd().equals( converterHintedStd ) ) {
					throw new HibernateException(
							"SqlTypeDescriptors did not match between BasicTypeParameters#getSqlTypeDescriptor and " +
									"BasicTypeParameters#getAttributeConverterDefinition#getJdbcType"
					);
				}
			}

			resolver.setValueConverter(
					attributeConverterDescriptor.createJpaAttributeConverter(
							new JpaAttributeConverterCreationContext() {
								@Override
								public ManagedBeanRegistry getManagedBeanRegistry() {
									return typeConfiguration.getServiceRegistry().getService( ManagedBeanRegistry.class );
								}

								@Override
								public TypeConfiguration getTypeConfiguration() {
									return typeConfiguration;
								}
							}
					)
			);
		}
		else {
			if ( resolver.getDomainJtd() == null ) {
				resolver.setDomainJtd( reflectedJtdResolver.get() );
			}

			if ( resolver.getDomainJtd() instanceof EnumJavaTypeDescriptor ) {
				final EnumJavaTypeDescriptor<?> enumJavaDescriptor = (EnumJavaTypeDescriptor<?>) resolver.getDomainJtd();

				final EnumType enumType = stdIndicators.getEnumeratedType() != null
						? stdIndicators.getEnumeratedType()
						: EnumType.ORDINAL;

				switch ( enumType ) {
					case STRING: {
						final JavaTypeDescriptor<String> stringJtd = typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( String.class );
						final SqlTypeDescriptor relationalStd = stringJtd.getJdbcRecommendedSqlType( stdIndicators );

						resolver.setRelationalJtd( stringJtd );
						resolver.setRelationalStd( relationalStd );

						final NamedEnumValueConverter valueConverter = new NamedEnumValueConverter(
								enumJavaDescriptor,
								relationalStd,
								stringJtd
						);

						resolver.setValueConverter( valueConverter );

						final org.hibernate.type.EnumType enumMappingType = new org.hibernate.type.EnumType(
								enumJavaDescriptor.getJavaType(),
								valueConverter,
								typeConfiguration
						);

						final CustomType basicType = new CustomType( enumMappingType, typeConfiguration );

						resolver.injectResolution(
								new InferredBasicValueResolution(
										basicType,
										enumJavaDescriptor,
										stringJtd,
										relationalStd,
										valueConverter,
										ImmutableMutabilityPlan.INSTANCE
								)
						);
						break;
					}
					case ORDINAL: {
						final JavaTypeDescriptor<Integer> integerJtd = typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class );
						final SqlTypeDescriptor std = integerJtd.getJdbcRecommendedSqlType( stdIndicators );

						resolver.setRelationalJtd( integerJtd );
						resolver.setRelationalStd( std );

						final OrdinalEnumValueConverter valueConverter = new OrdinalEnumValueConverter(
								enumJavaDescriptor,
								std,
								integerJtd
						);

						resolver.setValueConverter( valueConverter );

						final org.hibernate.type.EnumType enumMappingType = new org.hibernate.type.EnumType(
								enumJavaDescriptor.getJavaType(),
								valueConverter,
								typeConfiguration
						);

						final CustomType basicType = new CustomType( enumMappingType, typeConfiguration );

						resolver.injectResolution(
								new InferredBasicValueResolution(
										basicType,
										enumJavaDescriptor,
										integerJtd,
										std,
										valueConverter,
										ImmutableMutabilityPlan.INSTANCE
								)
						);
						break;
					}
					default: {
						throw new HibernateException( "Unknown EnumType : " + enumType );
					}
				}
			}
			else if ( resolver.getDomainJtd() instanceof TemporalJavaTypeDescriptor ) {
				if ( stdIndicators.getTemporalPrecision() != null ) {
					resolver.setDomainJtd(
							( (TemporalJavaTypeDescriptor) resolver.getDomainJtd() ).resolveTypeForPrecision(
									stdIndicators.getTemporalPrecision(),
									typeConfiguration
							)
					);
				}
			}
			else if ( resolver.getDomainJtd() instanceof PrimitiveByteArrayTypeDescriptor && isVersion ) {
				resolver.setDomainJtd( RowVersionTypeDescriptor.INSTANCE );
				resolver.injectResolution(
						new InferredBasicValueResolution(
								RowVersionType.INSTANCE,
								RowVersionType.INSTANCE.getJavaTypeDescriptor(),
								RowVersionType.INSTANCE.getJavaTypeDescriptor(),
								RowVersionType.INSTANCE.getSqlTypeDescriptor(),
								null,
								ImmutableMutabilityPlan.INSTANCE
						)
				);
			}
		}


		if ( resolver.getRelationalStd() == null ) {
			if ( resolver.getRelationalJtd() == null ) {
				if ( resolver.getDomainJtd() == null ) {
					throw new IllegalArgumentException(
							"Could not determine JavaTypeDescriptor nor SqlTypeDescriptor to use"
					);
				}

				resolver.setRelationalJtd( resolver.getDomainJtd() );
			}

			resolver.setRelationalStd( resolver.getRelationalJtd().getJdbcRecommendedSqlType( stdIndicators ) );
		}
		else if ( resolver.getRelationalJtd() == null ) {
			resolver.setRelationalJtd(
					resolver.getRelationalStd().getJdbcRecommendedJavaTypeMapping( typeConfiguration )
			);
			if ( resolver.getDomainJtd() == null ) {
				resolver.setDomainJtd( resolver.getRelationalJtd() );
			}
		}

		return resolver.build();
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

	public void setExplicitTypeParams(Properties explicitLocalTypeParams) {
		this.explicitLocalTypeParams = explicitLocalTypeParams;
	}

	/**
	 * Resolved form of {@link BasicValue} as part of interpreting the
	 * boot-time model into the run-time model
	 */
	public interface Resolution<J> {
		/**
		 * The associated BasicType
		 */
		BasicType getResolvedBasicType();

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
