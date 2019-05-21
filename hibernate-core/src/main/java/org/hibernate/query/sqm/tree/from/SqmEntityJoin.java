/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.AbstractSqmJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedEntityJoin;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmEntityJoin<T> extends AbstractSqmJoin<T,T> implements SqmQualifiedJoin<T,T> {
	private final SqmRoot sqmRoot;
	private SqmPredicate joinPredicate;

	public SqmEntityJoin(
			EntityTypeDescriptor<T> joinedEntityDescriptor,
			String alias,
			SqmJoinType joinType,
			SqmRoot sqmRoot) {
		super(
				SqmCreationHelper.buildRootNavigablePath( joinedEntityDescriptor.getEntityName(), alias ),
				joinedEntityDescriptor,
				sqmRoot,
				alias,
				joinType,
				sqmRoot.nodeBuilder()
		);
		this.sqmRoot = sqmRoot;
	}

	public SqmRoot getRoot() {
		return sqmRoot;
	}

	@Override
	public SqmRoot findRoot() {
		return getRoot();
	}

	@Override
	public SqmPath getLhs() {
		// An entity-join has no LHS
		return null;
	}

	@Override
	public SqmPathSource<?, T> getReferencedPathSource() {
		return (EntityTypeDescriptor<T>) super.getReferencedPathSource();
	}

	@Override
	public EntityJavaDescriptor<T> getJavaTypeDescriptor() {
		return getReferencedPathSource().getJavaTypeDescriptor();
	}

	public String getEntityName() {
		return getReferencedPathSource().getEntityName();
	}

	@Override
	public SqmPredicate getJoinPredicate() {
		return joinPredicate;
	}

	public void setJoinPredicate(SqmPredicate predicate) {
		this.joinPredicate = predicate;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitQualifiedEntityJoin( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public <S extends T> SqmTreatedEntityJoin<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		final EntityTypeDescriptor<S> treatTarget = nodeBuilder().getDomainModel().entity( treatJavaType );
		return new SqmTreatedEntityJoin<>( this, treatTarget, null, getSqmJoinType() );
	}
}
