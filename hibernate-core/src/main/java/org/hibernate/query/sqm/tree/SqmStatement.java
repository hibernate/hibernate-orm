/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import java.util.Set;

import org.hibernate.query.sqm.tree.expression.SqmParameter;

/**
 * The basic SQM statement contract.
 *
 * @author Steve Ebersole
 */
public interface SqmStatement extends SqmNode {

	// todo (6.0) : review potentially removing this as a SqmNode.
	// 		implementing SemanticQueryWalker visitation here effectively makes the
	// 		statement a SqmNode - which caused some issue somewhere, although I forget
	//		the details.  Review this an decide if this is a problem or ok

	Set<SqmParameter> getQueryParameters();
}
