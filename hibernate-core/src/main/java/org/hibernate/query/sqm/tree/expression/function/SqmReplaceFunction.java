/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import java.util.Locale;

/**
 * Support for the standard `locate(:patternString, :stringToSearch [, :replacementString]?)` function
 *
 * @author Steve Ebersole
 */
public class SqmReplaceFunction<T> extends AbstractSqmFunction<T> {
	public static final String NAME = "locate";

	private final SqmExpression stringToSearch;
	private final SqmExpression patternString;
	private final SqmExpression replacementString;

	public SqmReplaceFunction(
			SqmExpression<?> stringToSearch,
			SqmExpression<?> patternString,
			SqmExpression<?> replacementString,
			AllowableFunctionReturnType<T> resultType,
			NodeBuilder nodeBuilder) {
		super( resultType, nodeBuilder );
		this.patternString = patternString;
		this.stringToSearch = stringToSearch;
		this.replacementString = replacementString;

		assert patternString != null;
		assert stringToSearch != null;
		assert replacementString != null;
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

	public SqmExpression getReplacementString() {
		return replacementString;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitReplaceFunction( this );
	}

	@Override
	public String asLoggableText() {
		return String.format(
				Locale.ROOT,
				"%s(%s, %s, %s)",
				NAME,
				patternString.asLoggableText(),
				stringToSearch.asLoggableText(),
				replacementString == null ? "<no-start-position>" : replacementString.asLoggableText()
		);
	}
}
