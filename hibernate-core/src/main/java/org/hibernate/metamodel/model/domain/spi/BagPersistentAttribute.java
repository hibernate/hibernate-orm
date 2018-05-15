/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Collection;

/**
 * todo (6.0) : rename this PluralAttributeBag?
 *
 * @author Steve Ebersole
 */
public interface BagPersistentAttribute<O,E> extends PluralPersistentAttribute<O,Collection<E>,E>,
		javax.persistence.metamodel.CollectionAttribute<O,E> {
}
