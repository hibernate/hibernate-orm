/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
