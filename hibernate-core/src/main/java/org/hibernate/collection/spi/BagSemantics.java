/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.spi;

import java.util.Collection;

/**
 * @author Andrea Boriero
 */
public interface BagSemantics<BE extends Collection<E>, E> extends CollectionSemantics<BE,E> {
}
