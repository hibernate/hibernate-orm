/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.query.sqm.sql.internal.InstantiationException;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.sql.results.graph.instantiation.internal.InstantiationHelper.findField;
import static org.hibernate.sql.results.graph.instantiation.internal.InstantiationHelper.propertyMatches;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationAssemblerInjectionImpl<T> implements DomainResultAssembler<T> {
	private final JavaType<T> target;
	private final List<BeanInjection> beanInjections;

	public DynamicInstantiationAssemblerInjectionImpl(
			JavaType<T> target,
			List<ArgumentReader<?>> argumentReaders) {
		this.target = target;
		final Class<?> targetJavaType = target.getJavaTypeClass();
		final List<BeanInjection> beanInjections = new ArrayList<>( argumentReaders.size() );
		BeanInfoHelper.visitBeanInfo(
				targetJavaType,
				beanInfo -> {
					for ( ArgumentReader<?> argumentReader : argumentReaders ) {
						beanInjections.add( injection( beanInfo, argumentReader, targetJavaType ) );
					}
				}
		);

		if ( argumentReaders.size() != beanInjections.size() ) {
			throw new IllegalStateException( "The number of readers did not match the number of injections" );
		}
		this.beanInjections = beanInjections;
	}

	private DynamicInstantiationAssemblerInjectionImpl(List<BeanInjection> beanInjections, JavaType<T> target) {
		this.target = target;
		this.beanInjections = beanInjections;
	}

	private static BeanInjection injection(BeanInfo beanInfo, ArgumentReader<?> argument, Class<?> targetJavaType) {
		final Class<?> argType = argument.getAssembledJavaType().getJavaTypeClass();
		final String alias = argument.getAlias();

		// see if we can find a property with the given name...
		for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
			if ( propertyMatches( alias, argType, propertyDescriptor ) ) {
				final Method setter = propertyDescriptor.getWriteMethod();
				setter.setAccessible(true);
				return new BeanInjection( new BeanInjectorSetter<>( setter ), argument );
			}
		}

		// see if we can find a Field with the given name...
		final Field field = findField( targetJavaType, alias, argType );
		if ( field != null ) {
			return new BeanInjection( new BeanInjectorField<>( field ), argument );
		}
		else {
			throw new InstantiationException(
					"Cannot set field '" + alias + "' to instantiate '" + targetJavaType.getName() + "'"
			);
		}
	}

	@Override
	public JavaType<T> getAssembledJavaType() {
		return target;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T assemble(RowProcessingState rowProcessingState) {
		final T result;
		try {
			final Constructor<T> constructor = target.getJavaTypeClass().getDeclaredConstructor();
			constructor.setAccessible( true );
			result = constructor.newInstance();
		}
		catch ( NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException
				| java.lang.InstantiationException e ) {
			throw new InstantiationException( "Error instantiating class '"
					+ target.getTypeName() + "' using default constructor: " + e.getMessage(), e );
		}
		for ( BeanInjection beanInjection : beanInjections ) {
			final Object assembled = beanInjection.getValueAssembler().assemble( rowProcessingState );
			beanInjection.getBeanInjector().inject( result, assembled );
		}
		return result;
	}

	@Override
	public <X> void forEachResultAssembler(BiConsumer<Initializer<?>, X> consumer, X arg) {
		for ( BeanInjection beanInjection : beanInjections ) {
			beanInjection.getValueAssembler().forEachResultAssembler( consumer, arg );
		}
	}
}
