/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.metamodel.model.domain.SetPersistentAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmTreatedAttributeJoin;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSetJoin<O,T, S extends T> extends SqmSetJoin<O,S> implements SqmTreatedAttributeJoin<O,T,S> {
	private final SqmSetJoin<O,T> wrappedPath;
	private final TreatableDomainType<S> treatTarget;

	public SqmTreatedSetJoin(
			SqmSetJoin<O, T> wrappedPath,
			TreatableDomainType<S> treatTarget,
			String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedSetJoin(
				SqmSetJoin<O, T> wrappedPath,
				TreatableDomainType<S> treatTarget,
				String alias,
				boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.append( CollectionPart.Nature.ELEMENT.getName() )
						.treatAs( treatTarget.getTypeName(), alias ),
				(SetPersistentAttribute<O, S>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedSetJoin(
			NavigablePath navigablePath,
			SqmSetJoin<O, T> wrappedPath,
			TreatableDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				(SetPersistentAttribute<O, S>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedSetJoin<O, T, S> copy(SqmCopyContext context) {
		final SqmTreatedSetJoin<O, T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedSetJoin<O, T, S> path = context.registerCopy(
				this,
				new SqmTreatedSetJoin<>(
						getNavigablePath(),
						wrappedPath.copy( context ),
						treatTarget,
						getExplicitAlias(),
						isFetched()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public SqmSetJoin<O,T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public TreatableDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<S> getNodeType() {
		return treatTarget;
	}

	@Override
	public TreatableDomainType<S> getReferencedPathSource() {
		return treatTarget;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return treatTarget;
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		wrappedPath.appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getTypeName() );
		sb.append( ')' );
	}

	@Override
	public SqmTreatedSetJoin<O, T, S> on(JpaExpression<Boolean> restriction) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedSetJoin<O, T, S> on(Expression<Boolean> restriction) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restriction );
	}

	@Override
	public SqmTreatedSetJoin<O, T, S> on(JpaPredicate... restrictions) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restrictions );
	}

	@Override
	public SqmTreatedSetJoin<O, T, S> on(Predicate... restrictions) {
		return (SqmTreatedSetJoin<O, T, S>) super.on( restrictions );
	}
}
