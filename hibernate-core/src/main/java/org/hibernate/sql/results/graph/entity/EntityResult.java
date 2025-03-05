/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.sql.results.graph.DomainResult;

/**
 * Specialization of DomainResult for entity-valued results
 *
 * @author Steve Ebersole
 */
public interface EntityResult extends EntityResultGraphNode, DomainResult {
}
