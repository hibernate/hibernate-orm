/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ManagedTypeDescriptorRegistry;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ManagedTypeModelContext;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelProcessingContext;

/**
 * @author Steve Ebersole
 */
public class ManagedTypeModelContextImpl implements ManagedTypeModelContext {
	private final ManagedTypeDescriptorRegistry managedTypeDescriptorRegistry;
	private final ModelProcessingContext modelProcessingContext;

	public ManagedTypeModelContextImpl(ModelProcessingContext modelProcessingContext) {
		this.managedTypeDescriptorRegistry = new ManagedTypeDescriptorRegistryImpl( this );
		this.modelProcessingContext = modelProcessingContext;
	}

	@Override
	public ManagedTypeDescriptorRegistry getDescriptorRegistry() {
		return managedTypeDescriptorRegistry;
	}

	@Override
	public ModelProcessingContext getModelProcessingContext() {
		return modelProcessingContext;
	}
}
