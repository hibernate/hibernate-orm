/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaJoinedFrom;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.domain.AbstractSqmQualifiedJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;

/**
 * @author Christian Beikov
 */
@Incubating
public class SqmCteJoin<T> extends AbstractSqmQualifiedJoin<T, T> implements JpaJoinedFrom<T, T> {
	private final SqmCteStatement<T> cte;

	public SqmCteJoin(
			SqmCteStatement<T> cte,
			String alias,
			SqmJoinType joinType,
			SqmRoot<?> sqmRoot) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<cte>>", alias ),
				cte,
				(SqmPathSource<T>) cte.getCteTable().getTupleType(),
				alias,
				joinType,
				sqmRoot
		);
	}

	protected SqmCteJoin(
			NavigablePath navigablePath,
			SqmCteStatement<T> cte,
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
		this.cte = cte;
	}

	@Override
	public boolean isImplicitlySelectable() {
		return false;
	}

	@Override
	public SqmCteJoin<T> copy(SqmCopyContext context) {
		final SqmCteJoin<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCteJoin<T> path = context.registerCopy(
				this,
				new SqmCteJoin<>(
						getNavigablePath(),
						cte.copy( context ),
						getReferencedPathSource(),
						getExplicitAlias(),
						getSqmJoinType(),
						findRoot().copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	public SqmRoot<?> getRoot() {
		return (SqmRoot<?>) super.getLhs();
	}

	@Override
	public SqmRoot<?> findRoot() {
		return getRoot();
	}

	public SqmCteStatement<T> getCte() {
		return cte;
	}

	@Override
	public SqmPath<?> getLhs() {
		// A cte-join has no LHS
		return null;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedCteJoin( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmCorrelatedEntityJoin<T,T> createCorrelation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public PersistentAttribute<? super T, ?> getAttribute() {
		return null;
	}

	@Override
	public <S extends T> SqmQualifiedJoin<T, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	public <S extends T> SqmQualifiedJoin<T, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "CTE joins can not be treated" );
	}

	@Override
	public From<?, T> getParent() {
		return getCorrelationParent();
	}

	@Override
	public JoinType getJoinType() {
		return getSqmJoinType().getCorrespondingJpaJoinType();
	}
}
