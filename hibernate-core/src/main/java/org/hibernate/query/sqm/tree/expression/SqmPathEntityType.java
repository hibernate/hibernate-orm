/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class SqmPathEntityType<T> extends SqmBasicValuedSimplePath<T> {
	public SqmPathEntityType(SqmPath<T> entityValuedPath, NodeBuilder nodeBuilder) {
		super(
				entityValuedPath.getNavigablePath().append( EntityDiscriminatorMapping.ROLE_NAME ),
				// todo (6.0) : need a BasicValueSqmPathSource representing the discriminator for this to work.
				null,
				entityValuedPath,
				nodeBuilder
		);

		throw new NotYetImplementedFor6Exception( getClass() );

//		//noinspection unchecked
//		super(
//				,
//				( (EntityDomainType<T>) entityValuedPath.getNodeType() ).getDiscriminatorDescriptor(),
//				entityValuedPath,
//				nodeBuilder
//		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSqmPathEntityTypeExpression( this );
	}
}
