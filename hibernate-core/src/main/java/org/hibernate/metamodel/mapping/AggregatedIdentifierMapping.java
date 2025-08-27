/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
