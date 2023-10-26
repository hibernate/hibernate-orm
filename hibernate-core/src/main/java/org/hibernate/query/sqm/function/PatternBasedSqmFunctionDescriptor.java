/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;

import java.util.List;

/**
 * Support for HQL functions that have different representations
 * in different SQL dialects, where the difference can be handled
 * via a pattern template.
 * <p>
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
 * @author Alexey Loubyansky
 */
public class PatternBasedSqmFunctionDescriptor
		extends AbstractSqmSelfRenderingFunctionDescriptor {
	private final PatternRenderer renderer;
	private final String argumentListSignature;

	/**
	 * Constructs a pattern-based function template
	 */
	public PatternBasedSqmFunctionDescriptor(
			PatternRenderer renderer,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			FunctionArgumentTypeResolver argumentTypeResolver,
			String name,
			FunctionKind functionKind,
			String argumentListSignature) {
		super(
				name,
				functionKind,
				argumentsValidator != null
						? argumentsValidator
						// If no validator is given, it's still better to
						// validate against the parameter count as given
						// by the pattern than accepting every input
						// blindly and producing wrong output
						: renderer.hasVarargs()
						? StandardArgumentsValidators.min( renderer.getParamCount() )
						: StandardArgumentsValidators.exactly( renderer.getParamCount() ),
				returnTypeResolver,
				argumentTypeResolver
		);
		this.renderer = renderer;
		this.argumentListSignature = argumentListSignature;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		renderer.render( sqlAppender, sqlAstArguments, null, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		renderer.render( sqlAppender, sqlAstArguments, filter, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		renderer.render( sqlAppender, sqlAstArguments, filter, withinGroup, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		renderer.render( sqlAppender, sqlAstArguments, filter, respectNulls, fromFirst, walker );
	}

	@Override
	public String getArgumentListSignature() {
		return argumentListSignature == null ? super.getArgumentListSignature() : argumentListSignature;
	}
}
