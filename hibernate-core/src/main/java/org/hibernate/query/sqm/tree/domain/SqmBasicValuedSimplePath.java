/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmBasicValuedSimplePath extends AbstractSqmSimplePath {
	public SqmBasicValuedSimplePath(
			String uid,
			NavigablePath navigablePath,
			BasicValuedNavigable referencedNavigable,
			SqmPath lhs) {
		this( uid, navigablePath, referencedNavigable, lhs, null );
	}

	public SqmBasicValuedSimplePath(
			String uid,
			NavigablePath navigablePath,
			BasicValuedNavigable referencedNavigable,
			SqmPath lhs,
			String explicitAlias) {
		super( uid, navigablePath, referencedNavigable, lhs, explicitAlias );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException( "Basic-valued path [" + getNavigablePath() + "] cannot be de-referenced : " + name );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitBasicValuedPath( this );
	}

	@Override
	public BasicValuedNavigable getReferencedNavigable() {
		return (BasicValuedNavigable) super.getReferencedNavigable();
	}

	@Override
	public BasicValuedNavigable getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public Supplier<? extends BasicValuedNavigable> getInferableType() {
		return this::getReferencedNavigable;
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return (BasicJavaDescriptor) super.getJavaTypeDescriptor();
	}
}
