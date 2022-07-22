/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import java.io.Serializable;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.boot.model.process.internal.UserTypeResolution;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.usertype.UserType;

import static org.hibernate.mapping.MappingHelper.injectParameters;

/**
 * Models the information pertaining to a custom type definition supplied by the user.  Used
 * to delay instantiation of the actual {@link Type} instance.
 *
 * Generally speaking this information would come from annotations
 * ({@link org.hibernate.annotations.Type}) or XML mappings.  An alternative form of
 * supplying custom types is programmatically via one of:<ul>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(BasicType)}</li>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(UserType, String[])}</li>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyTypes(TypeContributor)}</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author John Verhaeg
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TypeDefinition implements Serializable {
	public static final AtomicInteger NAME_COUNTER = new AtomicInteger();

	private final String name;
	private final Class typeImplementorClass;
	private final String[] registrationKeys;
	private final Properties parameters;

	private BasicValue.Resolution<?> reusableResolution;


	public TypeDefinition(
			String name,
			Class typeImplementorClass,
			String[] registrationKeys,
			Properties parameters,
			TypeConfiguration typeConfiguration) {
		this.name = name;
		this.typeImplementorClass = typeImplementorClass;
		this.registrationKeys= registrationKeys;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public Class getTypeImplementorClass() {
		return typeImplementorClass;
	}

	public String[] getRegistrationKeys() {
		return registrationKeys;
	}

	public Properties getParameters() {
		return parameters;
	}

	public BasicValue.Resolution<?> resolve(
			Map localConfigParameters,
			MutabilityPlan explicitMutabilityPlan,
			MetadataBuildingContext context,
			JdbcTypeIndicators indicators) {
		if ( CollectionHelper.isEmpty( localConfigParameters ) ) {
			// we can use the re-usable resolution...
			if ( reusableResolution == null ) {
				reusableResolution = createResolution( name, Collections.emptyMap(), indicators, context );
			}

			return reusableResolution;
		}
		else {
			final String name = this.name + ":" + NAME_COUNTER.getAndIncrement();
			return createResolution( name, localConfigParameters, indicators, context );
		}
	}

	private BasicValue.Resolution<?> createResolution(
			String name,
			Map<?,?> usageSiteProperties,
			JdbcTypeIndicators indicators,
			MetadataBuildingContext context) {
		return createResolution(
				name,
				typeImplementorClass,
				parameters,
				usageSiteProperties,
				indicators,
				context
		);
	}

	private static BasicValue.Resolution<?> createResolution(
			String name,
			Class<?> typeImplementorClass,
			Properties parameters,
			Map<?,?> usageSiteProperties,
			JdbcTypeIndicators indicators,
			MetadataBuildingContext context) {
		final BootstrapContext bootstrapContext = context.getBootstrapContext();
		final TypeConfiguration typeConfiguration = bootstrapContext.getTypeConfiguration();
		final BeanInstanceProducer instanceProducer = bootstrapContext.getBeanInstanceProducer();
		final boolean isKnownType = Type.class.isAssignableFrom( typeImplementorClass )
				|| UserType.class.isAssignableFrom( typeImplementorClass );

		// support for AttributeConverter would be nice too
		if ( isKnownType ) {
			final Object typeInstance = instantiateType( bootstrapContext.getServiceRegistry(),
					name, typeImplementorClass, instanceProducer );

			if ( typeInstance instanceof TypeConfigurationAware ) {
				( (TypeConfigurationAware) typeInstance ).setTypeConfiguration( typeConfiguration );
			}

			final Properties combinedTypeParameters;

			if ( CollectionHelper.isNotEmpty( usageSiteProperties ) ) {
				combinedTypeParameters = new Properties( parameters );
				combinedTypeParameters.putAll( usageSiteProperties );
			}
			else {
				combinedTypeParameters = parameters;
			}

			injectParameters( typeInstance, combinedTypeParameters );

			if ( typeInstance instanceof UserType ) {
				final UserType<Object> userType = (UserType<Object>) typeInstance;
				final CustomType<Object> customType = new CustomType<>( userType, typeConfiguration );

				return new UserTypeResolution( customType, null, combinedTypeParameters );
			}

			if ( typeInstance instanceof BasicType ) {
				final BasicType resolvedBasicType = (BasicType) typeInstance;
				return new BasicValue.Resolution<Object>() {
					@Override
					public JdbcMapping getJdbcMapping() {
						return resolvedBasicType;
					}

					@Override
					public BasicType getLegacyResolvedBasicType() {
						return resolvedBasicType;
					}

					@Override
					public Properties getCombinedTypeParameters() {
						return combinedTypeParameters;
					}

					@Override
					public JavaType<Object> getDomainJavaType() {
						return resolvedBasicType.getMappedJavaType();
					}

					@Override
					public JavaType<?> getRelationalJavaType() {
						return resolvedBasicType.getMappedJavaType();
					}

					@Override
					public JdbcType getJdbcType() {
						return resolvedBasicType.getJdbcType();
					}

					@Override
					public BasicValueConverter getValueConverter() {
						return resolvedBasicType.getValueConverter();
					}

					@Override
					public MutabilityPlan<Object> getMutabilityPlan() {
						// a TypeDefinition does not explicitly provide a MutabilityPlan (yet?)
						return resolvedBasicType.isMutable()
								? getDomainJavaType().getMutabilityPlan()
								: ImmutableMutabilityPlan.instance();
					}
				};
			}
		}

		// Series of backward compatible special cases

		if ( Serializable.class.isAssignableFrom( typeImplementorClass ) ) {
			final JavaType<Serializable> jtd = typeConfiguration
					.getJavaTypeRegistry()
					.resolveDescriptor( typeImplementorClass );
			final JdbcType jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( Types.VARBINARY );
			final BasicType<Serializable> resolved = typeConfiguration.getBasicTypeRegistry().resolve( jtd, jdbcType );
			final SerializableType legacyType = new SerializableType( typeImplementorClass );

			return new BasicValue.Resolution<Object>() {
				@Override
				public JdbcMapping getJdbcMapping() {
					return resolved;
				}

				@Override
				public BasicType getLegacyResolvedBasicType() {
					return legacyType;
				}

				@Override
				public JavaType<Object> getDomainJavaType() {
					return (JavaType) resolved.getMappedJavaType();
				}

				@Override
				public JavaType<?> getRelationalJavaType() {
					return resolved.getMappedJavaType();
				}

				@Override
				public JdbcType getJdbcType() {
					return resolved.getJdbcType();
				}

				@Override
				public BasicValueConverter getValueConverter() {
					return resolved.getValueConverter();
				}

				@Override
				public MutabilityPlan<Object> getMutabilityPlan() {
					// a TypeDefinition does not explicitly provide a MutabilityPlan (yet?)
					return resolved.isMutable()
							? getDomainJavaType().getMutabilityPlan()
							: ImmutableMutabilityPlan.instance();
				}
			};
		}

		throw new IllegalArgumentException(
				"Named type [" + typeImplementorClass + "] did not implement BasicType nor UserType"
		);
	}

	private static Object instantiateType(StandardServiceRegistry serviceRegistry,
			String name, Class<?> typeImplementorClass,
			BeanInstanceProducer instanceProducer) {
		if ( Helper.INSTANCE.shouldIgnoreBeanContainer( serviceRegistry ) ) {
			if ( name != null ) {
				return instanceProducer.produceBeanInstance( name, typeImplementorClass );
			}
			else {
				return instanceProducer.produceBeanInstance( typeImplementorClass );
			}
		}
		else {
			final ManagedBean typeBean;
			if ( name != null ) {
				typeBean = serviceRegistry.getService( ManagedBeanRegistry.class )
						.getBean( name, typeImplementorClass, instanceProducer );
			}
			else {
				typeBean = serviceRegistry.getService( ManagedBeanRegistry.class )
						.getBean( typeImplementorClass, instanceProducer );
			}

			return typeBean.getBeanInstance();
		}
	}

	public static BasicValue.Resolution<?> createLocalResolution(
			String name,
			Class typeImplementorClass,
			MutabilityPlan explicitMutabilityPlan,
			Map localTypeParams,
			MetadataBuildingContext buildingContext) {
		name = name + ':' + NAME_COUNTER.getAndIncrement();

		final Properties properties = new Properties();
		properties.putAll( localTypeParams );

		final TypeConfiguration typeConfiguration = buildingContext.getBootstrapContext().getTypeConfiguration();

		return createResolution(
				name,
				typeImplementorClass,
				properties,
				null,
				typeConfiguration.getCurrentBaseSqlTypeIndicators(),
				buildingContext
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof TypeDefinition ) ) {
			return false;
		}

		final TypeDefinition that = (TypeDefinition) o;
		return Objects.equals( this.name, that.name )
				&& Objects.equals( this.typeImplementorClass, that.typeImplementorClass )
				&& Arrays.equals( this.registrationKeys, that.registrationKeys )
				&& Objects.equals( this.parameters, that.parameters );
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + ( typeImplementorClass != null ? typeImplementorClass.hashCode() : 0 );
		result = 31 * result + ( registrationKeys != null ? Arrays.hashCode( registrationKeys ) : 0 );
		result = 31 * result + ( parameters != null ? parameters.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "TypeDefinition{" +
				"name='" + name + '\'' +
				", typeImplementorClass=" + typeImplementorClass +
				", registrationKeys=" + Arrays.toString( registrationKeys ) +
				", parameters=" + parameters +
				'}';
	}
}
