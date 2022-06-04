/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.Incubating;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.derived.AnonymousTupleType;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.spi.NavigablePath;

/**
 * @author Christian Beikov
 */
@Incubating
public class SqmDerivedRoot<T> extends SqmRoot<T> implements JpaDerivedRoot<T> {

	private final SqmSubQuery<T> subQuery;
	private final boolean lateral;

	public SqmDerivedRoot(
			SqmSubQuery<T> subQuery,
			String alias,
			boolean lateral) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<derived>>", alias ),
				subQuery,
				lateral,
				new AnonymousTupleType<>( subQuery ),
				alias
		);
	}

	protected SqmDerivedRoot(
			NavigablePath navigablePath,
			SqmSubQuery<T> subQuery,
			boolean lateral,
			SqmPathSource<T> pathSource,
			String alias) {
		super(
				navigablePath,
				pathSource,
				alias,
				true,
				subQuery.nodeBuilder()
		);
		this.subQuery = subQuery;
		this.lateral = lateral;
	}

	@Override
	public SqmDerivedRoot<T> copy(SqmCopyContext context) {
		final SqmDerivedRoot<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmDerivedRoot<T> path = context.registerCopy(
				this,
				new SqmDerivedRoot<>(
						getNavigablePath(),
						getQueryPart().copy( context ),
						isLateral(),
						getReferencedPathSource(),
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
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
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitRootDerived( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public EntityDomainType<T> getModel() {
		// Or should we throw an exception instead?
		return null;
	}

	@Override
	public SqmCorrelatedRoot<T> createCorrelation() {
		// todo: implement
		throw new NotYetImplementedFor6Exception( getClass());
//		return new SqmCorrelatedRoot<>( this );
	}

	@Override
	public <S extends T> SqmTreatedRoot<T, S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "Derived roots can not be treated!" );
	}

	@Override
	public <S extends T> SqmTreatedRoot<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		throw new UnsupportedOperationException( "Derived roots can not be treated!" );
	}

	@Override
	public <S extends T> SqmFrom<?, S> treatAs(Class<S> treatJavaType, String alias) {
		throw new UnsupportedOperationException( "Derived roots can not be treated!" );
	}

	@Override
	public <S extends T> SqmFrom<?, S> treatAs(EntityDomainType<S> treatTarget, String alias) {
		throw new UnsupportedOperationException( "Derived roots can not be treated!" );
	}
}
