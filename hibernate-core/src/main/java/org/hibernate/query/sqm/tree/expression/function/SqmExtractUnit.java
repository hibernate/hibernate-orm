/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Gavin King
 */
public class SqmExtractUnit extends AbstractSqmExpression {
	private String name;

	public SqmExtractUnit(String name, ExpressableType<?> type) {
		super(type);
		this.name = name;
	}

	public SqmExtractUnit(String name) {
		this(name, StandardSpiBasicTypes.INTEGER);
	}

	public String getUnitName() {
		return name;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitExtractUnit(this);
	}
}
