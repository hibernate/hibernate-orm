package org.hibernate.hql.internal.ast.exec;

import antlr.RecognitionException;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.hql.internal.ast.tree.InsertStatement;
import org.hibernate.hql.internal.ast.tree.Statement;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;

import java.util.List;

/**
 * Executes HQL insert statements.
 *
 * @author Gavin King
 */
public class InsertExecutor extends BasicExecutor {
    public InsertExecutor(HqlSqlWalker walker) {
        super( ( (InsertStatement) walker.getAST() ).getIntoClause().getQueryable() );
        try {
            SqlGenerator gen = new SqlGenerator( walker.getSessionFactoryHelper().getFactory() );
            gen.statement( walker.getAST() );
            sql = gen.getSQL();
            gen.getParseErrorHandler().throwQueryException();
            parameterSpecifications = gen.getCollectedParameters();
        }
        catch ( RecognitionException e ) {
            throw QuerySyntaxException.convert( e );
        }
    }
}
