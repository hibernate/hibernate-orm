/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.community.dialect.DB2zLegacyDialect.DB2_LUW_VERSION9;


/**
 * A SQL AST translator for DB2z.
 *
 * @author Christian Beikov
 */
public class DB2zLegacySqlAstTranslator<T extends JdbcOperation> extends DB2LegacySqlAstTranslator<T> {

	private final DatabaseVersion version;

	public DB2zLegacySqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement, DatabaseVersion version) {
		super( sessionFactory, statement );
		this.version = version;
	}

	@Override
	protected boolean shouldEmulateFetchClause(QueryPart queryPart) {
		// Percent fetches or ties fetches aren't supported in DB2 z/OS
		// Also, variable limit isn't supported before 12.0
		return getQueryPartForRowNumbering() != queryPart && (
				useOffsetFetchClause( queryPart ) && !isRowsOnlyFetchClauseType( queryPart )
						|| version.isBefore(12) && queryPart.isRoot() && hasLimit()
						|| version.isBefore(12) && queryPart.getFetchClauseExpression() != null && !( queryPart.getFetchClauseExpression() instanceof Literal )
		);
	}

	@Override
	protected void renderComparison(Expression lhs, ComparisonOperator operator, Expression rhs) {
		// Supported at least since DB2 z/OS 9.0
		renderComparisonStandard( lhs, operator, rhs );
	}

	@Override
	protected void renderComparisonStandard(Expression lhs, ComparisonOperator operator, Expression rhs) {
		final JdbcMappingContainer lhsExpressionType = lhs.getExpressionType();
		final int ddlTypeCode = lhsExpressionType != null && lhsExpressionType.getJdbcTypeCount() == 1
				? lhsExpressionType.getSingleJdbcMapping().getJdbcType().getDdlTypeCode()
				: SqlTypes.VARCHAR;
		if ( ddlTypeCode == SqlTypes.SQLXML || JdbcType.isLob( ddlTypeCode ) ) {
			// In DB2 z/OS, XMLTYPE is not "comparable", so we have to cast the two parts to varchar for this purpose
			switch ( operator ) {
				case EQUAL:
				case NOT_DISTINCT_FROM:
				case NOT_EQUAL:
				case DISTINCT_FROM:
					if ( ddlTypeCode == SqlTypes.SQLXML ) {
						appendSql( "cast(xmlserialize(" );
						lhs.accept( this );
						appendSql( " as clob) as varchar(32672))" );
						appendSql( operator.sqlText() );
						appendSql( "cast(xmlserialize(" );
						rhs.accept( this );
						appendSql( " as clob) as varchar(32672))" );
					}
					else if ( ddlTypeCode == SqlTypes.BLOB ) {
						appendSql( "cast(" );
						lhs.accept( this );
						appendSql( " as varbinary(32672))" );
						appendSql( operator.sqlText() );
						appendSql( "cast(" );
						rhs.accept( this );
						appendSql( " as varbinary(32672))" );
					}
					else {
						appendSql( "cast(cast(" );
						lhs.accept( this );
						appendSql( " as clob) as varchar(32672))" );
						appendSql( operator.sqlText() );
						appendSql( "cast(cast(" );
						rhs.accept( this );
						appendSql( " as clob) as varchar(32672))" );
					}
					return;
				default:
					// Fall through
					break;
			}
		}
		super.renderComparisonStandard( lhs, operator, rhs );
	}

	@Override
	public void visitQueryPartTableReference(QueryPartTableReference tableReference) {
		// DB2 z/OS we need the "table" qualifier for table valued functions or lateral sub-queries
		append( "table " );
		super.visitQueryPartTableReference( tableReference );
	}

	@Override
	public void visitValuesTableReference(ValuesTableReference tableReference) {
		// DB2 z/OS doesn't support the VALUES clause in the FROM clause
		emulateValuesTableReferenceColumnAliasing( tableReference );
	}

	@Override
	public void visitBooleanExpressionPredicate(BooleanExpressionPredicate booleanExpressionPredicate) {
		// DB2 z/OS do not support boolean expressions in a predicate context, so we render `expr=true`
		appendSql( '(' );
		booleanExpressionPredicate.getExpression().accept( this );
		appendSql( '=' );
		getDialect().appendBooleanValueString( this, !booleanExpressionPredicate.isNegated() );
		appendSql( ')' );
	}

	@Override
	protected void renderExpressionAsClauseItem(Expression expression) {
		// DB2 z/OS does not support predicates as top-level items
		if ( expression instanceof Predicate ) {
			appendSql( "case when " );
			expression.accept( this );
			appendSql( " then " );
			getDialect().appendBooleanValueString( this, true );
			appendSql( " else " );
			getDialect().appendBooleanValueString( this, false );
			appendSql( " end" );
		}
		else {
			expression.accept( this );
		}
	}

	@Override
	protected String getNewTableChangeModifier() {
		// On DB2 zOS, `final` also sees the trigger data
		return "final";
	}

	@Override
	protected boolean preferUnionQueryForTupleInListPredicate() {
		// DB2 z/OS can't use an index when rendering a union query
		return false;
	}

	@Override
	protected boolean supportsFromClauseInUpdate() {
		return false;
	}

	@Override
	protected boolean supportsOffsetClause() {
		return version.isSameOrAfter( 12, 1, 500 );
	}

	@Override
	protected boolean supportsOffsetClause(QueryPart queryPart) {
		// No support for offset in subqueries
		return supportsOffsetClause() && getCurrentQueryPart().isRoot();
	}

	@Override
	public DatabaseVersion getDB2Version() {
		return DB2_LUW_VERSION9;
	}
}
