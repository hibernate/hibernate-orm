/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface EntityDiscriminatorMapping extends VirtualModelPart, BasicValuedModelPart {
	String ROLE_NAME = "{discriminator}";
	String LEGACY_HQL_ROLE_NAME = "class";

	static boolean matchesRoleName(String name) {
		return ROLE_NAME.equals( name ) || LEGACY_HQL_ROLE_NAME.equals( name );
	}

	@Override
	default String getPartName() {
		return ROLE_NAME;
	}

	@Override
	default MappingType getPartMappingType() {
		return this::getJavaTypeDescriptor;
	}

	@Override
	default JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		return null;
	}
}
