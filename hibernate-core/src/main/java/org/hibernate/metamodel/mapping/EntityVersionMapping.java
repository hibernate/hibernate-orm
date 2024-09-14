/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.spi.VersionValue;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.type.descriptor.java.VersionJavaType;

/**
 * Describes the mapping of an entity's version
 *
 * @see jakarta.persistence.Version
 */
public interface EntityVersionMapping extends BasicValuedModelPart {
	/**
	 * The attribute marked as the version
	 */
	BasicAttributeMapping getVersionAttribute();

	/**
	 * The strategy for distinguishing between detached and transient
	 * state based on the version mapping.
	 *
	 * @see EntityIdentifierMapping#getUnsavedStrategy()
	 */
	VersionValue getUnsavedStrategy();

	@Override
	VersionJavaType<?> getJavaType();

	@Override
	default VersionJavaType<?> getExpressibleJavaType() {
		return (VersionJavaType<?>) getMappedType().getMappedJavaType();
	}

	@Override
	default AttributeMapping asAttributeMapping() {
		return getVersionAttribute();
	}
}
