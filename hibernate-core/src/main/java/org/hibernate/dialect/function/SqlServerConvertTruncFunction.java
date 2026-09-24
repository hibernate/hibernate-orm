package org.hibernate.dialect.function;

import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;
import org.hibernate.query.sqm.tree.spi.expression.SqmExtractUnit;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Custom {@link TruncFunction} for SQL Server versions before 16,
 * which uses the custom {@link DateTruncConvertEmulation}
 *
 * @author Marco Belladelli
 */
public class SqlServerConvertTruncFunction extends TruncFunction {
	private final DateTruncConvertEmulation dateTruncEmulation;

	public SqlServerConvertTruncFunction(TypeConfiguration typeConfiguration) {
		super(
				"round(?1,0,1)",
				"round(?1,?2,1)",
				null,
				null,
				typeConfiguration
		);
		this.dateTruncEmulation = new DateTruncConvertEmulation( typeConfiguration );
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final List<SqmTypedNode<?>> args = new ArrayList<>( arguments );
		if ( arguments.size() == 2 && arguments.get( 1 ) instanceof SqmExtractUnit ) {
			// datetime truncation
			return dateTruncEmulation.generateSqmExpression(
					arguments,
					impliedResultType,
					queryEngine
			);
		}
		// numeric truncation
		return new SelfRenderingSqmFunction<>(
				this,
				numericRenderingSupport,
				args,
				impliedResultType,
				TruncArgumentsValidator.NUMERIC_VALIDATOR,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	/**
	 * Custom {@link DateTruncEmulation} that handles rendering when using the convert function to parse datetime strings
	 *
	 * @author Marco Belladelli
	 */
	private static class DateTruncConvertEmulation extends DateTruncEmulation {
		public DateTruncConvertEmulation(TypeConfiguration typeConfiguration) {
			super( "convert", typeConfiguration );
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
				ReturnableType<?> returnType,
				SqlAstTranslator<?> walker) {
			final var ddlTypeName = DdlTypeHelper.removeUnresolvedTypeArguments( DdlTypeHelper.getTypeName(
					((Expression) sqlAstArguments.get( 0 )).getExpressionType(),
					walker.getSessionFactory().getTypeConfiguration()
			) );
			final var hasOffset = ddlTypeName.toLowerCase( Locale.ROOT ).startsWith( "datetimeoffset" );
			sqlAppender.appendSql( toDateFunction );
			sqlAppender.append( '(' );
			sqlAppender.append( ddlTypeName );
			sqlAppender.append( ',' );
			if ( hasOffset ) {
				sqlAppender.append( "concat(" );
			}
			sqlAstArguments.get( 1 ).accept( walker );
			if ( hasOffset ) {
				sqlAppender.append( ",datename(tzoffset," );
				sqlAstArguments.get( 0 ).accept( walker );
				sqlAppender.append( "))" );
			}
			sqlAppender.append( ')' );
		}
	}
}
