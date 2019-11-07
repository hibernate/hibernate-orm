/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.ast;

import java.sql.Types;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.orm.test.metamodel.mapping.SmokeTests.Gender;
import org.hibernate.orm.test.metamodel.mapping.SmokeTests.SimpleEntity;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.hql.spi.HqlQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.sql.internal.SqmSelectInterpretation;
import org.hibernate.query.sqm.sql.internal.StandardSqmSelectToSqlAstConverter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.spi.StandardSqlAstSelectTranslator;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResult;
import org.hibernate.sql.results.internal.domain.basic.BasicResultAssembler;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;

import org.hibernate.testing.hamcrest.AssignableMatcher;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@DomainModel(
		annotatedClasses = SimpleEntity.class
)
@ServiceRegistry(
		settings = @ServiceRegistry.Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@SessionFactory
@Tags({
	@Tag("RunnableIdeTest"),
})
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
					//noinspection unchecked
					final SqmSelectStatement<String> sqmStatement = (SqmSelectStatement<String>) hqlQuery.getSqmStatement();

					final StandardSqmSelectToSqlAstConverter sqmConverter = new StandardSqmSelectToSqlAstConverter(
							hqlQuery.getQueryOptions(),
							( (QuerySqmImpl) hqlQuery ).getDomainParameterXref(),
							query.getParameterBindings(),
							session.getLoadQueryInfluencers(),
							scope.getSessionFactory()
					);

					final SqmSelectInterpretation sqmInterpretation = sqmConverter.interpret( sqmStatement );
					final SelectStatement sqlAst = sqmInterpretation.getSqlAst();

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots().size(), is( 1 ) );

					final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
					assertThat( rootTableGroup.getPrimaryTableReference(), notNullValue() );
					assertThat( rootTableGroup.getPrimaryTableReference().getTableExpression(), is( "mapping_simple_entity" ) );

					assertThat( rootTableGroup.getTableReferenceJoins().size(), is( 0 ) );

					assertThat( rootTableGroup.hasTableGroupJoins(), is( false ) );


					// `s` is the "alias stem" for `SimpleEntity` and as it is the first entity with that stem in
					// the query the base becomes `s1`.  The primary table reference is always suffixed as `_0`
					assertThat( rootTableGroup.getPrimaryTableReference().getIdentificationVariable(), is( "s1_0" ) );

					final SelectClause selectClause = sqlAst.getQuerySpec().getSelectClause();
					assertThat( selectClause.getSqlSelections().size(), is( 1 ) ) ;
					final SqlSelection sqlSelection = selectClause.getSqlSelections().get( 0 );
					assertThat( sqlSelection.getJdbcResultSetIndex(), is( 1 ) );
					assertThat( sqlSelection.getValuesArrayPosition(), is( 0 ) );
					assertThat( sqlSelection.getJdbcValueExtractor(), notNullValue() );

					final JdbcSelect jdbcSelectOperation = new StandardSqlAstSelectTranslator( session.getSessionFactory() )
							.interpret( sqlAst );

					assertThat(
							jdbcSelectOperation.getSql(),
							is( "select s1_0.name from mapping_simple_entity as s1_0" )
					);
				}
		);
	}

	@Test
	public void testConvertedHqlInterpretation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<Gender> query = session.createQuery( "select e.gender from SimpleEntity e", Gender.class );
					final HqlQueryImplementor<Gender> hqlQuery = (HqlQueryImplementor<Gender>) query;
					//noinspection unchecked
					final SqmSelectStatement<Gender> sqmStatement = (SqmSelectStatement<Gender>) hqlQuery.getSqmStatement();

					final StandardSqmSelectToSqlAstConverter sqmConverter = new StandardSqmSelectToSqlAstConverter(
							hqlQuery.getQueryOptions(),
							( (QuerySqmImpl) hqlQuery ).getDomainParameterXref(),
							query.getParameterBindings(),
							session.getLoadQueryInfluencers(),
							scope.getSessionFactory()
					);

					final SqmSelectInterpretation sqmInterpretation = sqmConverter.interpret( sqmStatement );
					final SelectStatement sqlAst = sqmInterpretation.getSqlAst();

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots().size(), is( 1 ) );

					final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
					assertThat( rootTableGroup.getPrimaryTableReference(), notNullValue() );
					assertThat( rootTableGroup.getPrimaryTableReference().getTableExpression(), is( "mapping_simple_entity" ) );

					assertThat( rootTableGroup.getTableReferenceJoins().size(), is( 0 ) );

					assertThat( rootTableGroup.hasTableGroupJoins(), is( false ) );


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
					final Expression selectedExpression = ( (SqlSelectionImpl) sqlSelection ).getWrappedSqlExpression();
					assertThat( selectedExpression, instanceOf( ColumnReference.class ) );
					final ColumnReference columnReference = (ColumnReference) selectedExpression;
					assertThat( columnReference.renderSqlFragment( scope.getSessionFactory() ), is( "s1_0.gender" ) );

					final MappingModelExpressable selectedExpressable = selectedExpression.getExpressionType();
					//assertThat( selectedExpressable, instanceOf( StandardBasicTypeImpl.class ) );
