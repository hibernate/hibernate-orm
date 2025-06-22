/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import org.hibernate.boot.spi.ClassmateContext;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ProvidedInstanceManagedBeanImpl;

import jakarta.persistence.AttributeConverter;

/**
 * ConverterDescriptor implementation for cases where we are handed
 * the AttributeConverter instance to use.
 *
 * @author Steve Ebersole
 */
class InstanceBasedConverterDescriptor<X,Y> extends AbstractConverterDescriptor<X,Y> {
	private final AttributeConverter<X,Y> converterInstance;

	InstanceBasedConverterDescriptor(
			AttributeConverter<X,Y> converterInstance,
			Boolean forceAutoApply,
			ClassmateContext classmateContext) {
		super( ReflectHelper.getClass( converterInstance ), forceAutoApply, classmateContext );
		this.converterInstance = converterInstance;
	}

	@Override
	protected ManagedBean<? extends AttributeConverter<X,Y>>
	createManagedBean(JpaAttributeConverterCreationContext context) {
		return new ProvidedInstanceManagedBeanImpl<>( converterInstance );
	}

}
