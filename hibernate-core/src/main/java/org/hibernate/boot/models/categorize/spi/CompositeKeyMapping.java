/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

/**
 * Id-mapping which is embeddable - either {@linkplain AggregatedKeyMapping physically}
 * or {@linkplain NonAggregatedKeyMapping virtually}.
 *
 * @author Steve Ebersole
 */
public interface CompositeKeyMapping extends KeyMapping {
}
