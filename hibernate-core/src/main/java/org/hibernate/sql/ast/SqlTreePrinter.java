/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast;

import java.util.List;

import org.hibernate.sql.ast.tree.SqlAstTreeLogger;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.FunctionTableReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.ValuesTableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.UpdateStatement;

/**
 * Logs a debug representation of the SQL AST.
 *
 * @implNote At the moment, we only render the from-elements.
 *
 * @author Steve Ebersole
 */
public class SqlTreePrinter {
	public static void logSqlAst(Statement sqlAstStatement) {
		if ( SqlAstTreeLogger.INSTANCE.isTraceEnabled() ) {
			final SqlTreePrinter printer = new SqlTreePrinter();
			printer.visitStatement( sqlAstStatement );
			SqlAstTreeLogger.INSTANCE.tracef( "SQL AST Tree:%n%s", printer.buffer );
		}
	}

	private final StringBuffer buffer = new StringBuffer();
	private int depth = 2;

	private SqlTreePrinter() {
	}

	private void visitStatement(Statement sqlAstStatement) {
		if ( sqlAstStatement instanceof SelectStatement selectStatement ) {
			logNode(
					"SelectStatement",
					() -> visitQueryPart( selectStatement.getQueryPart() )
			);
		}
		else if ( sqlAstStatement instanceof DeleteStatement deleteStatement ) {
			logNode(
					"DeleteStatement",
					() -> logWithIndentation(
							"target : " + deleteStatement.getTargetTable().getTableExpression()
					)
			);
		}
		else if ( sqlAstStatement instanceof UpdateStatement updateStatement ) {
			logNode(
					"UpdateStatement",
					() -> logWithIndentation(
							"target : " + updateStatement.getTargetTable().getTableExpression()
					)
			);
		}
		else if ( sqlAstStatement instanceof InsertSelectStatement insertStatement ) {
			logNode(
					"InsertStatement",
					() -> logWithIndentation(
							"target : " + insertStatement.getTargetTable().getTableExpression()
					)
			);
		}
		else {
			throw new UnsupportedOperationException( "Printing for this type of SQL AST not supported : " + sqlAstStatement );
		}
	}

	private void visitQueryPart(QueryPart queryPart) {
		if ( queryPart instanceof QueryGroup queryGroup ) {
			visitQueryGroup( queryGroup );
		}
		else if ( queryPart instanceof QuerySpec querySpec ) {
			visitQuerySpec( querySpec );
		}
		else {
			throw new IllegalArgumentException( "Unexpected query part" );
		}
	}

	private void visitQueryGroup(QueryGroup queryGroup) {
		logNode(
				"QueryGroup: " + queryGroup.getSetOperator(),
				() -> {
					for ( QueryPart queryPart : queryGroup.getQueryParts() ) {
						visitQueryPart( queryPart );
					}
				}
		);
	}

	private void visitQuerySpec(QuerySpec querySpec) {
		visitFromClause( querySpec.getFromClause() );
	}

	private void visitFromClause(FromClause fromClause) {
		logNode(
				"FromClause",
				() -> fromClause.visitRoots( this::visitTableGroup )
		);
	}

	private void visitTableGroup(TableGroup tableGroup) {
		logNode(
				toDisplayText( tableGroup ),
				() -> logTableGroupDetails( tableGroup )
		);
	}

	private String toDisplayText(TableGroup tableGroup) {
		return tableGroup.getClass().getSimpleName() + " ("
					+ tableGroup.getGroupAlias() + " : "
					+ tableGroup.getNavigablePath()
					+ ")";
	}

