/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.Incubating;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.derived.AnonymousTupleType;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaDerivedJoin;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedEntityJoin;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.spi.NavigablePath;

/**
 * @author Christian Beikov
 */
@Incubating
public class SqmDerivedJoin<T> extends AbstractSqmJoin<T, T> implements JpaDerivedJoin<T> {
	private final SqmSubQuery<T> subQuery;
	private final boolean lateral;
	private SqmPredicate joinPredicate;

	public SqmDerivedJoin(
			SqmSubQuery<T> subQuery,
			String alias,
			SqmJoinType joinType,
			boolean lateral,
			SqmRoot<?> sqmRoot) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<derived>>", alias ),
				subQuery,
				lateral,
				new AnonymousTupleType<>( subQuery ),
				alias,
				validateJoinType( joinType, lateral ),
				sqmRoot
		);
	}

	protected SqmDerivedJoin(
			NavigablePath navigablePath,
			SqmSubQuery<T> subQuery,
			boolean lateral,
			SqmPathSource<T> pathSource,
			String alias,
			SqmJoinType joinType,
			SqmRoot<?> sqmRoot) {
		super(
				navigablePath,
				pathSource,
				sqmRoot,
				alias,
				joinType,
				sqmRoot.nodeBuilder()
		);
		this.subQuery = subQuery;
		this.lateral = lateral;
	}

	private static SqmJoinType validateJoinType(SqmJoinType joinType, boolean lateral) {
		if ( lateral ) {
			switch ( joinType ) {
				case LEFT:
				case INNER:
					break;
				default:
					throw new IllegalArgumentException( "Lateral joins can only be left or inner. Illegal join type: " + joinType );
			}
		}
		return joinType;
	}

	@Override
	public SqmDerivedJoin<T> copy(SqmCopyContext context) {
		final SqmDerivedJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmDerivedJoin<T> path = context.registerCopy(
				this,
				new SqmDerivedJoin<>(
						getNavigablePath(),
						subQuery,
						lateral,
						getReferencedPathSource(),
						getExplicitAlias(),
						getSqmJoinType(),
						findRoot().copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	protected void copyTo(SqmDerivedJoin<T> target, SqmCopyContext context) {
		super.copyTo( target, context );
		target.joinPredicate = joinPredicate == null ? null : joinPredicate.copy( context );
	}

	public SqmRoot<?> getRoot() {
		return (SqmRoot<?>) super.getLhs();
	}

	@Override
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	@Override
	public SqmSubQuery<T> getQueryPart() {
		return subQuery;
	}

	@Override
	public boolean isLateral() {
		return lateral;
	}

	@Override
	public SqmPath<?> getLhs() {
		// A derived-join has no LHS
		return null;
	}

	@Override
	public SqmPredicate getJoinPredicate() {
		return joinPredicate;
	}

	@Override
	public void setJoinPredicate(SqmPredicate predicate) {
		this.joinPredicate = predicate;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedDerivedJoin( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCorrelatedEntityJoin<T> createCorrelation() {
		// todo: implement
		throw new NotYetImplementedFor6Exception(getClass());
//		return new SqmCorrelatedEntityJoin<>( this );
	}

	@Override
	public <S extends T> SqmTreatedEntityJoin<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "Derived joins can not be treated!" );
	}
	@Override
	public <S extends T> SqmTreatedEntityJoin<T,S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		throw new UnsupportedOperationException( "Derived joins can not be treated!" );
	}

	@Override
	public <S extends T> SqmFrom<?, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Derived joins can not be treated!" );
	}

	@Override
	public <S extends T> SqmFrom<?, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Derived joins can not be treated!" );
	}

}
