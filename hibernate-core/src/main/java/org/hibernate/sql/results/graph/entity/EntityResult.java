/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
