/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import java.util.function.Supplier;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.EnumJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmStaticEnumReference<T> implements SemanticPathPart, SqmExpression, ExpressableType {
	private final Enum referencedEnum;
	private final BasicJavaDescriptor jtd;

	public SqmStaticEnumReference(Enum referencedEnum, EnumJavaDescriptor jtd) {
		this.referencedEnum = referencedEnum;
		this.jtd = jtd;
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
	public ExpressableType getExpressableType() {
		return this;
	}

	@Override
	public Supplier<? extends ExpressableType> getInferableType() {
		return this::getExpressableType;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitFullyQualifiedEnum( referencedEnum );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return jtd;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}
}
