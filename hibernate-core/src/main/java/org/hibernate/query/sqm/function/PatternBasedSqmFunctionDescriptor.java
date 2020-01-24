/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;

/**
 * A {@link SqmFunctionDescriptor function descriptor} for functions
 * which produce SQL in a non-standard form (something other than
 * {@code f(x, y, z)}) which can be represented as a pattern.
 *
 * The pattern may contain numbered placeholders for function
 * arguments, for example {@code (?1 || ?2)}.
 *
 * The placeholders may change the order of the given arguments, for
 * example {@code concat(?2,?1)}.
 *
 * The last parameter may be variadic, indicated with the syntax
 * {@code (?1 || ?2...)}, meaning that the pattern accepts an
 * arbitrary number of arguments.
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 */
public class PatternBasedSqmFunctionDescriptor
		extends AbstractSqmSelfRenderingFunctionDescriptor
		implements FunctionRenderingSupport {
	private final PatternRenderer renderer;
	private final String argumentListSignature;

	/**
	 * Constructs a pattern-based function template
	 */
	public PatternBasedSqmFunctionDescriptor(
			String functionName, PatternRenderer renderer,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			String argumentListSignature) {
		super(functionName,
				argumentsValidator != null
					? argumentsValidator
					// If no validator is given, it's still better to
					// validate against the parameter count as given
					// by the pattern than accepting every input
					// blindly and producing wrong output
					: renderer.hasVarargs()
						? StandardArgumentsValidators.min( renderer.getParamCount() )
						: StandardArgumentsValidators.exactly( renderer.getParamCount() ),
				returnTypeResolver );
		this.renderer = renderer;
		this.argumentListSignature = argumentListSignature;
	}

	@Override
	public FunctionRenderingSupport getRenderingSupport() {
		return this;
	}

	@Override
	public void render(SqlAppender sqlAppender, String functionName, List<SqlAstNode> sqlAstArguments, SqlAstWalker walker, SessionFactoryImplementor sessionFactory) {
		renderer.render( sqlAppender, sqlAstArguments, walker, sessionFactory );
	}

	@Override
	public String getArgumentListSignature() {
		return argumentListSignature==null ? super.getArgumentListSignature() : argumentListSignature;
	}
}
