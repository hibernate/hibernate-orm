/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

/**
 * CompositeIdMapping which is physically an embeddable and represented by a single attribute.
 *
 * @see jakarta.persistence.EmbeddedId
 *
 * @author Steve Ebersole
 */
public interface AggregatedKeyMapping extends CompositeKeyMapping, SingleAttributeKeyMapping {
}
