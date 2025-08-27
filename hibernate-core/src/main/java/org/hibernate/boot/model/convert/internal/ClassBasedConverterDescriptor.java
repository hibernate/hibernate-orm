/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import org.hibernate.boot.spi.ClassmateContext;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.resource.beans.spi.ManagedBean;

import jakarta.persistence.AttributeConverter;

/**
 * ConverterDescriptor implementation for cases where we know the
 * AttributeConverter Class.  This is the normal case.
 *
 * @author Steve Ebersole
 */
class ClassBasedConverterDescriptor<X,Y> extends AbstractConverterDescriptor<X,Y> {

	private final boolean overrideable;

	ClassBasedConverterDescriptor(
			Class<? extends AttributeConverter<X,Y>> converterClass,
			Boolean forceAutoApply,
			ClassmateContext classmateContext,
			boolean overrideable) {
		super( converterClass, forceAutoApply, classmateContext );
		this.overrideable = overrideable;
	}

	@Override
	public boolean overrideable() {
		return overrideable;
	}

	@Override
	protected ManagedBean<? extends AttributeConverter<X,Y>>
	createManagedBean(JpaAttributeConverterCreationContext context) {
		return context.getManagedBeanRegistry().getBean( getAttributeConverterClass() );
	}
}
