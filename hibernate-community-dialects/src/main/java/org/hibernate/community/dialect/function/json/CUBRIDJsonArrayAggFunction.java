package org.hibernate.community.dialect.function.json;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.community.dialect.aggregate.internal.CUBRIDAggregateSupport;
import org.hibernate.dialect.function.json.JsonArrayAggFunction;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.Distinct;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JsonNullBehavior;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.translation.Clause;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * CUBRID json_arrayagg function.
 */
public class CUBRIDJsonArrayAggFunction extends JsonArrayAggFunction {

	public CUBRIDJsonArrayAggFunction(TypeConfiguration typeConfiguration) {
		super( false, typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		if ( withinGroup != null && !withinGroup.isEmpty() ) {
			// group_concat can only be ordered by its own argument
			throw new QueryException( "Can't emulate json_arrayagg 'order by' clause.", (String) null );
		}
		final JsonNullBehavior nullBehavior = sqlAstArguments.size() > 1
				? (JsonNullBehavior) sqlAstArguments.get( 1 )
				: JsonNullBehavior.ABSENT;
		// group_concat rejects JSON values, and a JSON string turned into text loses its quotes,
		// so concatenate one-element arrays and extract their elements
		sqlAppender.appendSql( "json_extract(concat('[',group_concat(" );
		final SqlAstNode firstArg = sqlAstArguments.get( 0 );
		final Expression arg;
		if ( firstArg instanceof Distinct distinct ) {
			sqlAppender.appendSql( "distinct " );
			arg = distinct.getExpression();
		}
		else {
			arg = (Expression) firstArg;
		}
		if ( filter != null ) {
			if ( nullBehavior != JsonNullBehavior.ABSENT ) {
				throw new QueryException( "Can't emulate json_arrayagg filter clause when using 'null on null' clause.", (String) null );
			}
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			translator.getCurrentClauseStack().pop();
			sqlAppender.appendSql( " then " );
			renderArgument( sqlAppender, arg, nullBehavior, translator );
			sqlAppender.appendSql( " else null end" );
		}
		else {
			renderArgument( sqlAppender, arg, nullBehavior, translator );
		}
		sqlAppender.appendSql( " separator ','),']'),'$[*][0]')" );
	}

	@Override
	protected void renderArgument(
			SqlAppender sqlAppender,
			Expression arg,
			JsonNullBehavior nullBehavior,
			SqlAstTranslator<?> translator) {
		if ( nullBehavior != JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( "nullif(" );
		}
		sqlAppender.appendSql( "concat(json_array(" );
		CUBRIDAggregateSupport.INSTANCE.appendJsonWriteExpression(
				sqlAppender,
				() -> arg.accept( translator ),
				arg.getExpressionType().getSingleJdbcMapping()
		);
		sqlAppender.appendSql( "))" );
		if ( nullBehavior != JsonNullBehavior.NULL ) {
			sqlAppender.appendSql( ",'[null]')" );
		}
	}
}
