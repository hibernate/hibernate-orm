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
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import static org.hibernate.query.sqm.produce.SqmCreationHelper.buildRootNavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmCrossJoin extends AbstractSqmFrom implements SqmJoin {
	private final SqmRoot sqmRoot;

	public SqmCrossJoin(
			EntityTypeDescriptor joinedEntityDescriptor, String alias,
			SqmRoot sqmRoot) {
		super(
				buildRootNavigablePath( alias, joinedEntityDescriptor.getEntityName() ),
				joinedEntityDescriptor,
				alias
		);
		this.sqmRoot = sqmRoot;
	}

	public SqmRoot getRoot() {
		return sqmRoot;
	}

	@Override
	public SqmPath getLhs() {
		// a cross-join has no LHS
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
	public Supplier<? extends ExpressableType> getInferableType() {
		return this::getReferencedNavigable;
	}

	public String getEntityName() {
		return getReferencedNavigable().getEntityName();
	}

	@Override
	public SqmJoinType getJoinType() {
		return SqmJoinType.CROSS;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitCrossJoinedFromElement( this );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	@Override
	public SqmRoot findRoot() {
		return getRoot();
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
