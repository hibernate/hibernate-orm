/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.DiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.AbstractSqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;

/**
 * {@link SqmPath} specialization for an embeddable discriminator
 *
 * @author Marco Belladelli
 */
public class EmbeddedDiscriminatorSqmPath<T> extends AbstractSqmPath<T> implements DiscriminatorSqmPath<T> {
	private final EmbeddableDomainType<T> embeddableDomainType;

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	protected EmbeddedDiscriminatorSqmPath(
			NavigablePath navigablePath,
			SqmPathSource referencedPathSource,
			SqmPath<?> lhs,
			EmbeddableDomainType embeddableDomainType,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
		this.embeddableDomainType = embeddableDomainType;
	}

	public EmbeddableDomainType<T> getEmbeddableDomainType() {
		return embeddableDomainType;
	}

	@Override
	public EmbeddedDiscriminatorSqmPathSource<T> getExpressible() {
		return (EmbeddedDiscriminatorSqmPathSource<T>) getNodeType();
	}

	@Override
	public EmbeddedDiscriminatorSqmPath<T> copy(SqmCopyContext context) {
		final EmbeddedDiscriminatorSqmPath<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		//noinspection unchecked
		return context.registerCopy(
				this,
				(EmbeddedDiscriminatorSqmPath<T>) getLhs().copy( context ).type()
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitDiscriminatorPath( this );
	}
}
