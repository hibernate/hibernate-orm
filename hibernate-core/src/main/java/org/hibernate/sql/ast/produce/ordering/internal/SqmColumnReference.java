/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmColumnReference implements SqmExpression, SqmNavigableReference {
	private final SqmFrom sqmFromBase;
	private final String columnName;

	public SqmColumnReference(SqmFrom sqmFromBase, String columnName) {
		this.sqmFromBase = sqmFromBase;
		this.columnName = columnName;
	}

	public SqmFrom getSqmFromBase() {
		return sqmFromBase;
	}

	public String getColumnName() {
		return columnName;
	}

	@Override
	public SqmPath getLhs() {
		return sqmFromBase;
	}

	@Override
	public ExpressableType getExpressableType() {
		return null;
	}

	@Override
	public Supplier<? extends ExpressableType> getInferableType() {
		return () -> null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitExplicitColumnReference( this );
	}

	@Override
	public String asLoggableText() {
		return null;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return null;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmNavigableReference


	@Override
	public Navigable getReferencedNavigable() {
		return null;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return null;
	}

	@Override
	public String getUniqueIdentifier() {
		return null;
	}

	@Override
	public String getExplicitAlias() {
		return null;
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {

	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class getJavaType() {
		return null;
	}
}
