/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.results.spi.QueryResultProducer;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Defines a SQM AST node that can be used as a selection in the query,
 * or as an argument to a dynamic-instantiation
 *
 * @author Steve Ebersole
 */
public interface SqmSelectableNode extends QueryResultProducer<SemanticQueryWalker>, SqmTypedNode {
	@Override
	default JavaTypeDescriptor getJavaTypeDescriptor() {
		return getProducedJavaTypeDescriptor();
	}
}
