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
public class ClassBasedConverterDescriptor extends AbstractConverterDescriptor {
	public ClassBasedConverterDescriptor(
			Class<? extends AttributeConverter<?,?>> converterClass,
			ClassmateContext classmateContext) {
		super( converterClass, null, classmateContext );
	}

	public ClassBasedConverterDescriptor(
			Class<? extends AttributeConverter<?,?>> converterClass,
			Boolean forceAutoApply,
			ClassmateContext classmateContext) {
		super( converterClass, forceAutoApply, classmateContext );
	}

	@Override
	protected ManagedBean<? extends AttributeConverter<?, ?>> createManagedBean(JpaAttributeConverterCreationContext context) {
		return context.getManagedBeanRegistry().getBean( getAttributeConverterClass() );
	}
}
