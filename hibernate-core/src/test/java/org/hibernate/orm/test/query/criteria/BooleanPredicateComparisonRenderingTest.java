/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.BooleanExpression;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.sql.internal.StandardSqmTranslator;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractBooleanPredicateComparisonRenderingTest {

	@Test
	void predicateComparedToBooleanLiteral(SessionFactoryScope scope) {
		final String sql = scope.fromSession( this::renderSql );
		verifySql( sql );
	}

	protected abstract void verifySql(String sql);

	private String renderSql(SessionImplementor session) {
		final var builder = session.getCriteriaBuilder();
		final var query = builder.createQuery( Article.class );
		final var article = query.from( Article.class );
		final var published = (BooleanExpression) (Object) article.get( "published" );

		query.where( published.not().equalTo( builder.booleanLiteral( false ) ) );

		final var hibernateQuery = (SelectionQueryImpl<Article>) session.createQuery( query );
		final var sqmConverter = new StandardSqmTranslator<SelectStatement>(
				(SqmSelectStatement<?>) hibernateQuery.getSqmStatement(),
				hibernateQuery.getQueryOptions(),
				hibernateQuery.getDomainParameterXref(),
				hibernateQuery.getParameterBindings(),
				session.getLoadQueryInfluencers(),
				session.getFactory().getSqlTranslationEngine(),
				true
		);
		final var sqlAst = sqmConverter.translate().getSqlAst();
		return session.getFactory().getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildSelectTranslator( session.getFactory(), sqlAst )
				.translate( null, QueryOptions.NONE )
				.getSqlString();
	}

	@Entity(name = "BooleanPredicateComparisonArticle")
	static class Article {
		@Id
		Long id;

		boolean published;
	}
}

@DomainModel(annotatedClasses = AbstractBooleanPredicateComparisonRenderingTest.Article.class)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.DIALECT,
				value = "org.hibernate.orm.test.query.criteria.BooleanPredicateComparisonSqlServerRenderingTest$SQLServer16Dialect"),
		@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "none")
})
@SessionFactory
class BooleanPredicateComparisonSqlServerRenderingTest extends AbstractBooleanPredicateComparisonRenderingTest {

	@Override
	protected void verifySql(String sql) {
		assertThat( sql )
				.contains( "published=1" )
				.doesNotContain( "published=0)=0" )
				.doesNotContain( "published=0=0" );
	}

	public static class SQLServer16Dialect extends SQLServerDialect {
		public SQLServer16Dialect() {
			super( DatabaseVersion.make( 16 ) );
		}
	}
}

@DomainModel(annotatedClasses = AbstractBooleanPredicateComparisonRenderingTest.Article.class)
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.DIALECT,
				value = "org.hibernate.orm.test.query.criteria.BooleanPredicateComparisonOracleRenderingTest$Oracle23Dialect"),
		@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "none")
})
@SessionFactory
class BooleanPredicateComparisonOracleRenderingTest extends AbstractBooleanPredicateComparisonRenderingTest {

	@Override
	protected void verifySql(String sql) {
		assertThat( sql )
				.contains( "published=true" )
				.doesNotContain( "published=false)=false" )
				.doesNotContain( "published=false=false" );
	}

	public static class Oracle23Dialect extends OracleDialect {
		public Oracle23Dialect() {
			super( DatabaseVersion.make( 23 ) );
		}
	}
}
