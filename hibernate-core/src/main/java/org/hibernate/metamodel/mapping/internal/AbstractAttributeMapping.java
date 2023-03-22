/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeMapping implements AttributeMapping {
	private final String name;
	private final int fetchableIndex;

	private final ManagedMappingType declaringType;

	public AbstractAttributeMapping(String name, int fetchableIndex, ManagedMappingType declaringType) {
		this.name = name;
		this.fetchableIndex = fetchableIndex;
		this.declaringType = declaringType;
	}

	/**
	 * For Hibernate Reactive
 	 */
	protected AbstractAttributeMapping(AbstractAttributeMapping original) {
		this( original.name, original.fetchableIndex, original.declaringType );
	}

	@Override
	public ManagedMappingType getDeclaringType() {
		return declaringType;
	}

	@Override
	public String getAttributeName() {
		return name;
	}

	@Override
	public int getFetchableKey() {
		return fetchableIndex;
	}

	@Override
	public MappingType getPartMappingType() {
		return getMappedType();
	}

	@Override
	public JavaType<?> getJavaType() {
		return getMappedType().getMappedJavaType();
	}

	void setForeignKeyDescriptor(ForeignKeyDescriptor foreignKeyDescriptor){
	}
}
