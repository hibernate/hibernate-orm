/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable;

import org.hibernate.sql.results.graph.DomainResult;

/**
 * DomainResult specialization for embeddable-valued results
 *
 * @author Steve Ebersole
 */
public interface EmbeddableResult<T> extends EmbeddableResultGraphNode, DomainResult<T> {

}
