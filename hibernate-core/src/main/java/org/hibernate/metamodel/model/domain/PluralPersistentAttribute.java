/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import javax.persistence.metamodel.PluralAttribute;

/**
 * Hibernate extension to the JPA {@link PluralAttribute} descriptor
 *
 * todo (6.0) : Create an form of plural attribute (and singular) in the API package (org.hibernate.metamodel.model.domain)
 * 		and have this extend it
 *
 * @author Steve Ebersole
 */
public interface PluralPersistentAttribute<D,C,E> extends PluralAttribute<D,C,E>, PersistentAttribute<D,C> {
	@Override
	ManagedDomainType<D> getDeclaringType();

	@Override
	CollectionDomainType<C,E> getType();

	@Override
	SimpleDomainType<E> getElementType();

	@Override
	SimpleDomainType<E> getValueGraphType();
}
