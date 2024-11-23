/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

/**
 * Hibernate extension to the JPA {@link jakarta.persistence.metamodel.EntityType} contract.
 *
 * @deprecated Use {@link EmbeddableDomainType} instead.  Originally intended
 * to describe the actual usage of an embeddable (the embedded) because it was intended
 * to include the mapping (column, etc) information.  However, that causes us to need
 * multiple embeddable instances per embeddable class.
 *
 * @author Steve Ebersole
 */
@Deprecated(since = "6.0")
public interface EmbeddedDomainType<J> extends EmbeddableDomainType<J> {
}
