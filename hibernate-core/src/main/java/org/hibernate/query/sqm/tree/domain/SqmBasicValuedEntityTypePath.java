/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;

/**
 * A path that is wrapping an entity type literal.
 *
 * @author Christian Beikov
 * @see SqmPath#type()
 */
public class SqmBasicValuedEntityTypePath<T> extends SqmBasicValuedSimplePath<T> {

	private final SqmLiteralEntityType<T> literal;

	public SqmBasicValuedEntityTypePath(
			NavigablePath navigablePath,
			EntityDomainType<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public SqmBasicValuedEntityTypePath(
			NavigablePath navigablePath,
			EntityDomainType<T> referencedPathSource,
			SqmPath<?> lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
		this.literal = new SqmLiteralEntityType<>( referencedPathSource, nodeBuilder );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Visitation

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEntityTypeLiteralExpression( literal );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "type(" );
		super.appendHqlString( sb );
		sb.append( ')' );
	}
}
