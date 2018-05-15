/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public class SqmCurrentTimestampFunction extends AbstractSqmFunction {
	public static final String NAME = "current_timestamp";

	public SqmCurrentTimestampFunction() {
		super( StandardSpiBasicTypes.TIME );
	}

	public SqmCurrentTimestampFunction(AllowableFunctionReturnType resultType) {
		super( resultType );
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return false;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitCurrentTimestampFunction( this );
	}

	@Override
	public String asLoggableText() {
		return NAME;
	}
}
