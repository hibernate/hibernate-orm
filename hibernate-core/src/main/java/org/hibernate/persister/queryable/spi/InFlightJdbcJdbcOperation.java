/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.queryable.spi;

import org.hibernate.sql.ast.QuerySpec;
import org.hibernate.sql.ast.from.TableSpace;

/**
 * @author Steve Ebersole
 */
public interface InFlightJdbcJdbcOperation {
	// todo (6.0) - possible design; need to massage
	//		could also just pass these values as args (versus a "param object" like this)

	/**
	 * Access to the QuerySpec that this persister reference will be part
	 * of, whether that is a top-level query or a sub-query.
	 */
	QuerySpec getContainingQuerySpec();

	/**
	 * Access to the TableSpace that defines the scope for this persister reference.
	 */
	TableSpace getContainingTableSpace();
}
