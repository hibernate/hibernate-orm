package org.hibernate.hql.internal.ast.exec;

import antlr.RecognitionException;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.hql.internal.ast.SqlGenerator;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.entity.Queryable;

import java.util.List;

/**
 * Executes HQL bulk updates against a single table.
 *
 * @author Gavin King
 */
public class UpdateExecutor extends BasicExecutor {
    public UpdateExecutor(HqlSqlWalker walker) {
        super( walker.getFinalFromClause().getFromElement().getQueryable() );
        try {
            SqlGenerator gen = new SqlGenerator( walker.getSessionFactoryHelper().getFactory() );
            gen.statement( walker.getAST() );
            // workaround for a problem where HqlSqlWalker actually generates
            // broken SQL with undefined aliases in the where clause, because
            // that is what MultiTableUpdateExecutor is expecting to get
            String alias = walker.getFinalFromClause().getFromElement().getTableAlias();
            sql = gen.getSQL().replace( alias + ".", "" );
            gen.getParseErrorHandler().throwQueryException();
            parameterSpecifications = gen.getCollectedParameters();
        }
        catch ( RecognitionException e ) {
            throw QuerySyntaxException.convert( e );
        }
    }
}
