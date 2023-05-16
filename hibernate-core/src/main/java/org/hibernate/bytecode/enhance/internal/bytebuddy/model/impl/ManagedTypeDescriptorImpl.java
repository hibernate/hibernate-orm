/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy.model.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ClassDetails;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ManagedTypeDescriptor;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ManagedTypeDescriptorRegistry;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.ManagedTypeModelContext;
import org.hibernate.bytecode.enhance.internal.bytebuddy.model.PersistentAttribute;
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * @author Steve Ebersole
 */
public class ManagedTypeDescriptorImpl implements ManagedTypeDescriptor {
	private final ClassDetails classDetails;
	private final LinkedHashMap<String, PersistentAttribute> attributeMap;

	private final ManagedTypeDescriptor superTypeDescriptor;

	public ManagedTypeDescriptorImpl(ClassDetails classDetails, List<PersistentAttribute> attributes, ManagedTypeModelContext context) {
		this.classDetails = classDetails;
		this.attributeMap = CollectionHelper.linkedMapOfSize( attributes.size() );

		this.superTypeDescriptor = classDetails.getSuperType() == null
				? null
				: context.getDescriptorRegistry().getDescriptor( classDetails.getSuperType() );
	}

	@Override
	public ClassDetails getClassDetails() {
		return classDetails;
	}

	public ManagedTypeDescriptor getSuperTypeDescriptor() {
		return superTypeDescriptor;
	}

	@Override
	public Map<String, PersistentAttribute> getPersistentAttributes() {
		return attributeMap;
	}
}
