/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.ast;

import java.sql.Types;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.orm.test.mapping.SmokeTests.Gender;
import org.hibernate.orm.test.mapping.SmokeTests.SimpleEntity;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.hql.spi.HqlQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.internal.StandardSqmTranslator;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslator;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.internal.BasicTypeImpl;

import org.hibernate.testing.hamcrest.AssignableMatcher;
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
@SuppressWarnings("WeakerAccess")
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
		scope.inTransaction(
				session -> {
					final QueryImplementor<String> query = session.createQuery(
							"select e.name from SimpleEntity e",
							String.class
					);
					final HqlQueryImplementor<String> hqlQuery = (HqlQueryImplementor<String>) query;
					final SqmSelectStatement<String> sqmStatement = (SqmSelectStatement<String>) hqlQuery.getSqmStatement();

					final StandardSqmTranslator<SelectStatement> sqmConverter = new StandardSqmTranslator<>(
							sqmStatement,
							hqlQuery.getQueryOptions(),
							( (QuerySqmImpl<?>) hqlQuery ).getDomainParameterXref(),
							query.getParameterBindings(),
							session.getLoadQueryInfluencers(),
							scope.getSessionFactory()
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


					// `s` is the "alias stem" for `SimpleEntity` and as it is the first entity with that stem in
					// the query the base becomes `s1`.  The primary table reference is always suffixed as `_0`
					assertThat( rootTableGroup.getPrimaryTableReference().getIdentificationVariable(), is( "s1_0" ) );

					final SelectClause selectClause = sqlAst.getQuerySpec().getSelectClause();
					assertThat( selectClause.getSqlSelections().size(), is( 1 ) ) ;
					final SqlSelection sqlSelection = selectClause.getSqlSelections().get( 0 );
					assertThat( sqlSelection.getJdbcResultSetIndex(), is( 1 ) );
					assertThat( sqlSelection.getValuesArrayPosition(), is( 0 ) );
					assertThat( sqlSelection.getJdbcValueExtractor(), notNullValue() );

					final JdbcSelect jdbcSelectOperation = new StandardSqlAstTranslator<JdbcSelect>(
							session.getSessionFactory(),
							sqlAst
					).translate( null, QueryOptions.NONE );

					assertThat(
							jdbcSelectOperation.getSql(),
							is( "select s1_0.name from mapping_simple_entity s1_0" )
					);
				}
		);
	}

	@Test
	public void testConvertedHqlInterpretation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final JdbcTypeRegistry jdbcTypeRegistry = session.getFactory()
							.getTypeConfiguration()
							.getJdbcTypeDescriptorRegistry();
					final QueryImplementor<Gender> query = session.createQuery( "select e.gender from SimpleEntity e", Gender.class );
					final HqlQueryImplementor<Gender> hqlQuery = (HqlQueryImplementor<Gender>) query;
					final SqmSelectStatement<Gender> sqmStatement = (SqmSelectStatement<Gender>) hqlQuery.getSqmStatement();

					final StandardSqmTranslator<SelectStatement> sqmConverter = new StandardSqmTranslator<>(
							sqmStatement,
							hqlQuery.getQueryOptions(),
							( (QuerySqmImpl<?>) hqlQuery ).getDomainParameterXref(),
							query.getParameterBindings(),
							session.getLoadQueryInfluencers(),
							scope.getSessionFactory()
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


					// `s` is the "alias stem" for `SimpleEntity` and as it is the first entity with that stem in
					// the query the base becomes `s1`.  The primary table reference is always suffixed as `_0`
					assertThat( rootTableGroup.getPrimaryTableReference().getIdentificationVariable(), is( "s1_0" ) );

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
					assertThat( columnReference.renderSqlFragment( scope.getSessionFactory() ), is( "s1_0.gender" ) );

					final JdbcMappingContainer selectedExpressable = selectedExpression.getExpressionType();
					assertThat( selectedExpressable, instanceOf( BasicTypeImpl.class ) );
					final BasicTypeImpl basicType = (BasicTypeImpl) selectedExpressable;
					assertThat( basicType.getJavaTypeDescriptor().getJavaTypeClass(), AssignableMatcher.assignableTo( Integer.class ) );
					assertThat(
							basicType.getJdbcTypeDescriptor(),
							is( jdbcTypeRegistry.getDescriptor( Types.TINYINT ) )
					);


					assertThat( sqlAst.getDomainResultDescriptors().size(), is( 1 ) );
					final DomainResult domainResult = sqlAst.getDomainResultDescriptors().get( 0 );
					assertThat( domainResult, instanceOf( BasicResult.class ) );
					final BasicResult scalarDomainResult = (BasicResult) domainResult;
					assertThat( scalarDomainResult.getAssembler(), instanceOf( BasicResultAssembler.class ) );
					final BasicResultAssembler<?> assembler = (BasicResultAssembler) scalarDomainResult.getAssembler();
					assertThat( assembler.getValueConverter(), notNullValue() );
					assertThat( assembler.getValueConverter(), instanceOf( OrdinalEnumValueConverter.class ) );

					final NavigablePath expectedSelectedPath = new NavigablePath(
							SimpleEntity.class.getName(),
							"e"
					).append( "gender" );
					assertThat( domainResult.getNavigablePath(), equalTo( expectedSelectedPath ) );
					assertThat( domainResult, instanceOf( BasicResult.class ) );

					// ScalarDomainResultImpl creates and caches the assembler at its creation.
					// this just gets access to that cached one
					final DomainResultAssembler resultAssembler = domainResult.createResultAssembler( null, null );

					assertThat( resultAssembler, instanceOf( BasicResultAssembler.class ) );
					final BasicValueConverter valueConverter = ( (BasicResultAssembler) resultAssembler ).getValueConverter();
					assertThat( valueConverter, notNullValue() );
					assertThat( valueConverter, instanceOf( OrdinalEnumValueConverter.class ) );

					final JdbcSelect jdbcSelectOperation = new StandardSqlAstTranslator<JdbcSelect>(
							session.getSessionFactory(),
							sqlAst
					).translate( null, QueryOptions.NONE );

					assertThat(
							jdbcSelectOperation.getSql(),
							is( "select s1_0.gender from mapping_simple_entity s1_0" )
					);
				}
		);
	}

	@Test
	public void testBadQueryResultType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<SimpleEntity> query = session.createQuery( "select e from SimpleEntity e", SimpleEntity.class );
					query.list();
				}
		);
	}
}
