/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.FunctionRenderingSupport;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

public class OracleArrayConstructorFunction extends ArrayConstructorFunction {

	public OracleArrayConstructorFunction() {
		super( false );
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new ArrayConstructorSqmFunction<>(
				this,
				this,
				arguments,
				impliedResultType,
				getArgumentsValidator(),
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	protected static class ArrayConstructorSqmFunction<T> extends SelfRenderingSqmFunction<T> {
		public ArrayConstructorSqmFunction(
				OracleArrayConstructorFunction descriptor,
				FunctionRenderingSupport renderingSupport,
				List<? extends SqmTypedNode<?>> arguments,
				ReturnableType<T> impliedResultType,
				ArgumentsValidator argumentsValidator,
				FunctionReturnTypeResolver returnTypeResolver,
				NodeBuilder nodeBuilder,
				String name) {
			super(
					descriptor,
					renderingSupport,
					arguments,
					impliedResultType,
					argumentsValidator,
					returnTypeResolver,
					nodeBuilder,
					name
			);
		}

		@Override
		public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
			final ReturnableType<?> resultType = resolveResultType( walker );

			List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
			if ( getArgumentsValidator() != null ) {
				getArgumentsValidator().validateSqlTypes( arguments, getFunctionName() );
			}
			if ( resultType == null ) {
				throw new SemanticException(
						"Oracle array constructor emulation requires knowledge about the return type, but resolved return type could not be determined"
				);
			}
			final DomainType<?> type = resultType.getSqmType();
			if ( !( type instanceof BasicPluralType<?, ?> ) ) {
				throw new SemanticException(
						"Oracle array constructor emulation requires a basic plural return type, but resolved return type was: " + type
				);
			}
			final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) type;
			final TypeConfiguration typeConfiguration = walker.getCreationContext().getSessionFactory().getTypeConfiguration();
			final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
			final DdlType ddlType = ddlTypeRegistry.getDescriptor(
					pluralType.getJdbcType().getDdlTypeCode()
			);
			final String arrayTypeName = ddlType.getCastTypeName( Size.nil(), pluralType, ddlTypeRegistry );
			return new SelfRenderingFunctionSqlAstExpression(
					getFunctionName(),
					getRenderingSupport(),
					arguments,
					resultType,
					getMappingModelExpressible( walker, resultType, arguments )
			) {
				@Override
				public void renderToSql(
						SqlAppender sqlAppender,
						SqlAstTranslator<?> walker,
						SessionFactoryImplementor sessionFactory) {
					sqlAppender.appendSql( arrayTypeName );
					final List<? extends SqlAstNode> arguments = getArguments();
					final int size = arguments.size();
					if ( size == 0 ) {
						sqlAppender.append( '(' );
					}
					else {
						char separator = '(';
						for ( int i = 0; i < size; i++ ) {
							SqlAstNode argument = arguments.get( i );
							sqlAppender.append( separator );
							argument.accept( walker );
							separator = ',';
						}
					}
					sqlAppender.append( ')' );
				}
			};
		}
	}
}
