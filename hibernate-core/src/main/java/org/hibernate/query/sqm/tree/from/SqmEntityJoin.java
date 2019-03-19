/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmEntityJoin extends AbstractSqmJoin implements SqmQualifiedJoin {
	private final SqmRoot sqmRoot;
	private SqmPredicate joinPredicate;

	public SqmEntityJoin(
			String uid,
			String alias,
			EntityTypeDescriptor joinedEntityDescriptor,
			SqmJoinType joinType,
			SqmRoot sqmRoot) {
		super(
				uid,
				SqmCreationHelper.buildRootNavigablePath( joinedEntityDescriptor.getEntityName(), alias ),
				joinedEntityDescriptor,
				alias,
				joinType
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
	public EntityTypeDescriptor<?> getReferencedNavigable() {
		return (EntityTypeDescriptor<?>) super.getReferencedNavigable();
	}

	@Override
	public EntityTypeDescriptor getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public Supplier<? extends EntityTypeDescriptor> getInferableType() {
		return this::getReferencedNavigable;
	}

	@Override
	public EntityJavaDescriptor getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	public String getEntityName() {
		return getReferencedNavigable().getEntityName();
	}

	@Override
	public SqmPredicate getJoinPredicate() {
		return joinPredicate;
	}

	public void setJoinPredicate(SqmPredicate predicate) {
		this.joinPredicate = predicate;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitQualifiedEntityJoinFromElement( this );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}
}
