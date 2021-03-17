/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.SingularAttribute;

/**
 * Hibernate extension to the JPA {@link SingularAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface SingularPersistentAttribute<D,J> extends SingularAttribute<D,J>, PersistentAttributeDescriptor<D,J> {
	@Override
	SimpleTypeDescriptor<J> getType();

	@Override
	ManagedTypeDescriptor<D> getDeclaringType();

	/**
	 * For a singular attribute, the value type is defined as the
	 * attribute type
	 */
	@Override
	default SimpleTypeDescriptor<?> getValueGraphType() {
		return getType();
	}

	@Override
	default Class<J> getJavaType() {
		return getType().getJavaType();
	}
}
