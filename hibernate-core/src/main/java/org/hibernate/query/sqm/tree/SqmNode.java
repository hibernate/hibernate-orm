/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.criteria.JpaCriteriaNode;
import org.hibernate.query.sqm.NodeBuilder;

import org.jboss.logging.Logger;

/**
 * Base contract for any SQM AST node.
 *
 * @author Steve Ebersole
 */
public interface SqmNode extends JpaCriteriaNode {
	Logger log = Logger.getLogger( SqmNode.class );

	default String asLoggableText() {
		log.debugf( "#asLoggableText not defined for %s - using #toString", getClass().getName() );
		return toString();
	}

	NodeBuilder nodeBuilder();

	SqmNode copy(SqmCopyContext context);
}
