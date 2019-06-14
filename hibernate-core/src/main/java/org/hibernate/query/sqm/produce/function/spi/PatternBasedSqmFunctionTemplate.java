/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;

/**
 * Support for HQL functions that have different representations
 * in different SQL dialects, where the difference can be handled
 * via a pattern template.
 * <p/>
 * In HQL we might define a function {@code concat(?1, ?2)} to
 * concatenate two strings p1 and p2. Dialects register different
 * instances of this class using the same name (concat) but with
 * different templates or patterns: {@code (?1 || ?2)} for Oracle,
 * {@code concat(?1, ?2)} for MySQL, {@code (?1 + ?2)} for SQL
 * Server. Each dialect defines a template as a string exactly as
 * shown above, marking each function parameter with '?' followed
 * by the parameter index. Parameters are indexed from 1. The
 * last parameter may be a vararg, indicated with the syntax
 * {@code (?1 || ?2...)}.
 *
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 */
public class PatternBasedSqmFunctionTemplate
		extends AbstractSelfRenderingFunctionTemplate
		implements SelfRenderingFunctionSupport {
	private final PatternRenderer renderer;
	private final String argumentListSignature;

	/**
	 * Constructs a pattern-based function template
	 */
	public PatternBasedSqmFunctionTemplate(
			PatternRenderer renderer,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			String name,
			String argumentListSignature) {
		super(
				name,
				returnTypeResolver,
				argumentsValidator != null
						? argumentsValidator
						// If no validator is given, it's still better to
						// validate against the parameter count as given
						// by the pattern than accepting every input
						// blindly and producing wrong output
						: renderer.hasVarargs()
						? StandardArgumentsValidators.min( renderer.getParamCount() )
						: StandardArgumentsValidators.exactly( renderer.getParamCount() )
		);
		this.renderer = renderer;
		this.argumentListSignature = argumentListSignature;
	}

	@Override
	protected SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<?> impliedResultType,
			QueryEngine queryEngine) {
		return this;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			SqlAstWalker walker) {
		renderer.render( sqlAppender, sqlAstArguments, walker );
	}

	@Override
	public String getArgumentListSignature() {
		return argumentListSignature==null ? super.getArgumentListSignature() : argumentListSignature;
	}
}
