/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Support for SQM {@link SqmFunctionDescriptor function descriptors}
 * with type information defined by an {@link ArgumentsValidator} and
 * a {@link FunctionReturnTypeResolver}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFunctionDescriptor implements SqmFunctionDescriptor {
	final ArgumentsValidator argumentsValidator;
	final FunctionReturnTypeResolver returnTypeResolver;
	final String functionName;

	public AbstractSqmFunctionDescriptor(
			String functionName,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		this.functionName = functionName;
		this.argumentsValidator = argumentsValidator == null
				? StandardArgumentsValidators.NONE
				: argumentsValidator;
		this.returnTypeResolver = returnTypeResolver == null
				? (impliedTypeAccess, arguments) -> impliedTypeAccess.get()
				: returnTypeResolver;
	}

	public String getSignature(String functionName) {
		return getReturnSignature() + functionName + getArgumentListSignature();
	}

	/**
	 * The return type of the function in a format suitable
	 * for display to the user.
	 */
	public String getReturnSignature() {
		String result = returnTypeResolver.getReturnType();
		return result.isEmpty() ? "" : result + " ";
	}

	/**
	 * The argument list of the function in a format suitable
	 * for display to the user.
	 */
	public String getArgumentListSignature() {
		String args = argumentsValidator.getSignature();
		return requiresArgumentList() ? args : "()".equals(args) ? "" : "[" + args + "]";
	}

	public String getFunctionName() {
		return functionName;
	}

	/**
	 * Validate the given {@code sqmArguments} and generate a representation
	 * of the described function as a SQL AST node.
	 */
	@Override
	public Expression generateSqlExpression(
			String functionName,
			List<? extends SqmVisitableNode> sqmArguments,
			Supplier<MappingModelExpressable> inferableTypeAccess,
			SqmToSqlAstConverter converter,
			SqlAstCreationState creationState) {
		argumentsValidator.validate( sqmArguments );

		// todo (6.0) : work out the specifics of the type resolution

		final List<SqlAstNode> arguments;
		int argumentCount = sqmArguments == null ? 0 : sqmArguments.size();
		switch ( argumentCount) {
			case 0:
				arguments = Collections.emptyList();
				break;
			case 1:
				SqlAstNode argument = (SqlAstNode) sqmArguments.get(0).accept( converter );
				arguments = Collections.singletonList( argument );
				break;
			default:
				arguments = new ArrayList<>( sqmArguments.size() );
				for (SqmVisitableNode sqmVisitableNode: sqmArguments) {
					SqlAstNode arg = (SqlAstNode) sqmVisitableNode.accept( converter );
					arguments.add( arg );
				}
		}

		final BasicValuedMapping returnType =
				returnTypeResolver.resolveFunctionReturnType(
						() -> (BasicValuedMapping) inferableTypeAccess.get(),
						arguments
				);

		return generateFunctionExpression( arguments, returnType );
	}

	protected abstract Expression generateFunctionExpression(
			List<SqlAstNode> sqlAstArgs,
			BasicValuedMapping returnType);

}
