/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.tree.predicate.SqmWhereClauseContainer;

/**
 * In some cases it is useful to be able to handle UPDATE or DELETE SQM trees
 * using a single contract.  This contract serves that role
 *
 * @author Steve Ebersole
 */
public interface SqmDeleteOrUpdateStatement<E> extends SqmDmlStatement<E>, SqmWhereClauseContainer {
}
