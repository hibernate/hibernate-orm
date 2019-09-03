/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class AbstractManagedMappingType implements ManagedMappingType {
	private final JavaTypeDescriptor mappedJavaTypeDescriptor;

	private SortedSet<AttributeMapping> attributeMappings;

	public AbstractManagedMappingType(JavaTypeDescriptor mappedJavaTypeDescriptor) {
		this.mappedJavaTypeDescriptor = mappedJavaTypeDescriptor;
	}

	@Override
	public JavaTypeDescriptor getMappedJavaTypeDescriptor() {
		return mappedJavaTypeDescriptor;
	}

	@Override
	public ModelPart findSubPart(String name) {
		if ( attributeMappings != null ) {
			for ( AttributeMapping attributeMapping : attributeMappings ) {
				if ( attributeMapping.getAttributeName().equals( name ) ) {
					return attributeMapping;
				}
			}
		}

		return null;
	}

	@Override
	public Collection<AttributeMapping> getAttributeMappings() {
		return attributeMappings == null ? Collections.emptySet() : attributeMappings;
	}

	@Override
	public void visitAttributeMappings(Consumer<AttributeMapping> action) {
		if ( attributeMappings == null ) {
			attributeMappings = new TreeSet<>(
					Comparator.comparing( AttributeMapping::getAttributeName )
			);
		}
	}
}
