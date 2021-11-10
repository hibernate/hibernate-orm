/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedRoot<T, S extends T> extends SqmRoot<S> implements SqmTreatedPath<T,S> {
	private final SqmRoot<T> wrappedPath;
	private final EntityDomainType<S> treatTarget;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmTreatedRoot(
			SqmRoot<T> wrappedPath,
			EntityDomainType<S> treatTarget) {
		super(
				wrappedPath.getNavigablePath().treatAs(
						treatTarget.getHibernateEntityName()
				),
				(EntityDomainType) wrappedPath.getReferencedPathSource(),
				null,
				wrappedPath.nodeBuilder()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public void addSqmJoin(SqmJoin<S, ?> join) {
		super.addSqmJoin( join );
		//noinspection unchecked
		wrappedPath.addSqmJoin( (SqmJoin<T, ?>) join );
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public EntityDomainType<S> getManagedType() {
		return getTreatTarget();
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public SqmPathSource<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public EntityDomainType<S> getReferencedPathSource() {
		return getManagedType();
	}

	@Override
	public SqmPath<?> getLhs() {
		return wrappedPath.getLhs();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitTreatedPath( this );
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final NavigablePath subNavPath = getNavigablePath().append( name );
		return creationState.getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
				subNavPath,
				snp -> {
					final SqmPathSource<?> subSource;
					if ( creationState.getCreationOptions().useStrictJpaCompliance() ) {
						subSource = getManagedType().findSubPathSource( name );
					}
					else {
						subSource = treatTarget.findSubPathSource( name );
					}

					if ( subSource == null ) {
						throw UnknownPathException.unknownSubPath( this, name );
					}

					return subSource.createSqmPath( this, null );
				}
		);
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		wrappedPath.appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getName() );
		sb.append( ')' );
	}
}