//					assertThat( basicType.getJavaTypeDescriptor().getJavaType(), AssignableMatcher.assignableTo( Integer.class ) );
//					assertThat( basicType.getSqlTypeDescriptor().getSqlType(), is( Types.INTEGER ) );
					assertThat( selectedExpressable, instanceOf( CustomType.class ) );
					final CustomType basicType = (CustomType) selectedExpressable;
					final EnumType enumType = (EnumType) basicType.getUserType();
					assertThat( enumType.getEnumValueConverter().getRelationalJavaDescriptor().getJavaType(), AssignableMatcher.assignableTo( Integer.class ) );
					assertThat( enumType.sqlTypes()[0], is( Types.INTEGER ) );


					assertThat( sqlAst.getDomainResultDescriptors().size(), is( 1 ) );
					final DomainResult domainResult = sqlAst.getDomainResultDescriptors().get( 0 );
					assertThat( domainResult, instanceOf( BasicResult.class ) );
					final BasicResult scalarDomainResult = (BasicResult) domainResult;
					assertThat( scalarDomainResult.getAssembler(), instanceOf( BasicResultAssembler.class ) );
					final BasicResultAssembler<?> assembler = (BasicResultAssembler) scalarDomainResult.getAssembler();
//					assertThat( assembler.getValueConverter(), notNullValue() );
//					assertThat( assembler.getValueConverter(), instanceOf( OrdinalEnumValueConverter.class ) );
					assertThat( assembler.getValueConverter(), nullValue() );

					final NavigablePath expectedSelectedPath = new NavigablePath(
							org.hibernate.orm.test.metamodel.mapping.SmokeTests.SimpleEntity.class.getName(),
							"e"
					).append( "gender" );
					assertThat( domainResult.getNavigablePath(), equalTo( expectedSelectedPath ) );
					assertThat( domainResult, instanceOf( BasicResult.class ) );

					// ScalarDomainResultImpl creates and caches the assembler at its creation.
					// this just gets access to that cached one
					final DomainResultAssembler resultAssembler = domainResult.createResultAssembler(
							null,
							null
					);

					assertThat( resultAssembler, instanceOf( BasicResultAssembler.class ) );
//					final BasicValueConverter valueConverter = ( (BasicResultAssembler) resultAssembler ).getValueConverter();
//					assertThat( valueConverter, notNullValue() );
//					assertThat( valueConverter, instanceOf( OrdinalEnumValueConverter.class ) );
					assertThat( ( (BasicResultAssembler) resultAssembler ).getValueConverter(), nullValue() );

					final JdbcSelect jdbcSelectOperation = new StandardSqlAstSelectTranslator( session.getSessionFactory() )
							.interpret( sqlAst );

					assertThat(
							jdbcSelectOperation.getSql(),
							is( "select s1_0.gender from mapping_simple_entity as s1_0" )
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
