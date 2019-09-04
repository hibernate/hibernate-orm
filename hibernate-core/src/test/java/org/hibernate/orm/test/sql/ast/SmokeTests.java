/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.ast;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.hql.spi.HqlQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.sql.internal.SqmSelectInterpretation;
import org.hibernate.query.sqm.sql.internal.SqmSelectToSqlAstConverter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@DomainModel(
		annotatedClasses = org.hibernate.orm.test.metamodel.mapping.SmokeTests.SimpleEntity.class
)
@ServiceRegistry(
		settings = @ServiceRegistry.Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@SessionFactory
public class SmokeTests {
	@Test
	public void testSimpleHql(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final QueryImplementor<String> query = session.createQuery( "select e.name from SimpleEntity e", String.class );
					final HqlQueryImplementor<String> hqlQuery = (HqlQueryImplementor<String>) query;
					//noinspection unchecked
					final SqmSelectStatement<String> sqmStatement = (SqmSelectStatement<String>) hqlQuery.getSqmStatement();

					final SqmSelectToSqlAstConverter sqmConverter = new SqmSelectToSqlAstConverter(
							hqlQuery.getQueryOptions(),
							( (QuerySqmImpl) hqlQuery ).getDomainParameterXref(),
							query.getParameterBindings(),
							session.getLoadQueryInfluencers(),
							scope.getSessionFactory()
					);

					final SqmSelectInterpretation interpretation = sqmConverter.interpret( sqmStatement );
					final SelectStatement sqlAst = interpretation.getSqlAst();

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots().size(), is( 1 ) );
					final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
					assertThat( rootTableGroup.hasTableGroupJoins(), is( false ) );
					assertThat( rootTableGroup.getPrimaryTableReference(), notNullValue() );
					assertThat( rootTableGroup.getPrimaryTableReference().getTableExpression(), is( "mapping_simple_entity" ) );

					// `s` is the "alias stem" for `SimpleEntity` and as it is the first entity with that stem in
					// the query the base becomes `s1`.  The primary table reference is always suffixed as `_0`
					assertThat( rootTableGroup.getPrimaryTableReference().getIdentificationVariable(), is( "s1_0" ) );

					final SelectClause selectClause = sqlAst.getQuerySpec().getSelectClause();
					assertThat( selectClause.getSqlSelections().size(), is( 1 ) ) ;
					final SqlSelection sqlSelection = selectClause.getSqlSelections().get( 0 );
					assertThat( sqlSelection.getJdbcResultSetIndex(), is( 1 ) );
					assertThat( sqlSelection.getValuesArrayPosition(), is( 0 ) );
					assertThat( sqlSelection.getJdbcValueExtractor(), notNullValue() );
				}
		);
	}
}
