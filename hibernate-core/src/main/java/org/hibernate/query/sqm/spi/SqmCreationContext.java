/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.spi;

import org.hibernate.Incubating;
import org.hibernate.query.BindingContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;

/**
 * The context in which all SQM creations occur.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationContext extends BindingContext {
	QueryEngine getQueryEngine();

	default NodeBuilder getNodeBuilder() {
		return getQueryEngine().getCriteriaBuilder();
	}

	/**
	 * @apiNote Avoid calling this method, since {@link Class}
	 *          objects are not available to the query validator
	 *          in Hibernate Processor at compilation time.
	 */
	Class<?> classForName(String className);
}
