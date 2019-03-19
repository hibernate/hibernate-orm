/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedPath implements SqmPathWrapper {
	private final SqmPath wrappedPath;
	private final EntityTypeDescriptor<?> treatTarget;

	public SqmTreatedPath(
			SqmPath wrappedPath,
			EntityTypeDescriptor<?> treatTarget) {
		assert wrappedPath.getReferencedNavigable() instanceof EntityValuedNavigable<?>;

		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public SqmPath getWrappedPath() {
		return wrappedPath;
	}

	public EntityTypeDescriptor<?> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public Navigable<?> getReferencedNavigable() {
		return wrappedPath.getReferencedNavigable();
	}

	@Override
	public SqmPath getLhs() {
		return wrappedPath.getLhs();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return wrappedPath.getNavigablePath();
	}

	@Override
	public String getExplicitAlias() {
		return wrappedPath.getExplicitAlias();
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {
		wrappedPath.setExplicitAlias( explicitAlias );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		return wrappedPath.resolvePathPart( name, currentContextKey, isTerminal, creationState );
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		return wrappedPath.resolveIndexedAccess( selector, currentContextKey, isTerminal, creationState );
	}

	@Override
	public ExpressableType getExpressableType() {
		return wrappedPath.getExpressableType();
	}

	@Override
	public Supplier<? extends ExpressableType> getInferableType() {
		return wrappedPath.getInferableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitTreatedPath( this );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return wrappedPath.getJavaTypeDescriptor();
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
