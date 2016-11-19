/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.converter.internal;

import javax.persistence.AttributeConverter;

import org.hibernate.resource.cdi.spi.ManagedBean;
import org.hibernate.resource.cdi.spi.ManagedBeanRegistry;
import org.hibernate.type.converter.spi.ConverterBuilder;
import org.hibernate.type.converter.spi.ConverterBuildingContext;

/**
 * @author Steve Ebersole
 */
public class ConverterBuilderFromClass<O,R> implements ConverterBuilder<O,R> {
	private final Class<? extends AttributeConverter<O, R>> converterClass;

	public ConverterBuilderFromClass(Class<? extends AttributeConverter<O, R>> converterClass) {
		this.converterClass = converterClass;
	}

	@Override
	public Class<? extends AttributeConverter<O, R>> getImplementationClass() {
		return converterClass;
	}

	@Override
	public AttributeConverter<O, R> buildAttributeConverter(ConverterBuildingContext context) {
		final ManagedBean<? extends AttributeConverter<O,R>> converterBean = context.getServiceRegistry()
				.getService( ManagedBeanRegistry.class )
				.getBean( converterClass );

		return new AttributeConverter<O, R>() {
			@Override
			public R convertToDatabaseColumn(O attribute) {
				return converterBean.getBeanInstance().convertToDatabaseColumn( attribute );
			}

			@Override
			public O convertToEntityAttribute(R dbData) {
				return converterBean.getBeanInstance().convertToEntityAttribute( dbData );
			}
		};
	}
}