/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.sql.ColumnMapping;
import org.hibernate.SessionFactory;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.named.ResultMementoBasic;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderBasicValuedConverted;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderBasicValuedStandard;
import org.hibernate.query.results.spi.ResultBuilderBasicValued;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

import java.lang.reflect.Type;
import java.util.function.Consumer;

import static org.hibernate.internal.util.GenericsHelper.typeArguments;

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
		explicitColumnName = definition.name();

		final var definedType = definition.type();
		if ( void.class == definedType ) {
			builder = new CompleteResultBuilderBasicValuedStandard( explicitColumnName, null, null );
		}
		else {
			final var typeConfiguration = context.getTypeConfiguration();
			final var managedBeanRegistry = context.getSessionFactory().getManagedBeanRegistry();
			builder = resolveBuilder( explicitColumnName, definedType, typeConfiguration, managedBeanRegistry );
		}
	}

	public String getColumnName() {
		return explicitColumnName;
	}



	private static <T> ResultBuilderBasicValued resolveBuilder(
			String columnName,
			Class<T> definedType,
			TypeConfiguration typeConfiguration,
			ManagedBeanRegistry managedBeanRegistry) {

		if ( AttributeConverter.class.isAssignableFrom( definedType ) ) {
			@SuppressWarnings("unchecked")
			final var converterClass = (Class<? extends AttributeConverter<?, ?>>) definedType;
			final var converterBean = managedBeanRegistry.getBean( converterClass );
			final var converterJtd = typeConfiguration.getJavaTypeRegistry().resolveDescriptor( converterClass );

			final var typeArguments = typeArguments( AttributeConverter.class, converterBean.getBeanClass() );

			//noinspection rawtypes,unchecked
			return new CompleteResultBuilderBasicValuedConverted(
					columnName,
					converterBean,
					converterJtd,
					determineDomainJavaType( typeArguments, typeConfiguration.getJavaTypeRegistry() ),
					resolveUnderlyingMapping( typeArguments, typeConfiguration )
			);
		}
		else {
			final BasicType<?> explicitType;
			final JavaType<?> explicitJavaType;

			// see if this is a registered BasicType...
			final var registeredBasicType = typeConfiguration.getBasicTypeRegistry()
					.getRegisteredType( definedType.getName() );
			if ( registeredBasicType != null ) {
				explicitType = registeredBasicType;
				explicitJavaType = registeredBasicType.getJavaTypeDescriptor();
			}
			else {
				final var jtdRegistry = typeConfiguration.getJavaTypeRegistry();
				final var registeredJtd = jtdRegistry.resolveDescriptor( definedType );
				if ( BasicType.class.isAssignableFrom( registeredJtd.getJavaTypeClass() ) ) {
					//noinspection unchecked
					final var typeBean =
							(ManagedBean<BasicType<?>>)
									managedBeanRegistry.getBean( registeredJtd.getJavaTypeClass() );
					explicitType = typeBean.getBeanInstance();
					explicitJavaType = explicitType.getJavaTypeDescriptor();
				}
				else if ( UserType.class.isAssignableFrom( registeredJtd.getJavaTypeClass() ) ) {
					//noinspection unchecked
					final var userTypeBean =
							(ManagedBean<UserType<?>>)
									managedBeanRegistry.getBean( registeredJtd.getJavaTypeClass() );
					// todo (6.0) : is this the best approach?  or should we keep a Class<? extends UserType> -> @Type mapping somewhere?
					explicitType = new CustomType<>( userTypeBean.getBeanInstance(), typeConfiguration );
					explicitJavaType = explicitType.getJavaTypeDescriptor();
				}
				else {
					explicitType = null;
					explicitJavaType = jtdRegistry.resolveDescriptor( definedType );
				}
			}

			return new CompleteResultBuilderBasicValuedStandard( columnName, explicitType, explicitJavaType );
		}
	}

	private static BasicJavaType<?> determineDomainJavaType(
			Type[] typeArguments,
			JavaTypeRegistry jtdRegistry) {
		final var domainClass = (Class<?>) typeArguments[0];
		return (BasicJavaType<?>) jtdRegistry.resolveDescriptor( domainClass );
	}

	private static BasicValuedMapping resolveUnderlyingMapping(
			Type[] typeArguments,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.standardBasicTypeForJavaType( typeArguments[1] );
	}

	private ResultMementoBasicStandard(String explicitColumnName, ResultBuilderBasicValued builder) {
		this.explicitColumnName = explicitColumnName;
		this.builder = builder;
	}

	public ResultMementoBasicStandard(
			String explicitColumnName,
			BasicType<?> explicitType) {
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

	@Override
	public <R> ColumnMapping<R> toJpaMapping(SessionFactory sessionFactory) {
		return toJpaMappingElement( sessionFactory );
	}

	@Override
	public <R> ColumnMapping<R> toJpaMappingElement(SessionFactory sessionFactory) {
		//noinspection unchecked
		return ColumnMapping.of( explicitColumnName, (Class<R>) getResultJavaType() );
	}

	@Override
	public Class<?> getResultJavaType() {
		return builder.getJavaType();
	}
}
