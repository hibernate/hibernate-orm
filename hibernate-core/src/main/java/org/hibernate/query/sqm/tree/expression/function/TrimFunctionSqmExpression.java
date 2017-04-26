/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.type.SqmDomainTypeBasic;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class TrimFunctionSqmExpression extends AbstractFunctionSqmExpression {
	public static final String NAME = "trim";

	public enum Specification {
		LEADING,
		TRAILING,
		BOTH
	}

	private final Specification specification;
	private final SqmExpression trimCharacter;
	private final SqmExpression source;

	public TrimFunctionSqmExpression(
			BasicValuedExpressableType resultType,
			Specification specification,
			SqmExpression trimCharacter,
			SqmExpression source) {
		super( resultType );
		this.specification = specification;
		this.trimCharacter = trimCharacter;
		this.source = source;

		assert specification != null;
		assert trimCharacter != null;
		assert source != null;
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitTrimFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "TRIM(" + specification.name() +
				" '" + trimCharacter.asLoggableText() +
				"' FROM " + source.asLoggableText() + ")";
	}
}