	private void logTableGroupDetails(TableGroup tableGroup) {
		if ( !tableGroup.isInitialized() ) {
			return;
		}
		if ( tableGroup.getPrimaryTableReference() instanceof NamedTableReference ) {
			logWithIndentation(
					"primaryTableReference : %s as %s",
					tableGroup.getPrimaryTableReference().getTableId(),
					tableGroup.getPrimaryTableReference().getIdentificationVariable()
			);
		}
		else {
			if ( tableGroup.getPrimaryTableReference() instanceof ValuesTableReference ) {
				logWithIndentation(
						"primaryTableReference : values (..) as %s",
						tableGroup.getPrimaryTableReference().getIdentificationVariable()
				);
			}
			else if ( tableGroup.getPrimaryTableReference() instanceof FunctionTableReference ) {
				logWithIndentation(
						"primaryTableReference : %s(...) as %s",
						( (FunctionTableReference) tableGroup.getPrimaryTableReference() ).getFunctionExpression().getFunctionName(),
						tableGroup.getPrimaryTableReference().getIdentificationVariable()
				);
			}
			else {
				logNode(
						"PrimaryTableReference as " + tableGroup.getPrimaryTableReference().getIdentificationVariable(),
						() -> {
							Statement statement = ( (QueryPartTableReference) tableGroup.getPrimaryTableReference() ).getStatement();
							visitStatement( statement );
						}
				);
			}
		}

		final List<TableReferenceJoin> tableReferenceJoins = tableGroup.getTableReferenceJoins();
		if ( ! tableReferenceJoins.isEmpty() ) {
			logNode(
					"TableReferenceJoins",
					() -> {
						for ( TableReferenceJoin join : tableReferenceJoins ) {
							logWithIndentation(
									"%s join %s as %s",
									join.getJoinType().getText(),
									join.getJoinedTableReference().getTableExpression(),
									join.getJoinedTableReference().getIdentificationVariable()
							);
						}
					}
			);
		}

		final List<TableGroupJoin> nestedTableGroupJoins = tableGroup.getNestedTableGroupJoins();
		if ( ! nestedTableGroupJoins.isEmpty() ) {
			logNode(
					"NestedTableGroupJoins",
					() -> tableGroup.visitNestedTableGroupJoins( this::visitTableGroupJoin )
			);
		}

		final List<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
		if ( ! tableGroupJoins.isEmpty() ) {
			logNode(
					"TableGroupJoins",
					() -> tableGroup.visitTableGroupJoins( this::visitTableGroupJoin )
			);
		}
	}

	private void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
		logNode(
				tableGroupJoin.getJoinType().getText() + " join " + toDisplayText( tableGroupJoin.getJoinedGroup() ),
				() -> logTableGroupDetails( tableGroupJoin.getJoinedGroup() )
		);
	}

	private void logNode(String text) {
		logWithIndentation( "%s", text );
	}

	private void logNode(String text, Runnable subTreeHandler) {
		logNode( text, subTreeHandler, false );
	}

	private void logNode(String text, Runnable subTreeHandler, boolean indentContinuation) {
		logWithIndentation( "%s {", text );
		depth++;

		try {
			if ( indentContinuation ) {
				depth++;
			}
			subTreeHandler.run();
		}
		catch (Exception e) {
			SqlAstTreeLogger.INSTANCE.tracef( e, "Error processing node {%s}", text );
		}
		finally {
			if ( indentContinuation ) {
				depth--;
			}
		}

		depth--;
		logWithIndentation( "}", text );
	}

	private void logWithIndentation(Object line) {
		pad( depth );
		buffer.append( line ).append( System.lineSeparator() );
	}

	private void logWithIndentation(String pattern, Object arg1) {
		logWithIndentation( String.format( pattern, arg1 ) );
	}

	private void logWithIndentation(String pattern, Object arg1, Object arg2) {
		logWithIndentation( String.format( pattern, arg1, arg2 ) );
	}

	private void logWithIndentation(String pattern, Object arg1, Object arg2, Object arg3) {
		logWithIndentation( String.format( pattern, arg1, arg2, arg3 ) );
	}

	private void pad(int depth) {
		for ( int i = 0; i < depth; i++ ) {
			buffer.append( "  " );
		}
	}

}
