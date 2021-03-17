/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Set;
import javax.persistence.metamodel.SetAttribute;

/**
 * Hibernate extension to the JPA {@link SetAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface SetPersistentAttribute<D,E> extends SetAttribute<D,E>, PluralPersistentAttribute<D,Set<E>,E> {
}
