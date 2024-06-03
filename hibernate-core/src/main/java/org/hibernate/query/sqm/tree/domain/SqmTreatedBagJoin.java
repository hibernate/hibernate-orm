/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedBagJoin<O,T, S extends T> extends SqmBagJoin<O,S> implements SqmTreatedPath<T,S> {
	private final SqmBagJoin<O, T> wrappedPath;
	private final TreatableDomainType<S> treatTarget;

	public SqmTreatedBagJoin(
			SqmBagJoin<O, T> wrappedPath,
			TreatableDomainType<S> treatTarget,
			String alias) {
		this( wrappedPath, treatTarget, alias, false );
	}

	public SqmTreatedBagJoin(
			SqmBagJoin<O, T> wrappedPath,
			TreatableDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				wrappedPath.getNavigablePath()
						.append( CollectionPart.Nature.ELEMENT.getName() )
						.treatAs( treatTarget.getTypeName(), alias ),
				(BagPersistentAttribute<O, S>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				fetched,
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	private SqmTreatedBagJoin(
			NavigablePath navigablePath,
			SqmBagJoin<O, T> wrappedPath,
			TreatableDomainType<S> treatTarget,
			String alias,
			boolean fetched) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				navigablePath,
				(BagPersistentAttribute<O, S>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.isFetched(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmTreatedBagJoin<O, T, S> copy(SqmCopyContext context) {
		final SqmTreatedBagJoin<O, T, S> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmTreatedBagJoin<O, T, S> path = context.registerCopy(
				this,
				new SqmTreatedBagJoin<>(
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
	public SqmBagJoin<O,T> getWrappedPath() {
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
	public SqmAttributeJoin<O, S> makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmTreatedBagJoin<>( wrappedPath, treatTarget, getAlias() );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "treat(" );
		wrappedPath.appendHqlString( sb );
		sb.append( " as " );
		sb.append( treatTarget.getTypeName() );
		sb.append( ')' );
	}
}
