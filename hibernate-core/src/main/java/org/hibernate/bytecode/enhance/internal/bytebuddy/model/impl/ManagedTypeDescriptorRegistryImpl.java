/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ManagedTypeDescriptor;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ManagedTypeDescriptorRegistry;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ManagedTypeModelContext;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ModelSourceHelper;

/**
 * @author Steve Ebersole
 */
public class ManagedTypeDescriptorRegistryImpl implements ManagedTypeDescriptorRegistry {
	private final Map<String,ManagedTypeDescriptor> managedTypeDescriptorMap = new LinkedHashMap<>();
	private final ManagedTypeModelContext modelContext;

	public ManagedTypeDescriptorRegistryImpl(ManagedTypeModelContext modelContext) {
		this.modelContext = modelContext;
	}

	@Override
	public ManagedTypeDescriptor findDescriptor(String name) {
		return managedTypeDescriptorMap.get( name );
	}

	public ManagedTypeDescriptor resolveDescriptor(String name) {
		final ManagedTypeDescriptor existing = findDescriptor( name );
		if ( existing != null ) {
			return existing;
		}

		final ClassDetails classDetails = modelContext.getModelProcessingContext().getClassDetailsRegistry().resolveClassDetails( name );
		return null;

//		final ManagedTypeDescriptor managedTypeDescriptor = ModelSourceHelper.buildManagedTypeDescriptor(
//				classDetails,
//				null,
//				modelContext
//		);
//		managedTypeDescriptorMap.put( name, managedTypeDescriptor );
//		return managedTypeDescriptor;
	}
}
