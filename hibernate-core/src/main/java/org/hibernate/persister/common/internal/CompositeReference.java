/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import org.hibernate.persister.embeddable.spi.EmbeddablePersister;

/**
 * Describes parts of the domain model that can be composite values.
 *
 * @author Steve Ebersole
 */
public interface CompositeReference extends CompositeContainer {
	CompositeContainer getCompositeContainer();
	EmbeddablePersister getEmbeddablePersister();
}
