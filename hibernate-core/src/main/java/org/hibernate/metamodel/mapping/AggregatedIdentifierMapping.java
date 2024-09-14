/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;

/**
 * An "aggregated" composite identifier, which is another way to say that the
 * identifier is represented as an {@linkplain jakarta.persistence.EmbeddedId embeddable}.
 *
 * @see jakarta.persistence.EmbeddedId
 *
 * @author Steve Ebersole
 */
public interface AggregatedIdentifierMapping extends SingleAttributeIdentifierMapping {
}
