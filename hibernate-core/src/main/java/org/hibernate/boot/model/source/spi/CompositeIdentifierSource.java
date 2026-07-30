/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * Common contract for composite identifiers. Specific subtypes include aggregated
 * (think {@link jakarta.persistence.EmbeddedId}) and non-aggregated (think
 * {@link jakarta.persistence.IdClass}).
 *
 * @author Steve Ebersole
 */
@Remove
public interface CompositeIdentifierSource extends IdentifierSource, EmbeddableSourceContributor {
}
