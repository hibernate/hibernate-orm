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
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * Models the information pertaining to a custom type definition supplied by the user.  Used
 * to delay instantiation of the actual {@link Type} instance.
 *
 * Generally speaking this information would come from annotations
 * ({@link org.hibernate.annotations.TypeDef}) or XML mappings.  An alternative form of
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
			JdbcTypeDescriptorIndicators indicators) {
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
			JdbcTypeDescriptorIndicators indicators,
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
			JdbcTypeDescriptorIndicators indicators,
			MetadataBuildingContext context) {
		final BootstrapContext bootstrapContext = context.getBootstrapContext();
		final TypeConfiguration typeConfiguration = bootstrapContext.getTypeConfiguration();
		final BeanInstanceProducer instanceProducer = bootstrapContext.getBeanInstanceProducer();
		final boolean isKnownType = Type.class.isAssignableFrom( typeImplementorClass )
				|| UserType.class.isAssignableFrom( typeImplementorClass );

		// support for AttributeConverter would be nice too
		if ( isKnownType ) {
			final ManagedBean typeBean;
			if ( name != null ) {
				typeBean = bootstrapContext
						.getServiceRegistry()
						.getService( ManagedBeanRegistry.class )
						.getBean( name, typeImplementorClass, instanceProducer );
			}
			else {
				typeBean = bootstrapContext
						.getServiceRegistry()
						.getService( ManagedBeanRegistry.class )
						.getBean( typeImplementorClass, instanceProducer );
			}

			final Object typeInstance = typeBean.getBeanInstance();

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

			if ( typeInstance instanceof ParameterizedType ) {
				if ( combinedTypeParameters != null ) {
					( (ParameterizedType) typeInstance ).setParameterValues( combinedTypeParameters );
				}
			}

			if ( typeInstance instanceof UserType ) {
				final UserType userType = (UserType) typeInstance;
				final CustomType customType = new CustomType( userType, typeConfiguration );

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
					public JavaTypeDescriptor<Object> getDomainJavaDescriptor() {
						return resolvedBasicType.getMappedJavaTypeDescriptor();
					}

					@Override
					public JavaTypeDescriptor<?> getRelationalJavaDescriptor() {
						return resolvedBasicType.getMappedJavaTypeDescriptor();
					}

					@Override
					public JdbcTypeDescriptor getJdbcTypeDescriptor() {
						return resolvedBasicType.getJdbcTypeDescriptor();
					}

					@Override
					public BasicValueConverter getValueConverter() {
						return null;
					}

					@Override
					public MutabilityPlan<Object> getMutabilityPlan() {
						// a TypeDefinition does not explicitly provide a MutabilityPlan (yet?)
						return resolvedBasicType.isMutable()
								? getDomainJavaDescriptor().getMutabilityPlan()
								: ImmutableMutabilityPlan.instance();
					}
				};
			}
		}

		// Series of backward compatible special cases

		if ( Serializable.class.isAssignableFrom( typeImplementorClass ) ) {
			final JavaTypeDescriptor<Serializable> jtd = typeConfiguration
					.getJavaTypeDescriptorRegistry()
					.resolveDescriptor( typeImplementorClass );
			final JdbcTypeDescriptor jdbcType = typeConfiguration.getJdbcTypeDescriptorRegistry().getDescriptor( Types.VARBINARY );
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
				public JavaTypeDescriptor<Object> getDomainJavaDescriptor() {
					return (JavaTypeDescriptor) resolved.getMappedJavaTypeDescriptor();
				}

				@Override
				public JavaTypeDescriptor<?> getRelationalJavaDescriptor() {
					return resolved.getMappedJavaTypeDescriptor();
				}

				@Override
				public JdbcTypeDescriptor getJdbcTypeDescriptor() {
					return resolved.getJdbcTypeDescriptor();
				}

				@Override
				public BasicValueConverter getValueConverter() {
					return null;
				}

				@Override
				public MutabilityPlan<Object> getMutabilityPlan() {
					// a TypeDefinition does not explicitly provide a MutabilityPlan (yet?)
					return resolved.isMutable()
							? getDomainJavaDescriptor().getMutabilityPlan()
							: ImmutableMutabilityPlan.instance();
				}
			};
		}

		throw new IllegalArgumentException(
				"Named type [" + typeImplementorClass + "] did not implement BasicType nor UserType"
		);
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
