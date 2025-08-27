/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.lang.reflect.ParameterizedType;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.named.ResultMementoBasic;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderBasicValuedConverted;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderBasicValuedStandard;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.ColumnResult;

import static org.hibernate.boot.model.convert.internal.ConverterHelper.extractAttributeConverterParameterizedType;

/**
 * Implementation of {@link ResultMementoBasic} for scalar (basic) results.
 * <p>
 * Ultimately a scalar result is defined as a column name and a {@link BasicType}
 * with the following notes:
 * <ul>
 *     <li>
 *         For JPA mappings, the column name is required.  For {@code hbm.xml}
 *         mappings, it is optional (positional)
 *     </li>
 *     <li>
 *         Ultimately, when reading values, we need the {@link BasicType}.  We
 *         know the {@code BasicType} in a few  different ways:<ul>
 *             <li>
 *                 If we know an explicit {@code Type}, that is used.
 *             </li>
 *             <li>
 *                 If we do not know the {@code Type}, but do know the Java type
 *                 then we determine the {@code BasicType} based on the reported
 *                 SQL type and its known mapping to the specified Java type
 *             </li>
 *             <li>
 *                 If we know neither, we use the reported SQL type and its
 *                 recommended Java type to resolve the {@code BasicType} to use
 *             </li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class ResultMementoBasicStandard implements ResultMementoBasic {
	public final String explicitColumnName;

	private final ResultBuilderBasicValued builder;

	/**
	 * Creation for JPA descriptor
	 */
	public ResultMementoBasicStandard(
			ColumnResult definition,
			ResultSetMappingResolutionContext context) {
		this.explicitColumnName = definition.name();

		final Class<?> definedType = definition.type();
		if ( void.class == definedType ) {
			builder = new CompleteResultBuilderBasicValuedStandard( explicitColumnName, null, null );
		}
		else {
			final TypeConfiguration typeConfiguration = context.getTypeConfiguration();
			final ManagedBeanRegistry managedBeanRegistry = context.getSessionFactory().getManagedBeanRegistry();

			if ( AttributeConverter.class.isAssignableFrom( definedType ) ) {
				@SuppressWarnings("unchecked")
				final Class<? extends AttributeConverter<?, ?>> converterClass =
						(Class<? extends AttributeConverter<?, ?>>) definedType;
				final ManagedBean<? extends AttributeConverter<?,?>> converterBean =
						managedBeanRegistry.getBean( converterClass );
				final JavaType<? extends AttributeConverter<?,?>> converterJtd =
						typeConfiguration.getJavaTypeRegistry().getDescriptor( converterClass );

				final ParameterizedType parameterizedType =
						extractAttributeConverterParameterizedType( converterBean.getBeanClass() );

				builder = new CompleteResultBuilderBasicValuedConverted(
						explicitColumnName,
						converterBean,
						converterJtd,
						determineDomainJavaType( parameterizedType, typeConfiguration.getJavaTypeRegistry() ),
						resolveUnderlyingMapping( parameterizedType, typeConfiguration )
				);
			}
			else {
				final BasicType<?> explicitType;
				final JavaType<?> explicitJavaType;

				// see if this is a registered BasicType...
				final BasicType<Object> registeredBasicType =
						typeConfiguration.getBasicTypeRegistry().getRegisteredType( definedType.getName() );
				if ( registeredBasicType != null ) {
					explicitType = registeredBasicType;
					explicitJavaType = registeredBasicType.getJavaTypeDescriptor();
				}
				else {
					final JavaTypeRegistry jtdRegistry = typeConfiguration.getJavaTypeRegistry();
					final JavaType<Object> registeredJtd = jtdRegistry.getDescriptor( definedType );
					if ( BasicType.class.isAssignableFrom( registeredJtd.getJavaTypeClass() ) ) {
						final ManagedBean<BasicType<?>> typeBean =
								(ManagedBean) managedBeanRegistry.getBean( registeredJtd.getJavaTypeClass() );
						explicitType = typeBean.getBeanInstance();
						explicitJavaType = explicitType.getJavaTypeDescriptor();
					}
					else if ( UserType.class.isAssignableFrom( registeredJtd.getJavaTypeClass() ) ) {
						final ManagedBean<UserType<?>> userTypeBean =
								(ManagedBean) managedBeanRegistry.getBean( registeredJtd.getJavaTypeClass() );
						// todo (6.0) : is this the best approach?  or should we keep a Class<? extends UserType> -> @Type mapping somewhere?
						explicitType = new CustomType<>( userTypeBean.getBeanInstance(), typeConfiguration );
						explicitJavaType = explicitType.getJavaTypeDescriptor();
					}
					else {
						explicitType = null;
						explicitJavaType = jtdRegistry.getDescriptor( definedType );
					}
				}

				builder = new CompleteResultBuilderBasicValuedStandard( explicitColumnName, explicitType, explicitJavaType );
			}
		}
	}

	private BasicJavaType<?> determineDomainJavaType(
			ParameterizedType parameterizedType,
			JavaTypeRegistry jtdRegistry) {
		final java.lang.reflect.Type[] typeParameters = parameterizedType.getActualTypeArguments();
		final java.lang.reflect.Type domainTypeType = typeParameters[ 0 ];
		final Class<?> domainClass = (Class<?>) domainTypeType;

		return (BasicJavaType<?>) jtdRegistry.getDescriptor( domainClass );
	}

	private BasicValuedMapping resolveUnderlyingMapping(
			ParameterizedType parameterizedType,
			TypeConfiguration typeConfiguration) {
		final java.lang.reflect.Type[] typeParameters = parameterizedType.getActualTypeArguments();
		return typeConfiguration.standardBasicTypeForJavaType( (Class) typeParameters[ 1 ] );
	}

	public ResultMementoBasicStandard(
			String explicitColumnName,
			BasicType<?> explicitType,
			ResultSetMappingResolutionContext context) {
		this.explicitColumnName = explicitColumnName;
		this.builder = new CompleteResultBuilderBasicValuedStandard(
				explicitColumnName,
				explicitType,
				explicitType != null
						? explicitType.getJavaTypeDescriptor()
						: null
		);
	}

	@Override
	public ResultBuilderBasicValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return builder;
	}
}
