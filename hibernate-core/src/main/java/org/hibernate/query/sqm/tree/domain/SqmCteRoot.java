/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.spi.NavigablePath;

/**
 * @author Christian Beikov
 */
@Incubating
public class SqmCteRoot<T> extends SqmRoot<T> implements JpaRoot<T> {

	private final SqmCteStatement<T> cte;

	public SqmCteRoot(
			SqmCteStatement<T> cte,
			String alias) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<cte>>", alias ),
				cte,
				(SqmPathSource<T>) cte.getCteTable().getTupleType(),
				alias
		);
	}

	protected SqmCteRoot(
			NavigablePath navigablePath,
			SqmCteStatement<T> cte,
			SqmPathSource<T> pathSource,
			String alias) {
		super(
				navigablePath,
				pathSource,
				alias,
				true,
				cte.nodeBuilder()
		);
		this.cte = cte;
	}

	@Override
	public SqmCteRoot<T> copy(SqmCopyContext context) {
		final SqmCteRoot<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCteRoot<T> path = context.registerCopy(
				this,
				new SqmCteRoot<>(
						getNavigablePath(),
						getCte().copy( context ),
						getReferencedPathSource(),
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
	}

	public SqmCteStatement<T> getCte() {
		return cte;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitRootCte( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public EntityDomainType<T> getModel() {
		// Or should we throw an exception instead?
		return null;
	}

	@Override
	public SqmPathSource<?> getResolvedModel() {
		return getReferencedPathSource();
	}

	@Override
	public SqmCorrelatedRoot<T> createCorrelation() {
		return new SqmCorrelatedDerivedRoot<>( this );
	}

	@Override
	public <S extends T> SqmTreatedRoot<T, S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "CTE roots can not be treated" );
	}

	@Override
	public <S extends T> SqmTreatedRoot<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		throw new UnsupportedOperationException( "CTE roots can not be treated" );
	}

	@Override
	public <S extends T> SqmFrom<?, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "CTE roots can not be treated" );
	}

	@Override
	public <S extends T> SqmFrom<?, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "CTE roots can not be treated" );
	}
}
