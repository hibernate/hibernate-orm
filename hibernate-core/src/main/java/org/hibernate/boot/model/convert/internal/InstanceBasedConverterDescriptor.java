/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.convert.internal;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ProvidedInstanceManagedBeanImpl;

import jakarta.persistence.AttributeConverter;

/**
 * ConverterDescriptor implementation for cases where we are handed
 * the AttributeConverter instance to use.
 *
 * @author Steve Ebersole
 */
public class InstanceBasedConverterDescriptor extends AbstractConverterDescriptor {
	private final AttributeConverter<?,?> converterInstance;

	public InstanceBasedConverterDescriptor(
			AttributeConverter<?,?> converterInstance,
			ClassmateContext classmateContext) {
		this( converterInstance, null, classmateContext );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public InstanceBasedConverterDescriptor(
			AttributeConverter<?,?> converterInstance,
			Boolean forceAutoApply,
			ClassmateContext classmateContext) {
		super( (Class) converterInstance.getClass(), forceAutoApply, classmateContext );
		this.converterInstance = converterInstance;
	}

	@Override
	protected ManagedBean<? extends AttributeConverter<?, ?>> createManagedBean(JpaAttributeConverterCreationContext context) {
		return new ProvidedInstanceManagedBeanImpl<>( converterInstance );
	}

}
