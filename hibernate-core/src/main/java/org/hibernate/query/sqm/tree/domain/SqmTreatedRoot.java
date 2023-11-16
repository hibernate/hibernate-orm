/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class SqmTreatedRoot extends SqmRoot implements SqmTreatedFrom {
	private final SqmRoot wrappedPath;
	private final EntityDomainType treatTarget;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmTreatedRoot(
			SqmRoot wrappedPath,
			EntityDomainType treatTarget) {
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private SqmTreatedRoot(
			NavigablePath navigablePath,
			SqmRoot wrappedPath,
			EntityDomainType treatTarget) {
		super(
				navigablePath,
				(EntityDomainType) wrappedPath.getReferencedPathSource(),
				null,
				wrappedPath.nodeBuilder()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public SqmTreatedRoot copy(SqmCopyContext context) {
		final SqmTreatedRoot existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedRoot path = context.registerCopy(
				this,
				new SqmTreatedRoot(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public EntityDomainType getTreatTarget() {
		return treatTarget;
	}

	@Override
	public EntityDomainType getManagedType() {
		return getTreatTarget();
	}

	@Override
	public SqmPath getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public SqmPathSource getNodeType() {
		return treatTarget;
	}

	@Override
	public EntityDomainType getReferencedPathSource() {
		return getTreatTarget();
	}

	@Override
	public SqmPath<?> getLhs() {
		return wrappedPath.getLhs();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object accept(SemanticQueryWalker walker) {
		return walker.visitTreatedPath( this );
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPath<?> sqmPath = get( name );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
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
