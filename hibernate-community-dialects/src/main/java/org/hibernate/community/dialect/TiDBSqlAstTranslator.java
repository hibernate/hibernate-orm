package org.hibernate.community.dialect;

import org.hibernate.dialect.sql.ast.internal.MySQLSqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.StandardDerivedTableRenderingSupport;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.exec.spi.JdbcOperation;

/**
 * A SQL AST translator for TiDB.
 *
 * @author Christian Beikov
 * @author Cong Wang
 */
public class TiDBSqlAstTranslator<T extends JdbcOperation> extends MySQLSqlAstTranslator<T> {

	public TiDBSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request, TiDBDialect dialect) {
		super( request, dialect );
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		// TiDB does not support a column list on derived tables, i.e. `(select ...) t(c1,c2)`
		// See https://github.com/pingcap/tidb/issues/65151
		return StandardDerivedTableRenderingSupport.QUERY_AND_VALUES_SELECT_LIST;
	}

	@Override
	protected void renderNewRowAlias() {
		// TiDB does not support referencing a row alias for the inserted values, so use values(col) instead
		// See https://github.com/pingcap/tidb/issues/70814
	}

	@Override
	protected void renderUpdateValue(ColumnValueBinding columnValueBinding) {
		appendSql( "values(" );
		appendSql( columnValueBinding.getColumnReference().getColumnExpression() );
		appendSql( ")" );
	}
}
