/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.jboss.logging.Logger;

/**
 * Base contract for any SQM AST node.
 *
 * @author Steve Ebersole
 */
public interface SqmNode {
	Logger log = Logger.getLogger( SqmNode.class );

	default String asLoggableText() {
		log.warnf( "#asLoggableText not defined for %s - using #toString", getClass().getName() );
		return toString();
	}
}
