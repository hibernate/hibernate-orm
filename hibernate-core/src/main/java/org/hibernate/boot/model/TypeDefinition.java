/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import java.io.Serializable;
import java.sql.Types;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.boot.model.process.internal.UserTypeResolution;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.usertype.UserType;

import static java.util.Collections.emptyMap;
import static org.hibernate.boot.model.process.internal.InferredBasicValueResolver.resolveSqlTypeIndicators;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.mapping.MappingHelper.injectParameters;

/**
 * Models the information pertaining to a custom type definition supplied by the user.
 * Used to delay instantiation of the actual {@link Type} instance.
 * <p>
 * Generally speaking this information would come from annotations
 * ({@link org.hibernate.annotations.Type}) or XML mappings. An alternative way to
 * supply custom types is programmatically, via one of:
 * <ul>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(BasicType)}</li>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(UserType, String[])}</li>
 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyTypes(TypeContributor)}</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author John Verhaeg
 */
public class TypeDefinition implements Serializable {
	public static final AtomicInteger NAME_COUNTER = new AtomicInteger();

	private final String name;
	private final Class<?> typeImplementorClass;
	private final String[] registrationKeys;
	private final Map<String,String> parameters;

	private BasicValue.Resolution<?> reusableResolution;


	public TypeDefinition(
			String name,
			Class<?> typeImplementorClass,
			String[] registrationKeys,
			Map<String,String> parameters) {
		this.name = name;
		this.typeImplementorClass = typeImplementorClass;
		this.registrationKeys = registrationKeys;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public Class<?> getTypeImplementorClass() {
		return typeImplementorClass;
	}

	public String[] getRegistrationKeys() {
		return registrationKeys;
	}

	public Map<String,String> getParameters() {
		return parameters;
	}

	public BasicValue.Resolution<?> resolve(
			Map<?,?> localConfigParameters,
			MetadataBuildingContext context,
			JdbcTypeIndicators indicators) {
		return resolve( localConfigParameters, null, context, indicators );
	}

	public BasicValue.Resolution<?> resolve(
			Map<?,?> localConfigParameters,
			// TODO: why is this parameter ignored??
			MutabilityPlan<?> explicitMutabilityPlan,
			MetadataBuildingContext context,
			JdbcTypeIndicators indicators) {
		if ( isEmpty( localConfigParameters ) ) {
			// we can use the reusable resolution...
			if ( reusableResolution == null ) {
				reusableResolution = createResolution( this.name, emptyMap(), indicators, context );
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

	private static <T> BasicValue.Resolution<T> createResolution(
			String name,
			Class<T> typeImplementorClass,
			Map<?,?> parameters,
			Map<?,?> usageSiteProperties,
			JdbcTypeIndicators indicators,
			MetadataBuildingContext context) {
		final var bootstrapContext = context.getBootstrapContext();
		final var typeConfiguration = bootstrapContext.getTypeConfiguration();

		final boolean isKnownType =
				Type.class.isAssignableFrom( typeImplementorClass )
				|| UserType.class.isAssignableFrom( typeImplementorClass );
		// support for AttributeConverter would be nice too
		if ( isKnownType ) {
			final T typeInstance =
					instantiateType( bootstrapContext.getServiceRegistry(), context.getBuildingOptions(),
							name, typeImplementorClass, bootstrapContext.getCustomTypeProducer() );

			if ( typeInstance instanceof TypeConfigurationAware configurationAware ) {
				configurationAware.setTypeConfiguration( typeConfiguration );
			}

			final var combinedTypeParameters = new Properties();
			if ( parameters!=null ) {
				combinedTypeParameters.putAll( parameters );
			}
			if ( usageSiteProperties!=null ) {
				combinedTypeParameters.putAll( usageSiteProperties );
			}

			injectParameters( typeInstance, combinedTypeParameters );

			if ( typeInstance instanceof UserType ) {
				@SuppressWarnings("unchecked")
				final var userType = (UserType<T>) typeInstance;
				final var customType = new CustomType<>( userType, typeConfiguration );
				return new UserTypeResolution<>( customType, null, combinedTypeParameters );
			}

			if ( typeInstance instanceof BasicType ) {
				@SuppressWarnings("unchecked")
				final BasicType<T> resolvedBasicType = (BasicType<T>) typeInstance;
				return new BasicValue.Resolution<>() {
					@Override
					public JdbcMapping getJdbcMapping() {
						return resolvedBasicType;
					}

					@Override
					public BasicType<T> getLegacyResolvedBasicType() {
						return resolvedBasicType;
					}

					@Override
					public Properties getCombinedTypeParameters() {
						return combinedTypeParameters;
					}

					@Override
					public JavaType<T> getDomainJavaType() {
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
					public BasicValueConverter<T,?> getValueConverter() {
						return resolvedBasicType.getValueConverter();
					}

					@Override
					public MutabilityPlan<T> getMutabilityPlan() {
						// a TypeDefinition does not explicitly provide a MutabilityPlan (yet?)
						return resolvedBasicType.isMutable()
								? getDomainJavaType().getMutabilityPlan()
								: ImmutableMutabilityPlan.instance();
					}
				};
			}
		}

		// Series of backward compatible special cases
		return resolveLegacyCases( typeImplementorClass, indicators, typeConfiguration );
	}

	private static <T> BasicValue.Resolution<T> resolveLegacyCases(
			Class<T> typeImplementorClass, JdbcTypeIndicators indicators, TypeConfiguration typeConfiguration) {
		return createBasicTypeResolution( getLegacyType( typeImplementorClass ),
				typeImplementorClass, indicators, typeConfiguration );
	}

	private static <T> BasicType<T> getLegacyType(Class<T> typeImplementorClass) {
		if ( Serializable.class.isAssignableFrom( typeImplementorClass ) ) {
			return new SerializableType( typeImplementorClass );
		}
		else if ( typeImplementorClass.isInterface() ) {
			return (BasicType<T>) new JavaObjectType();
		}
		else {
			throw new IllegalArgumentException( "Named type [" + typeImplementorClass
												+ "] did not implement BasicType nor UserType" );
		}
	}

	private static <T> BasicValue.Resolution<T> createBasicTypeResolution(
			BasicType<T> type,
			Class<T> typeImplementorClass,
			JdbcTypeIndicators indicators,
			TypeConfiguration typeConfiguration) {
		final var jtd = typeConfiguration.getJavaTypeRegistry().resolveDescriptor( typeImplementorClass );
		final var jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( Types.VARBINARY );
		final var basicType = typeConfiguration.getBasicTypeRegistry().resolve( jtd, jdbcType );
		final var resolved = resolveSqlTypeIndicators( indicators, basicType, jtd );

		return new BasicValue.Resolution<>() {
			@Override
			public JdbcMapping getJdbcMapping() {
				return resolved;
			}

			@Override
			public BasicType<T> getLegacyResolvedBasicType() {
				return type;
			}

			@Override
			public JavaType<T> getDomainJavaType() {
				return resolved.getMappedJavaType();
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
			public BasicValueConverter<T,?> getValueConverter() {
				return resolved.getValueConverter();
			}

			@Override
			public MutabilityPlan<T> getMutabilityPlan() {
				// a TypeDefinition does not explicitly provide a MutabilityPlan (yet?)
				return resolved.isMutable()
						? getDomainJavaType().getMutabilityPlan()
						: ImmutableMutabilityPlan.instance();
			}
		};
	}

	private static <T> T instantiateType(
			StandardServiceRegistry serviceRegistry,
			MetadataBuildingOptions buildingOptions,
			String name,
			Class<T> typeImplementorClass,
			BeanInstanceProducer instanceProducer) {
		if ( !buildingOptions.isAllowExtensionsInCdi() ) {
			return name != null
					? instanceProducer.produceBeanInstance( name, typeImplementorClass )
					: instanceProducer.produceBeanInstance( typeImplementorClass );
		}
		else {
			final var beanRegistry = serviceRegistry.requireService( ManagedBeanRegistry.class );
			final var typeBean = name != null
					? beanRegistry.getBean( name, typeImplementorClass, instanceProducer )
					: beanRegistry.getBean( typeImplementorClass, instanceProducer );
			return typeBean.getBeanInstance();
		}
	}

	public static BasicValue.Resolution<?> createLocalResolution(
			String name,
			Class<?> typeImplementorClass,
			Map<?,?> localTypeParams,
			MetadataBuildingContext buildingContext) {
		return createResolution(
				name + ':' + NAME_COUNTER.getAndIncrement(),
				typeImplementorClass,
				localTypeParams,
				null,
				buildingContext.getBootstrapContext().getTypeConfiguration()
						.getCurrentBaseSqlTypeIndicators(),
				buildingContext
		);
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !(object instanceof TypeDefinition that) ) {
			return false;
		}
		else {
			return Objects.equals( this.name, that.name )
				&& Objects.equals( this.typeImplementorClass, that.typeImplementorClass )
				&& Arrays.equals( this.registrationKeys, that.registrationKeys )
				&& Objects.equals( this.parameters, that.parameters );
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, typeImplementorClass, Arrays.hashCode( registrationKeys ), parameters );
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
