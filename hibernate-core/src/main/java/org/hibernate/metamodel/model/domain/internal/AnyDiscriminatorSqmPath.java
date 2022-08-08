/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.AbstractSqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.spi.NavigablePath;

public class AnyDiscriminatorSqmPath<T> extends AbstractSqmPath<T> {

	protected AnyDiscriminatorSqmPath(
			NavigablePath navigablePath,
			AnyDiscriminatorSqmPathSource referencedPathSource,
			SqmPath lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
	}

	@Override
	public AnyDiscriminatorSqmPath<T> copy(SqmCopyContext context) {
		final AnyDiscriminatorSqmPath existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy(
				this,
				(AnyDiscriminatorSqmPath) getLhs().copy( context ).type()
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitAnyDiscriminatorTypeExpression( this ) ;
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "type(" );
		getLhs().appendHqlString( sb );
		sb.append( ')' );
	}

	@Override
	public SqmPath<?> resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Override
	public SqmTreatedPath treatAs(Class treatJavaType) throws PathException {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@Override
	public SqmTreatedPath treatAs(EntityDomainType treatTarget) throws PathException {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@Override
	public SqmPath<T> getLhs() {
		return (SqmPath<T>) super.getLhs().getLhs();
	}

	@Override
	public AnyDiscriminatorSqmPathSource<T> getExpressible() {
		return (AnyDiscriminatorSqmPathSource<T>) getNodeType();
	}

}
