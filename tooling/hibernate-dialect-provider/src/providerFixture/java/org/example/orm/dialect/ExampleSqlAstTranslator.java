/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.util.List;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.SPI;
import org.hibernate.dialect.sql.ast.spi.DerivedColumnAliasing;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.DerivedTableRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.LateralReferenceStyle;
import org.hibernate.dialect.sql.ast.spi.InsertConflictAction;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.InsertConflictRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.QueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.ReturningRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.SelectItemReferenceStrategy;
import org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.SetReturningFunctionRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardQueryMutationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.TableJoinRenderingPlan;
import org.hibernate.dialect.sql.ast.spi.TableJoinRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.TableReferenceAliasContext;
import org.hibernate.dialect.sql.ast.spi.ValuesTableRenderingStyle;
import org.hibernate.query.SortDirection;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.dialect.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.from.FromClause;
import org.hibernate.sql.ast.spi.query.insert.InsertSelectStatement;
import org.hibernate.sql.ast.spi.query.insert.Values;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.ast.spi.query.update.Assignment;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.exec.spi.JdbcOperation;

/// External SQL AST translator which consumes representative query and
/// mutation nodes through provider-facing override points.
///
/// @author Steve Ebersole
public class ExampleSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {
	public ExampleSqlAstTranslator(SqlAstTranslationRequest<? extends Statement, T> request) {
		super( request );
	}

	public EntityVersionMapping versionSeedMapping(Expression expression) {
		return getVersionSeedMapping( expression );
	}

	public boolean recognizesParameterInterpretation(Expression expression) {
		return isParameterInterpretation( expression );
	}

