/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Collection;
import javax.persistence.metamodel.CollectionAttribute;

/**
 * Hibernate extension to the JPA {@link CollectionAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface CollectionAttributeImplementor<D,E> extends CollectionAttribute<D,E>, PluralAttributeImplementor<D, Collection<E>,E> {
	@Override
	SimpleTypeImplementor<E> getValueGraphType();

	@Override
	SimpleTypeImplementor<E> getElementType();

	@Override
	ManagedTypeImplementor<D> getDeclaringType();
}
