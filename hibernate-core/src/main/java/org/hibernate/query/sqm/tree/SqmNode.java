/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * Base contract for any SQM AST node.
 *
 * @author Steve Ebersole
 */
public interface SqmNode {
	String asLoggableText();
	<T> T accept(SemanticQueryWalker<T> walker);
}
