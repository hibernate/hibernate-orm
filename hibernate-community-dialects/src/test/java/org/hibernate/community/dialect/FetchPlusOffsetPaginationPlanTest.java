/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.internal.H2SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.PaginationRenderingSupport;
import org.hibernate.dialect.sql.ast.spi.StandardPaginationRenderingSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the SQL rendering and JDBC row-discard semantics of the
/// `FetchPlusOffset` pagination plan.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = FetchPlusOffsetPaginationPlanTest.Book.class)
@SessionFactory
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.DIALECT,
				provider = FetchPlusOffsetPaginationPlanTest.TestSettingProvider.class
		)
)
public class FetchPlusOffsetPaginationPlanTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 1; i <= 3; i++ ) {
				session.persist( new Book( i, "Book " + i ) );
			}
		} );
	}

	@Test
	void discardsOffsetRowsAfterFetchingOffsetPlusLimit(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Book> books = session.createSelectionQuery(
						"from Book b order by b.id",
						Book.class
				)
					.setFirstResult( 2 )
					.setMaxResults( 1 )
					.getResultList();

			assertThat( books ).extracting( Book::getId ).containsExactly( 3 );
		} );
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

		public Integer getId() {
			return id;
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
				protected <S extends Statement, O extends JdbcOperation> SqlAstTranslator<O> createTranslator(
						SqlAstTranslationRequest<S, O> request) {
					return new H2SqlAstTranslator<>( request ) {
						@Override
						protected PaginationRenderingSupport getPaginationRenderingSupport() {
							return StandardPaginationRenderingSupport.FETCH_PLUS_OFFSET;
						}
					};
				}
			};
		}
	}
}
