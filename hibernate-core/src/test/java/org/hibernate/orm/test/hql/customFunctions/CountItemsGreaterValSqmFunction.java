/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql.customFunctions;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.CastFunction;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.*;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;

//tag::hql-user-defined-dialect-function-sqm-renderer[]
public class CountItemsGreaterValSqmFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
    private final CastFunction castFunction;
    private final BasicType<BigDecimal> bigDecimalType;

    public CountItemsGreaterValSqmFunction(String name, Dialect dialect, TypeConfiguration typeConfiguration) {
        super(
            name,
            FunctionKind.AGGREGATE,
            /* Function consumes 2 numeric typed args:
            - the aggregation argument
            - the bottom edge for the count predicate*/
            new ArgumentTypesValidator(StandardArgumentsValidators.exactly(2),
                    FunctionParameterType.NUMERIC,
                    FunctionParameterType.NUMERIC
            ),
            // Function returns one value - the number of items
            StandardFunctionReturnTypeResolvers.invariant(
                    typeConfiguration.getBasicTypeRegistry()
                            .resolve(StandardBasicTypes.BIG_INTEGER)
            ),
            StandardFunctionArgumentTypeResolvers.invariant(
                    typeConfiguration, NUMERIC, NUMERIC
            )
        );
        // Extracting cast function for setting input arguments to correct the type
        castFunction = new CastFunction(
                dialect,
                dialect.getPreferredSqlTypeCodeForBoolean()
        );
        bigDecimalType = typeConfiguration.getBasicTypeRegistry()
                .resolve(StandardBasicTypes.BIG_DECIMAL);
    }

    @Override
    public void render(
            SqlAppender sqlAppender,
            List<? extends SqlAstNode> sqlAstArguments,
            SqlAstTranslator<?> walker) {
        render(sqlAppender, sqlAstArguments, null, walker);
    }

    //tag::hql-user-defined-dialect-function-sqm-renderer-definition[]
    @Override
    public void render(
            SqlAppender sqlAppender,
            List<? extends SqlAstNode> sqlAstArguments,
            Predicate filter,
            SqlAstTranslator<?> translator) {
        // Renderer definition
        //end::hql-user-defined-dialect-function-sqm-renderer[]

        // Appending name of SQL function to result query
        sqlAppender.appendSql(getName());
        sqlAppender.appendSql('(');

        // Extracting 2 arguments
        final Expression first_arg = (Expression) sqlAstArguments.get(0);
        final Expression second_arg = (Expression) sqlAstArguments.get(1);

        // If JPQL contains "filter" expression, but database doesn't support it
        // then append: function_name(case when (filter_expr) then (argument) else null end)
        final boolean caseWrapper = filter != null && !translator.supportsFilterClause();
        if (caseWrapper) {
            translator.getCurrentClauseStack().push(Clause.WHERE);
            sqlAppender.appendSql("case when ");

            filter.accept(translator);
            translator.getCurrentClauseStack().pop();

            sqlAppender.appendSql(" then ");
            renderArgument(sqlAppender, translator, first_arg);
            sqlAppender.appendSql(" else null end)");
        } else {
            renderArgument(sqlAppender, translator, first_arg);
            sqlAppender.appendSql(", ");
            renderArgument(sqlAppender, translator, second_arg);
            sqlAppender.appendSql(')');
            if (filter != null) {
                translator.getCurrentClauseStack().push(Clause.WHERE);
                sqlAppender.appendSql(" filter (where ");

                filter.accept(translator);
                sqlAppender.appendSql(')');
                translator.getCurrentClauseStack().pop();
            }
        }
        //tag::hql-user-defined-dialect-function-sqm-renderer[]
    }

    //end::hql-user-defined-dialect-function-sqm-renderer[]
    private void renderArgument(
            SqlAppender sqlAppender,
            SqlAstTranslator<?> translator,
            Expression arg) {
        // Extracting the type of argument
        final JdbcMapping sourceMapping = arg.getExpressionType().getJdbcMappings().get(0);
        if (sourceMapping.getJdbcType().isNumber()) {
            castFunction.render(sqlAppender,
                    Arrays.asList(arg, new CastTarget(bigDecimalType)),
                    translator
            );
        } else {
            arg.accept(translator);
        }
    }
    //tag::hql-user-defined-dialect-function-sqm-renderer[]
    //end::hql-user-defined-dialect-function-sqm-renderer-definition[]
}
//end::hql-user-defined-dialect-function-sqm-renderer[]
