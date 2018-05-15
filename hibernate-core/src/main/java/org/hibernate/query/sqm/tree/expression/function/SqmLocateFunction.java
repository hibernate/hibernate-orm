/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import java.util.Locale;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Support for the standard `locate(:patternString, :stringToSearch [, :startPosition]?)` function
 *
 * @author Steve Ebersole
 */
public class SqmLocateFunction extends AbstractSqmFunction {
	public static final String NAME = "locate";

	private final SqmExpression patternString;
	private final SqmExpression stringToSearch;
	private final SqmExpression startPosition;

	public SqmLocateFunction(
			SqmExpression patternString,
			SqmExpression stringToSearch,
			SqmExpression startPosition,
			AllowableFunctionReturnType resultType) {
		super( resultType );
		this.patternString = patternString;
		this.stringToSearch = stringToSearch;
		this.startPosition = startPosition;

		assert patternString != null;
		assert stringToSearch != null;
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	public SqmExpression getPatternString() {
		return patternString;
	}

	public SqmExpression getStringToSearch() {
		return stringToSearch;
	}

	public SqmExpression getStartPosition() {
		return startPosition;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitLocateFunction( this );
	}

	@Override
	public String asLoggableText() {
		return String.format(
				Locale.ROOT,
				"%s(%s, %s, %s)",
				NAME,
				patternString.asLoggableText(),
				stringToSearch.asLoggableText(),
				startPosition == null ? "<no-start-position>" : startPosition.asLoggableText()
		);
	}
}
