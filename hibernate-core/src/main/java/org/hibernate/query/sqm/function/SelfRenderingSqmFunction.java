/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.emptyList;

/**
 * @author Steve Ebersole
 */
public class SelfRenderingSqmFunction<T> extends SqmFunction<T> {
	private final AllowableFunctionReturnType<T> impliedResultType;
	private final ArgumentsValidator argumentsValidator;
	private final FunctionReturnTypeResolver returnTypeResolver;
	private final FunctionRenderingSupport renderingSupport;
	private AllowableFunctionReturnType<?> resultType;

	public SelfRenderingSqmFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderingSupport renderingSupport,
			List<? extends SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
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
		final AllowableFunctionReturnType<?> resultType = resolveResultType(
				walker.getCreationContext().getDomainModel().getTypeConfiguration()
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
				resultType == null ? null : getMappingModelExpressable( walker, resultType )
		);
	}

	public SqmExpressable<T> getNodeType() {
		SqmExpressable<T> nodeType = super.getNodeType();
		if ( nodeType == null ) {
			nodeType = (SqmExpressable<T>) resolveResultType( nodeBuilder().getTypeConfiguration() );
		}

		return nodeType;
	}

	protected AllowableFunctionReturnType<?> resolveResultType(TypeConfiguration typeConfiguration) {
		if ( resultType == null ) {
			resultType = returnTypeResolver.resolveFunctionReturnType(
				impliedResultType,
				getArguments(),
				typeConfiguration
			);
			setExpressableType( resultType );
		}
		return resultType;
	}

	protected MappingModelExpressable<?> getMappingModelExpressable(
			SqmToSqlAstConverter walker,
			AllowableFunctionReturnType<?> resultType) {
		MappingModelExpressable<?> mapping;
		if ( resultType instanceof MappingModelExpressable ) {
			// here we have a BasicType, which can be cast
			// directly to BasicValuedMapping
			mapping = (MappingModelExpressable<?>) resultType;
		}
		else {
			// here we have something that is not a BasicType,
			// and we have no way to get a BasicValuedMapping
			// from it directly
			mapping = returnTypeResolver.resolveFunctionReturnType(
					() -> {
						try {
							final MappingMetamodel domainModel = walker.getCreationContext().getDomainModel();
							return (BasicValuedMapping) domainModel.resolveMappingExpressable(
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
