/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
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
