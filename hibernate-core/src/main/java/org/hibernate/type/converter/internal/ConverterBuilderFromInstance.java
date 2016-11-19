/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.converter.internal;

import javax.persistence.AttributeConverter;

import org.hibernate.type.converter.spi.ConverterBuilder;
import org.hibernate.type.converter.spi.ConverterBuildingContext;

/**
 * ConverterBuilder implementation for cases where we have an instance of the
 * converter already.
 *
 * @author Steve Ebersole
 */
public class ConverterBuilderFromInstance<O,R> implements ConverterBuilder<O,R> {
	private final AttributeConverter<O, R> converter;

	public ConverterBuilderFromInstance(AttributeConverter<O, R> converter) {
		this.converter = converter;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends AttributeConverter<O, R>> getImplementationClass() {
		return (Class<? extends AttributeConverter<O, R>>) converter.getClass();
	}

	@Override
	public AttributeConverter<O, R> buildAttributeConverter(ConverterBuildingContext context) {
		return converter;
	}
}