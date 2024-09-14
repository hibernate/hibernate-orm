/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;

/**
 * Marker interface for standard EmbeddableInstantiator implementations.
 *
 * This allows us to recognize custom instantiators
 */
public interface StandardEmbeddableInstantiator extends EmbeddableInstantiator {
}
