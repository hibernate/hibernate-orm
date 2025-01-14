/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Common contract for composite identifiers. Specific subtypes include aggregated
 * (think {@link jakarta.persistence.EmbeddedId}) and non-aggregated (think
 * {@link jakarta.persistence.IdClass}).
 *
 * @author Steve Ebersole
 */
public interface CompositeIdentifierSource extends IdentifierSource, EmbeddableSourceContributor {
}
