/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.lang.reflect.ParameterizedType;
import java.util.function.Consumer;

import org.hibernate.boot.model.convert.internal.ConverterHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.named.ResultMementoBasic;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.complete.CompleteResultBuilderBasicValuedConverted;
import org.hibernate.query.results.complete.CompleteResultBuilderBasicValuedStandard;
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

		final SessionFactoryImplementor sessionFactory = context.getSessionFactory();
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

		final Class<?> definedType = definition.type();

		if ( void.class == definedType ) {
			builder = new CompleteResultBuilderBasicValuedStandard( explicitColumnName, null, null );
		}
		else if ( AttributeConverter.class.isAssignableFrom( definedType ) ) {
			final Class<? extends AttributeConverter<?, ?>> converterClass =
					(Class<? extends AttributeConverter<?, ?>>) definedType;
			final ManagedBean<? extends AttributeConverter<?,?>> converterBean = sessionFactory.getServiceRegistry()
					.requireService( ManagedBeanRegistry.class )
					.getBean( converterClass );
			final JavaType<? extends AttributeConverter<?,?>> converterJtd = typeConfiguration
					.getJavaTypeRegistry()
					.getDescriptor( converterClass );

			final ParameterizedType parameterizedType = ConverterHelper.extractAttributeConverterParameterizedType( converterBean.getBeanClass() );

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
			final BasicType<Object> registeredBasicType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( definition.type().getName() );
			if ( registeredBasicType != null ) {
				explicitType = registeredBasicType;
				explicitJavaType = registeredBasicType.getJavaTypeDescriptor();
			}
			else {
				final JavaTypeRegistry jtdRegistry = typeConfiguration.getJavaTypeRegistry();
				final JavaType<Object> registeredJtd = jtdRegistry.getDescriptor( definition.type() );
				final ManagedBeanRegistry beanRegistry =
						sessionFactory.getServiceRegistry().requireService( ManagedBeanRegistry.class );
				if ( BasicType.class.isAssignableFrom( registeredJtd.getJavaTypeClass() ) ) {
					final ManagedBean<BasicType<?>> typeBean =
							(ManagedBean) beanRegistry.getBean( registeredJtd.getJavaTypeClass() );
					explicitType = typeBean.getBeanInstance();
					explicitJavaType = explicitType.getJavaTypeDescriptor();
				}
				else if ( UserType.class.isAssignableFrom( registeredJtd.getJavaTypeClass() ) ) {
					final ManagedBean<UserType<?>> userTypeBean =
							(ManagedBean) beanRegistry.getBean( registeredJtd.getJavaTypeClass() );
					// todo (6.0) : is this the best approach?  or should we keep a Class<? extends UserType> -> @Type mapping somewhere?
					explicitType = new CustomType<>( userTypeBean.getBeanInstance(), typeConfiguration );
					explicitJavaType = explicitType.getJavaTypeDescriptor();
				}
				else {
					explicitType = null;
					explicitJavaType = jtdRegistry.getDescriptor( definition.type() );
				}
			}

			builder = new CompleteResultBuilderBasicValuedStandard( explicitColumnName, explicitType, explicitJavaType );
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
