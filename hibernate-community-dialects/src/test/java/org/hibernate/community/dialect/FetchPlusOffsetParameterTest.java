/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.H2SqlAstTranslator;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.exec.spi.JdbcOperation;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = FetchPlusOffsetParameterTest.Book.class)
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(settingName = AvailableSettings.DIALECT, provider = FetchPlusOffsetParameterTest.TestSettingProvider.class)
)
@Jira("https://hibernate.atlassian.net/browse/HHH-19888")
public class FetchPlusOffsetParameterTest {

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					for ( int i = 1; i <= 3; i++ ) {
						session.persist( new Book( i, "Book " + i ) );
					}
				}
		);
	}

	@Test
	public void testStaticOffset(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List<Book> books = session.createSelectionQuery(
									"from Book b order by b.id",
									Book.class
							)
							.setFirstResult( 2 )
							.setMaxResults( 1 ).getResultList();
					// The custom dialect will fetch offset + limit + staticOffset rows
					// Since staticOffset is -1, it must yield 2 rows
					assertEquals( 2, books.size() );
				}
		);
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Integer id;
		private String title;

		public Book() {
		}

		public Book(Integer id, String title) {
			this.id = id;
			this.title = title;
		}
	}


	public static class TestSettingProvider implements SettingProvider.Provider<String> {

		@Override
		public String getSetting() {
			return TestDialect.class.getName();
		}
	}

	public static class TestDialect extends H2Dialect {

		public TestDialect(DialectResolutionInfo info) {
			super( info );
		}

		public TestDialect() {
		}

		@Override
		public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
			return new StandardSqlAstTranslatorFactory() {
				@Override
				protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
						SessionFactoryImplementor sessionFactory, Statement statement) {
					return new H2SqlAstTranslator<>( sessionFactory, statement ) {
						@Override
						public void visitOffsetFetchClause(QueryPart queryPart) {
							final Expression offsetClauseExpression;
							final Expression fetchClauseExpression;
							if ( queryPart.isRoot() && hasLimit() ) {
								prepareLimitOffsetParameters();
								offsetClauseExpression = getOffsetParameter();
								fetchClauseExpression = getLimitParameter();
							}
							else {
								assert queryPart.getFetchClauseType() == FetchClauseType.ROWS_ONLY;
								offsetClauseExpression = queryPart.getOffsetClauseExpression();
								fetchClauseExpression = queryPart.getFetchClauseExpression();
							}
							if ( offsetClauseExpression != null && fetchClauseExpression != null ) {
								appendSql( " fetch first " );
								getClauseStack().push( Clause.FETCH );
								try {
									renderFetchPlusOffsetExpressionAsSingleParameter(
											fetchClauseExpression,
											offsetClauseExpression,
											-1
									);
								}
								finally {
									getClauseStack().pop();
								}
								appendSql( " rows only" );
							}
						}
					};
				}
			};
		}
	}
}


