package org.hibernate.dialect.function;

import org.hibernate.Internal;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING_OR_CLOB;

/**
 * Case conversion for ordinary strings and XMLQUERY/XMLCAST for LOBs,
 * avoiding a length-limited conversion to VARCHAR.
 * <p>
 * The LOB emulation inherits Db2's XMLQUERY restrictions: it cannot appear in
 * JOIN or MERGE ON conditions, GROUP BY clauses, or aggregate function arguments.
 * <p>
 */
@Internal
public final class DB2CaseConversionFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
	public DB2CaseConversionFunction(String name, TypeConfiguration typeConfiguration) {
		super(
				name,
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 1 ), STRING_OR_CLOB ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final var argument = (Expression) arguments.get( 0 );
		if ( isLob( argument ) ) {
			sqlAppender.appendSql( "case when " );
			walker.render( argument, SqlAstNodeRenderingMode.DEFAULT );
			sqlAppender.appendSql( " is null then null else xmlcast(xmlquery('for $s in $v return fn:" );
			sqlAppender.appendSql( getName() );
			sqlAppender.appendSql( "-case($s)' passing " );
			walker.render( argument, SqlAstNodeRenderingMode.DEFAULT );
			sqlAppender.appendSql( " as \"v\") as clob(2g)) end" );
		}
		else {
			sqlAppender.appendSql( getName() );
			sqlAppender.appendSql( '(' );
			walker.render( argument, SqlAstNodeRenderingMode.DEFAULT );
			sqlAppender.appendSql( ')' );
		}
	}

	@Override
	public String getSignature(String name) {
		return "(STRING_OR_CLOB string)";
	}

	private static boolean isLob(Expression argument) {
		final int ddlTypeCode = argument.getExpressionType().getSingleJdbcMapping().getJdbcType().getDdlTypeCode();
		if ( ddlTypeCode == SqlTypes.CLOB || ddlTypeCode == SqlTypes.NCLOB
			|| ddlTypeCode == SqlTypes.LONG32VARCHAR || ddlTypeCode == SqlTypes.LONG32NVARCHAR ) {
			return true;
		}
		if ( argument instanceof SelfRenderingFunctionSqlAstExpression<?> function ) {
			if ( function.getFunctionRenderer() instanceof DB2CaseConversionFunction ) {
				return isLob( (Expression) function.getArguments().get( 0 ) );
			}
			if ( function.getFunctionName().equals( "coalesce" ) ) {
				for ( var operand : function.getArguments() ) {
					if ( isLob( (Expression) operand ) ) {
						return true;
					}
				}
			}
		}
		else if ( argument instanceof CaseSearchedExpression caseExpression ) {
			for ( var when : caseExpression.getWhenFragments() ) {
				if ( isLob( when.getResult() ) ) {
					return true;
				}
			}
			return caseExpression.getOtherwise() != null && isLob( caseExpression.getOtherwise() );
		}
		else if ( argument instanceof CaseSimpleExpression caseExpression ) {
			for ( var when : caseExpression.getWhenFragments() ) {
				if ( isLob( when.getResult() ) ) {
					return true;
				}
			}
			return caseExpression.getOtherwise() != null && isLob( caseExpression.getOtherwise() );
		}
		else if ( argument instanceof SelectStatement selectStatement ) {
			return selectStatement.getQueryPart().queryQuerySpecs( querySpec -> {
				final var selections = querySpec.getSelectClause().getSqlSelections();
				return selections.size() == 1 && isLob( selections.get( 0 ).getExpression() )
						? Boolean.TRUE
						: null;
			} ) != null;
		}
		return false;
	}

}
