/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.tree.predicate.SqmWhereClauseContainer;

/**
 * In some cases it is useful to be able to handle UPDATE or DELETE SQM trees
 * using a single contract.  This contract serves that role.
 *
 * @author Steve Ebersole
 */
public interface SqmDeleteOrUpdateStatement<T> extends SqmDmlStatement<T>, SqmWhereClauseContainer {
}
