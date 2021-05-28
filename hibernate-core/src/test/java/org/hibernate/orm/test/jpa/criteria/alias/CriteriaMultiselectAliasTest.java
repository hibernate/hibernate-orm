/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.alias;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * @author Vlad Mihalcea
 */
@Jpa( annotatedClasses = CriteriaMultiselectAliasTest.Book.class )
public class CriteriaMultiselectAliasTest {

	@BeforeEach
	void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Book book = new Book();
			book.id = 1;
			book.name = bookName();

			em.persist( book );
		} );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.createQuery( "delete Book" ).executeUpdate() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13140")
	void testAlias(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final CriteriaBuilder cb = em.getCriteriaBuilder();

			final CriteriaQuery<Object[]> query = cb.createQuery( Object[].class );

			final Root<Book> entity = query.from( Book.class );
			query.multiselect(
					entity.get( "id" ).alias( "id" ),
					entity.get( "name" ).alias( "title" )
			);

			List<BookDto> dtos = em.createQuery( query )
					.unwrap( Query.class )
					.setTupleTransformer( Transformers.aliasToBean( BookDto.class ) )
					.getResultList();
			assertThat( dtos, hasSize( 1 ) );

			BookDto dto = dtos.get( 0 );
			assertThat( dto.getId(), is( 1) );
			assertThat( dto.getTitle(), is( bookName() ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13192")
	void testNoAliasInWhereClause(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final CriteriaBuilder cb = em.getCriteriaBuilder();

			final CriteriaQuery<Object[]> query = cb.createQuery( Object[].class );

			final Root<Book> entity = query.from( Book.class );
			query.multiselect(
					entity.get( "id" ).alias( "id" ),
					entity.get( "name" ).alias( "title" )
			);
			query.where( cb.equal( entity.get( "name" ), cb.parameter( String.class, "name" ) ) );

			List<BookDto> dtos = em.createQuery( query )
					.setParameter( "name", bookName() )
					.unwrap( Query.class )
					.setTupleTransformer( Transformers.aliasToBean( BookDto.class ) )
					.getResultList();
			assertThat( dtos, hasSize( 1 ) );

			BookDto dto = dtos.get( 0 );
			assertThat( dto.getId(), is ( 1 ) );
			assertThat( dto.getTitle(), is( bookName() ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13192")
	void testNoAliasInWhereClauseSimplified(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Object> criteriaQuery = cb.createQuery();
			Root<Book> root = criteriaQuery.from( Book.class );
			criteriaQuery.where( cb.equal( root.get( "id" ), cb.parameter( Integer.class, "id" ) ) );
			criteriaQuery.select( root.get( "id" ).alias( "x" ) );

			List<Object> results = em.createQuery( criteriaQuery )
					.setParameter( "id", 1 )
					.getResultList();
			assertThat( results.get( 0 ), is( 1 ) );
		} );
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		private Integer id;

		private String name;
	}

	public static class BookDto {

		private Integer id;

		private String title;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	protected String bookName() {
		return "Vlad's High-Performance Java Persistence";
	}
}
