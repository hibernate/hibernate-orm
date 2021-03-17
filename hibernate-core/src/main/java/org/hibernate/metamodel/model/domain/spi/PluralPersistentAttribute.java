/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.PluralAttribute;

/**
 * Hibernate extension to the JPA {@link PluralAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface PluralPersistentAttribute<D,C,E> extends PluralAttribute<D,C,E>, PersistentAttributeDescriptor<D,C> {
	@Override
	ManagedTypeDescriptor<D> getDeclaringType();

	@Override
	SimpleTypeDescriptor<E> getElementType();

	@Override
	SimpleTypeDescriptor<E> getValueGraphType();
}
