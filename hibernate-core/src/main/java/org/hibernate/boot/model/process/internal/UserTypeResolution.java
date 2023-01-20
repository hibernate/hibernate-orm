/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.Properties;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.MappingHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
public class UserTypeResolution<J, T extends UserType<J>> implements BasicValue.Resolution<J> {
	private static int COUNTER;

	private final Class<T> customTypeImplementor;

	private final BeanInstanceProducer instanceProducer;
	private final ServiceRegistry serviceRegistry;
	private final TypeConfiguration typeConfiguration;
	private final Supplier<DynamicParameterizedType.ParameterType> dynamicParameterizedTypeCreator;

	/**
	 * We need this for the way envers interprets the boot-model
	 * and builds its own :(
	 */
	private final Properties combinedTypeParameters;

	private boolean resolved = false;
	private CustomType<J> userTypeAdapter;
	private MutabilityPlan<J> mutabilityPlan;

	public UserTypeResolution(
			Class<T> customTypeImplementor,
			BeanInstanceProducer instanceProducer,
			ServiceRegistry serviceRegistry,
			TypeConfiguration typeConfiguration,
			Supplier<DynamicParameterizedType.ParameterType> dynamicParameterizedTypeCreator,
			Properties combinedTypeParameters) {
		this.customTypeImplementor = customTypeImplementor;
		this.instanceProducer = instanceProducer;
		this.serviceRegistry = serviceRegistry;
		this.typeConfiguration = typeConfiguration;
		this.dynamicParameterizedTypeCreator = dynamicParameterizedTypeCreator;
		this.combinedTypeParameters = combinedTypeParameters;
	}

	public boolean isResolved() {
		return resolved;
	}

	private void resolve() {
		if ( !resolved ) {
			assert userTypeAdapter == null;
			assert mutabilityPlan == null;

			final ManagedBean<T> typeBean;
			final ManagedBeanRegistry beanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );
			if ( combinedTypeParameters.isEmpty() ) {
				typeBean = beanRegistry.getBean( customTypeImplementor, instanceProducer );
			}
			else {
				final String name = customTypeImplementor.getName() + COUNTER++;
				typeBean = beanRegistry.getBean( name, customTypeImplementor, instanceProducer );
			}

			final T typeInstance = typeBean.getBeanInstance();

			if ( typeInstance instanceof TypeConfigurationAware ) {
				( (TypeConfigurationAware) typeInstance ).setTypeConfiguration( typeConfiguration );
			}

			if ( typeInstance instanceof DynamicParameterizedType ) {
				if ( Boolean.parseBoolean( combinedTypeParameters.getProperty( DynamicParameterizedType.IS_DYNAMIC ) ) ) {
					if ( combinedTypeParameters.get( DynamicParameterizedType.PARAMETER_TYPE ) == null ) {
						if ( dynamicParameterizedTypeCreator == null ) {
							throw new HibernateException( "UserType implements DynamicParameterizedType, but cannot create the ParameterType" );
						}
						final DynamicParameterizedType.ParameterType parameterType = dynamicParameterizedTypeCreator.get();
						combinedTypeParameters.put( DynamicParameterizedType.PARAMETER_TYPE, parameterType );
					}
				}
			}

			MappingHelper.injectParameters( typeInstance, combinedTypeParameters );

			userTypeAdapter = new CustomType<>( typeInstance, typeConfiguration );
			mutabilityPlan = new UserTypeMutabilityPlanAdapter<>( typeInstance );

			resolved = true;
		}
	}

	@Override
	public BasicType<J> getLegacyResolvedBasicType() {
		resolve();
		return userTypeAdapter;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		resolve();
		return userTypeAdapter.getJdbcMapping();
	}

	@Override
	public JavaType<J> getDomainJavaType() {
		resolve();
		return userTypeAdapter.getMappedJavaType();
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		resolve();
		return userTypeAdapter.getJdbcJavaType();
	}

	@Override
	public JdbcType getJdbcType() {
		resolve();
		return userTypeAdapter.getJdbcType();
	}

	@Override
	public BasicValueConverter<J,?> getValueConverter() {
		// any conversion is inherent to the user-type
		return null;
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		resolve();
		return mutabilityPlan;
	}

	@Override
	public Properties getCombinedTypeParameters() {
		return combinedTypeParameters;
	}
}
