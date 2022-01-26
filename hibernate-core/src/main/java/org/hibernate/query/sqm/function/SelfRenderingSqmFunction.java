/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.emptyList;

/**
 * @author Steve Ebersole
 */
public class SelfRenderingSqmFunction<T> extends SqmFunction<T> {
	private final ReturnableType<T> impliedResultType;
	private final ArgumentsValidator argumentsValidator;
	private final FunctionReturnTypeResolver returnTypeResolver;
	private final FunctionRenderingSupport renderingSupport;
	private ReturnableType<?> resultType;

	public SelfRenderingSqmFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderingSupport renderingSupport,
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super( name, descriptor, impliedResultType, arguments, nodeBuilder );
		this.renderingSupport = renderingSupport;
		this.impliedResultType = impliedResultType;
		this.argumentsValidator = argumentsValidator;
		this.returnTypeResolver = returnTypeResolver;
	}

	public FunctionRenderingSupport getRenderingSupport() {
		return renderingSupport;
	}

	protected ArgumentsValidator getArgumentsValidator() {
		return argumentsValidator;
	}

	protected static List<SqlAstNode> resolveSqlAstArguments(List<? extends SqmTypedNode<?>> sqmArguments, SqmToSqlAstConverter walker) {
		if ( sqmArguments == null || sqmArguments.isEmpty() ) {
			return emptyList();
		}

		final ArrayList<SqlAstNode> sqlAstArguments = new ArrayList<>( sqmArguments.size() );
		for ( SqmTypedNode<?> sqmArgument : sqmArguments ) {
			sqlAstArguments.add( (SqlAstNode) ( (SqmVisitableNode) sqmArgument ).accept( walker ) );
		}
		return sqlAstArguments;
	}

	@Override
	public SelfRenderingFunctionSqlAstExpression convertToSqlAst(SqmToSqlAstConverter walker) {
		final ReturnableType<?> resultType = resolveResultType(
				walker.getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);

		List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
		if ( argumentsValidator != null ) {
			argumentsValidator.validateSqlTypes( arguments, getFunctionName() );
		}
		return new SelfRenderingFunctionSqlAstExpression(
				getFunctionName(),
				getRenderingSupport(),
				arguments,
				resultType,
				resultType == null ? null : getMappingModelExpressible( walker, resultType )
		);
	}

	public SqmExpressible<T> getNodeType() {
		SqmExpressible<T> nodeType = super.getNodeType();
		if ( nodeType == null ) {
			nodeType = (SqmExpressible<T>) resolveResultType( nodeBuilder().getTypeConfiguration() );
		}

		return nodeType;
	}

	protected ReturnableType<?> resolveResultType(TypeConfiguration typeConfiguration) {
		if ( resultType == null ) {
			resultType = returnTypeResolver.resolveFunctionReturnType(
				impliedResultType,
				getArguments(),
				typeConfiguration
			);
			setExpressibleType( resultType );
		}
		return resultType;
	}

	protected MappingModelExpressible<?> getMappingModelExpressible(
			SqmToSqlAstConverter walker,
			ReturnableType<?> resultType) {
		MappingModelExpressible<?> mapping;
		if ( resultType instanceof MappingModelExpressible) {
			// here we have a BasicType, which can be cast
			// directly to BasicValuedMapping
			mapping = (MappingModelExpressible<?>) resultType;
		}
		else {
			// here we have something that is not a BasicType,
			// and we have no way to get a BasicValuedMapping
			// from it directly
			mapping = returnTypeResolver.resolveFunctionReturnType(
					() -> {
						try {
							final MappingMetamodelImplementor domainModel = walker.getCreationContext()
									.getSessionFactory()
									.getRuntimeMetamodels()
									.getMappingMetamodel();
							return (BasicValuedMapping) domainModel.resolveMappingExpressible(
									getNodeType(),
									walker.getFromClauseAccess()::getTableGroup
							);
						}
						catch (Exception e) {
							return null; // this works at least approximately
						}
					},
					resolveSqlAstArguments( getArguments(), walker )
			);
		}
		return mapping;
	}

}
