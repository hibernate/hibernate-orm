/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert;

import jakarta.persistence.AttributeConverter;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.convert.internal.JpaAttributeConverterImpl;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Factory for converter instances
 *
 * @author Steve Ebersole
 */
public class Converters {
	public static <O,R> BasicValueConverter<O,R> jpaAttributeConverter(
			JavaType<R> relationalJtd,
			JavaType<O> domainJtd,
			Class<? extends AttributeConverter<O,R>> converterClass,
			SessionFactory factory) {
		final SessionFactoryImplementor sfi = (SessionFactoryImplementor) factory;

		final ManagedBeanRegistry beanRegistry = sfi.getServiceRegistry().getService( ManagedBeanRegistry.class );
		final ManagedBean<? extends AttributeConverter<O, R>> converterBean = beanRegistry.getBean( converterClass );

		final TypeConfiguration typeConfiguration = sfi.getTypeConfiguration();
		final JavaTypeRegistry jtdRegistry = typeConfiguration.getJavaTypeRegistry();
		final JavaType<? extends AttributeConverter<O, R>> converterJtd = jtdRegistry.getDescriptor( converterClass );

		return new JpaAttributeConverterImpl<>( converterBean, converterJtd, domainJtd, relationalJtd );
	}

	private Converters() {
	}
}
