/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Gavin King
 */
public class SqmExtractUnit implements SqmExpression {
	private String name;
	private AllowableFunctionReturnType type;

	public SqmExtractUnit(String name, AllowableFunctionReturnType type) {
		this.type = type;
		this.name = name;
	}

	public SqmExtractUnit(String name) {
		this(name, StandardSpiBasicTypes.INTEGER);
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitExtractUnit(this);
	}

	public String getUnitName() {
		return name;
	}

	@Override
	public AllowableFunctionReturnType getExpressableType() {
		return type;
	}

	@Override
	public void applyInferableType(ExpressableType<?> type) {}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return null;
	}
}


