/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.hibernate.testing.util.ast.HqlHelper;
import org.hibernate.testing.util.ast.LoadingAstHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-19925")
@DomainModel(annotatedClasses = {
		LockingBasedOnSelectClauseTests.Book.class,
		LockingBasedOnSelectClauseTests.Author.class
})
@SessionFactory
public class LockingBasedOnSelectClauseTests {
	@BeforeAll
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var king = new Author( 1, "Stephen King" );
			var darkTower = new Book( 1, "The Dark Tower", king );
			session.persist( king );
			session.persist( darkTower );
		} );
	}

	@AfterAll
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "See https://sourceforge.net/p/hsqldb/bugs/1734/")
	void testBasicHqlUsage(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "select b.author from Book b" )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.list();
			// The correct outcome here is for the authors table to be locked as the root selection.
			TransactionUtil.assertRowLock(
					factoryScope,
					"authors",
					"name",
					"id",
					1,
					true
			);
		} );
	}

	@Test
	void testSubQueryHqlTranslation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Book b where b.id in (select id from Book)" ).list();
		} );
	}

	private static final String BOOK_PATH = "org.hibernate.orm.test.locking.LockingBasedOnSelectClauseTests$Book";
	private static final String BOOK_PATH_HQL = BOOK_PATH+ "(b)";
	private static final String BOOK_AUTHOR_PATH_HQL = BOOK_PATH_HQL + ".author";

	@Test
	void testBasicHqlTranslation(SessionFactoryScope factoryScope) {
		final HqlHelper.HqlTranslation hqlTranslation = HqlHelper.translateHql(
				"select b.author from Book b",
				factoryScope.getSessionFactory()
		);

		final Statement sqlAst = hqlTranslation.sqlAst();
		assertThat( sqlAst ).isInstanceOf( SelectStatement.class );
		final SelectStatement selectAst = ( SelectStatement ) sqlAst;
		assertThat( selectAst.getQuerySpec().getRootPathsForLocking() ).hasSize( 1 );
		assertThat( selectAst.getQuerySpec().getRootPathsForLocking().iterator().next().getFullPath() )
				.isEqualTo( BOOK_AUTHOR_PATH_HQL );
	}

	@Test
	void testScalarHqlTranslation(SessionFactoryScope factoryScope) {
		final HqlHelper.HqlTranslation hqlTranslation = HqlHelper.translateHql(
				"select b.title from Book b",
				factoryScope.getSessionFactory()
		);

		final Statement sqlAst = hqlTranslation.sqlAst();
		assertThat( sqlAst ).isInstanceOf( SelectStatement.class );
		final SelectStatement selectAst = ( SelectStatement ) sqlAst;
		assertThat( selectAst.getQuerySpec().getRootPathsForLocking() ).hasSize( 1 );
		assertThat( selectAst.getQuerySpec().getRootPathsForLocking().iterator().next().getFullPath() )
				.isEqualTo( BOOK_PATH_HQL );
	}

	@Test
	void testScalarHqlTranslation2(SessionFactoryScope factoryScope) {
		final HqlHelper.HqlTranslation hqlTranslation = HqlHelper.translateHql(
				"select b.title, b.author from Book b",
				factoryScope.getSessionFactory()
		);

		final Statement sqlAst = hqlTranslation.sqlAst();
		assertThat( sqlAst ).isInstanceOf( SelectStatement.class );
		final SelectStatement selectAst = ( SelectStatement ) sqlAst;
		assertThat( selectAst.getQuerySpec().getRootPathsForLocking() ).hasSize( 2 );
		final Iterator<NavigablePath> paths = selectAst.getQuerySpec().getRootPathsForLocking().iterator();
		assertThat( paths.next().getFullPath() ).isEqualTo( BOOK_PATH_HQL );
		assertThat( paths.next().getFullPath() ).isEqualTo( BOOK_AUTHOR_PATH_HQL );
	}

	@Test
	void testDynamicInstantiationHqlTranslation(SessionFactoryScope factoryScope) {
		final HqlHelper.HqlTranslation hqlTranslation = HqlHelper.translateHql(
				"select new list(b.title, b.author) from Book b",
				factoryScope.getSessionFactory()
		);

		final Statement sqlAst = hqlTranslation.sqlAst();
		assertThat( sqlAst ).isInstanceOf( SelectStatement.class );
		final SelectStatement selectAst = ( SelectStatement ) sqlAst;
		assertThat( selectAst.getQuerySpec().getRootPathsForLocking() ).hasSize( 2 );
		final Iterator<NavigablePath> paths = selectAst.getQuerySpec().getRootPathsForLocking().iterator();
		assertThat( paths.next().getFullPath() ).isEqualTo( BOOK_PATH_HQL );
		assertThat( paths.next().getFullPath() ).isEqualTo( BOOK_AUTHOR_PATH_HQL );
	}

	@Test
	void testLoadingTranslation(SessionFactoryScope factoryScope) {
		var entityDescriptor = factoryScope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( Book.class );
		var translation = LoadingAstHelper.translateLoading(
				entityDescriptor,
				1,
				factoryScope.getSessionFactory()
		);
		assertThat( translation.sqlAst().getQuerySpec().getRootPathsForLocking() ).hasSize( 1 );
		assertThat( translation.sqlAst().getQuerySpec().getRootPathsForLocking().iterator().next().getFullPath() )
				.isEqualTo( BOOK_PATH );
	}

	@Entity(name="Book")
	@Table(name="books")
	public static class Book {
		@Id
		private Integer id;
		private String title;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "author_fk")
		private Author author;

		public Book() {
		}

		public Book(Integer id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}
	}

	@Entity(name="Author")
	@Table(name="authors")
	public static class Author {
		@Id
		private Integer id;
		private String name;

		public Author() {
		}

		public Author(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