	@Override
	@SPI({ SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
	protected SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.ALIAS;
	}

	@Override
	protected void renderInsertIntoNoColumns(TableInsertStandard tableInsert) {
		renderIntoIntoAndTable( tableInsert );
		appendSql( "default values" );
	}

	@Override
	public void visitOffsetFetchClause(QueryPart queryPart) {
		super.visitOffsetFetchClause( queryPart );
	}

	@Override
	protected void renderSelectStatement(SelectStatement statement) {
		super.renderSelectStatement( statement );
	}

	@Override
	protected void renderSelectClause(SelectClause selectClause) {
		super.renderSelectClause( selectClause );
	}

	@Override
	protected void renderSelectItems(SelectClause selectClause) {
		super.renderSelectItems( selectClause );
	}

	@Override
	protected PaginationRenderingSupport getPaginationRenderingSupport() {
		return request -> {
			if ( request.isRowNumberingCurrentQueryPart()
					|| !request.hasOffset() && !request.hasFetch() ) {
				return new PaginationRenderingPlan.None();
			}
			return new PaginationRenderingPlan.OffsetFetch( true );
		};
	}

	@Override
	protected void renderPartitionItem(Expression expression) {
		super.renderPartitionItem( expression );
	}

	@Override
	protected void visitOrderBy(List<SortSpecification> sortSpecifications) {
		super.visitOrderBy( sortSpecifications );
	}

	@Override
	protected void renderEmptyOrderBy() {
		super.renderEmptyOrderBy();
	}

	@Override
	protected void visitSortSpecification(
			Expression sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence,
			boolean ignoreCase) {
		super.visitSortSpecification( sortExpression, sortOrder, nullPrecedence, ignoreCase );
	}

	@Override
	protected void renderSortExpression(Expression sortExpression, boolean ignoreCase) {
		super.renderSortExpression( sortExpression, ignoreCase );
	}

	@Override
	protected String getSingleRowTableExpression() {
		return super.getSingleRowTableExpression();
	}

	@Override
	protected String getSelectOnlyFromClause() {
		return super.getSelectOnlyFromClause();
	}

	@Override
	protected void emulateSortSpecificationNullPrecedence(Expression sortExpression, Nulls nullPrecedence) {
		super.emulateSortSpecificationNullPrecedence( sortExpression, nullPrecedence );
	}

	@Override
	protected QueryMutationRenderingSupport getQueryMutationRenderingSupport() {
		return StandardQueryMutationRenderingSupport.MERGE;
	}

	@Override
	protected ReturningRenderingSupport getReturningRenderingSupport() {
		return request -> request.returningColumns().isEmpty()
				? new ReturningRenderingPlan.None()
				: new ReturningRenderingPlan.Clause();
	}

	@Override
	protected InsertConflictRenderingSupport getInsertConflictRenderingSupport() {
		return request -> request.action() == InsertConflictAction.NONE
				? new InsertConflictRenderingPlan.None()
				: new InsertConflictRenderingPlan.Standard();
	}

	@Override
	protected DerivedTableRenderingSupport getDerivedTableRenderingSupport() {
		return request -> switch ( request.kind() ) {
			case QUERY_PART, INLINE_CTE -> new DerivedTableRenderingPlan.QueryPart(
					DerivedColumnAliasing.SELECT_LIST,
					request.lateral() && request.supportsLateralKeyword()
							? LateralReferenceStyle.KEYWORD
							: request.lateral()
									? LateralReferenceStyle.EMULATED_QUERY_PART
									: LateralReferenceStyle.IMPLICIT,
					false,
					null
			);
			case VALUES -> new DerivedTableRenderingPlan.Values(
					DerivedColumnAliasing.SELECT_LIST,
					ValuesTableRenderingStyle.SELECT_UNION,
					null
			);
			case FUNCTION -> new DerivedTableRenderingPlan.Function(
					DerivedColumnAliasing.IDENTIFICATION_VARIABLE_ONLY,
					LateralReferenceStyle.IMPLICIT,
					null
			);
		};
	}

	@Override
	protected SetReturningFunctionRenderingSupport getSetReturningFunctionRenderingSupport() {
		return request -> request.ordinalityRequested()
				? new SetReturningFunctionRenderingPlan.DerivedOrdinality(
						SetReturningFunctionRenderingPlan.DerivedOrdinality.InvocationWrapper.NONE,
						SetReturningFunctionRenderingPlan.DerivedOrdinality.OrdinalityExpression.ROW_NUMBER,
						false
				)
				: new SetReturningFunctionRenderingPlan.Native(
						SetReturningFunctionRenderingPlan.Native.Ordinality.NONE
				);
	}

	@Override
	protected void renderTableReferenceAlias(String alias, TableReferenceAliasContext context) {
		super.renderTableReferenceAlias( alias, context );
	}

	@Override
	protected TableJoinRenderingSupport getTableJoinRenderingSupport() {
		return request -> new TableJoinRenderingPlan.Standard();
	}

	@Override
	protected void renderDeleteClause(DeleteStatement statement) {
		super.renderDeleteClause( statement );
	}

	@Override
	protected void renderUpdateClause(UpdateStatement statement) {
		super.renderUpdateClause( statement );
	}

	@Override
	protected void renderSetAssignment(Assignment assignment) {
		super.renderSetAssignment( assignment );
	}

	@Override
	protected void renderAssignmentColumn(ColumnReference column) {
		super.renderAssignmentColumn( column );
	}

	private void renderStandardMutationPlans(
			DeleteStatement deleteStatement,
			UpdateStatement updateStatement,
			InsertSelectStatement insertStatement,
			List<Values> valuesList) {
		renderDeleteStatementDirect( deleteStatement );
		renderDeleteStatementWithJoinEmulation( deleteStatement, null );
		renderUpdateStatementDirect( updateStatement );
		renderUpdateStatementAsScalarSubquery( updateStatement );
		renderUpdateStatementAsMerge( updateStatement );
		renderUpdateStatementAsInlineView( updateStatement );
		renderUpdateStatementAsTupleSet( updateStatement );
		renderInsertStatementAsMerge( insertStatement );
		renderValuesListAsSelectUnion( valuesList );
	}

	private boolean renderFromClauseSpacesIfPresent(FromClause fromClause) {
		if ( hasFrom( fromClause ) ) {
			renderFromClauseSpaces( fromClause );
			return true;
		}
		return false;
	}
}
