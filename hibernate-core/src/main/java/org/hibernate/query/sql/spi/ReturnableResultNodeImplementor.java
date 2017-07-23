/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import org.hibernate.query.sql.ReturnableResultRegistration;
import org.hibernate.sql.ast.tree.spi.select.QueryResult;

/**
 * Unification of both the accessor {@link ReturnableResultRegistration}
 * and mutator {@link ResultNodeImplementor} for nodes which are returnable
 *
 * @author Steve Ebersole
 */
public interface ReturnableResultNodeImplementor
		extends ResultNodeImplementor, ReturnableResultRegistration {
	QueryResult buildReturn(NodeResolutionContext resolutionContext);
}
