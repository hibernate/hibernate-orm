/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.ast;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.orm.test.mapping.SmokeTests.Gender;
import org.hibernate.orm.test.mapping.SmokeTests.SimpleEntity;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.internal.SqmQueryImpl;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.internal.StandardSqmTranslator;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslator;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = SimpleEntity.class
)
@ServiceRegistry(
		settings = @Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@SessionFactory
public class SmokeTests {
	@Test
	public void testSimpleHqlInterpretation(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectStatement sqlAst = SqlAstHelper.translateHqlSelectQuery(
					"select e.name from SimpleEntity e",
					String.class,
					session
			);

			final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
			assertThat( fromClause.getRoots().size(), is( 1 ) );

			final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
			assertThat( rootTableGroup.getPrimaryTableReference(), notNullValue() );
			assertThat( rootTableGroup.getPrimaryTableReference().getTableId(), is( "mapping_simple_entity" ) );

			assertThat( rootTableGroup.getTableReferenceJoins().size(), is( 0 ) );

			assertThat( rootTableGroup.getTableGroupJoins().isEmpty(), is( true ) );


			// `se` is the "alias stem" for `SimpleEntity` and as it is the first entity with that stem in
			// the query the base becomes `se1`.  The primary table reference is always suffixed as `_0`
			assertThat( rootTableGroup.getPrimaryTableReference().getIdentificationVariable(), is( "se1_0" ) );

			final SelectClause selectClause = sqlAst.getQuerySpec().getSelectClause();
			assertThat( selectClause.getSqlSelections().size(), is( 1 ) ) ;
			final SqlSelection sqlSelection = selectClause.getSqlSelections().get( 0 );
			assertThat( sqlSelection.getJdbcResultSetIndex(), is( 1 ) );
			assertThat( sqlSelection.getValuesArrayPosition(), is( 0 ) );
			assertThat( sqlSelection.getJdbcValueExtractor(), notNullValue() );

			final JdbcOperationQuerySelect jdbcSelectOperation = new StandardSqlAstTranslator<JdbcOperationQuerySelect>(
					session.getSessionFactory(),
					sqlAst
			).translate( null, QueryOptions.NONE );

			assertThat(
					jdbcSelectOperation.getSqlString(),
					is( "select se1_0.name from mapping_simple_entity se1_0" )
			);
		} );
	}

	@Test
	public void testConvertedHqlInterpretation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Gender> query = session.createQuery( "select e.gender from SimpleEntity e", Gender.class );
					final SqmQueryImplementor<Gender> hqlQuery = (SqmQueryImplementor<Gender>) query;
					final SqmSelectStatement<Gender> sqmStatement = (SqmSelectStatement<Gender>) hqlQuery.getSqmStatement();

					final StandardSqmTranslator<SelectStatement> sqmConverter = new StandardSqmTranslator<>(
							sqmStatement,
							hqlQuery.getQueryOptions(),
							( (SqmQueryImpl<?>) hqlQuery ).getDomainParameterXref(),
							hqlQuery.getParameterBindings(),
							session.getLoadQueryInfluencers(),
							scope.getSessionFactory().getSqlTranslationEngine(),
							true
					);

					final SqmTranslation<SelectStatement> sqmInterpretation = sqmConverter.translate();
					final SelectStatement sqlAst = sqmInterpretation.getSqlAst();

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots().size(), is( 1 ) );

					final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
					assertThat( rootTableGroup.getPrimaryTableReference(), notNullValue() );
					assertThat( rootTableGroup.getPrimaryTableReference().getTableId(), is( "mapping_simple_entity" ) );

					assertThat( rootTableGroup.getTableReferenceJoins().size(), is( 0 ) );

					assertThat( rootTableGroup.getTableGroupJoins().isEmpty(), is( true ) );


					// `se` is the "alias stem" for `SimpleEntity` and as it is the first entity with that stem in
					// the query the base becomes `se1`.  The primary table reference is always suffixed as `_0`
					assertThat( rootTableGroup.getPrimaryTableReference().getIdentificationVariable(), is( "se1_0" ) );

					final SelectClause selectClause = sqlAst.getQuerySpec().getSelectClause();
					assertThat( selectClause.getSqlSelections().size(), is( 1 ) );

					final SqlSelection sqlSelection = selectClause.getSqlSelections().get( 0 );
					assertThat( sqlSelection.getJdbcResultSetIndex(), is( 1 ) );
					assertThat( sqlSelection.getValuesArrayPosition(), is( 0 ) );
					assertThat( sqlSelection.getJdbcValueExtractor(), notNullValue() );

					assertThat( sqlSelection, instanceOf( SqlSelectionImpl.class ) );
					final Expression selectedExpression = sqlSelection.getExpression();
					assertThat( selectedExpression, instanceOf( ColumnReference.class ) );
					final ColumnReference columnReference = (ColumnReference) selectedExpression;
					assertThat( columnReference.getExpressionText(), is( "se1_0.gender" ) );

					final JdbcMapping selectedExpressible = selectedExpression.getExpressionType().getSingleJdbcMapping();
					assertThat( selectedExpressible.getJdbcType().isInteger(), is( true ) );

					assertThat( sqlAst.getDomainResultDescriptors().size(), is( 1 ) );
					final DomainResult<?> domainResult = sqlAst.getDomainResultDescriptors().get( 0 );
					assertThat( domainResult, instanceOf( BasicResult.class ) );
					final BasicResult<?> scalarDomainResult = (BasicResult<?>) domainResult;
					assertThat( scalarDomainResult.getAssembler(), instanceOf( BasicResultAssembler.class ) );

					final NavigablePath expectedSelectedPath = new NavigablePath(
							SimpleEntity.class.getName(),
							"e"
					).append( "gender" );
					assertThat( domainResult.getNavigablePath(), equalTo( expectedSelectedPath ) );
					assertThat( domainResult, instanceOf( BasicResult.class ) );

					// ScalarDomainResultImpl creates and caches the assembler at its creation.
					// this just gets access to that cached one
					final DomainResultAssembler<?> resultAssembler = domainResult.createResultAssembler(
							null,
							null
					);

					assertThat( resultAssembler, instanceOf( BasicResultAssembler.class ) );

					final JdbcOperationQuerySelect jdbcSelectOperation = new StandardSqlAstTranslator<JdbcOperationQuerySelect>(
							session.getSessionFactory(),
							sqlAst
					).translate( null, QueryOptions.NONE );

					assertThat(
							jdbcSelectOperation.getSqlString(),
							is( "select se1_0.gender from mapping_simple_entity se1_0" )
					);
				}
		);
	}

	@Test
	public void testBadQueryResultType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<SimpleEntity> query = session.createQuery( "select e from SimpleEntity e", SimpleEntity.class );
					query.list();
				}
		);
	}
}
