/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.alias;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.Query;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.transform.Transformers;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
public class CriteriaMultiselectAliasTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Book.class
		};
	}

	@Before
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.id = 1;
			book.name = bookName();

			entityManager.persist( book );
		} );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-13140")
	@TestForIssue(jiraKey = "HHH-13140")
	public void testAlias() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			final CriteriaQuery<Object[]> query = cb.createQuery( Object[].class );

			final Root<Book> entity = query.from( Book.class );
			query.multiselect(
					entity.get( "id" ).alias( "id" ),
					entity.get( "name" ).alias( "title" )
			);

			List<BookDto> dtos = entityManager.createQuery( query )
					.unwrap( Query.class )
					.setResultTransformer( Transformers.aliasToBean( BookDto.class ) )
					.getResultList();
			assertEquals( 1, dtos.size() );
			BookDto dto = dtos.get( 0 );

			assertEquals( 1, (int) dto.getId() );
			assertEquals( bookName(), dto.getTitle() );
		} );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-13140")
	@TestForIssue(jiraKey = "HHH-13192")
	public void testNoAliasInWhereClause() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			final CriteriaQuery<Object[]> query = cb.createQuery( Object[].class );

			final Root<Book> entity = query.from( Book.class );
			query.multiselect(
					entity.get( "id" ).alias( "id" ),
					entity.get( "name" ).alias( "title" )
			);
			query.where(cb.equal(entity.get("name"), cb.parameter(String.class, "name")));

			List<BookDto> dtos = entityManager.createQuery( query )
					.setParameter( "name", bookName() )
					.unwrap( Query.class )
					.setResultTransformer( Transformers.aliasToBean( BookDto.class ) )
					.getResultList();
			assertEquals( 1, dtos.size() );
			BookDto dto = dtos.get( 0 );

			assertEquals( 1, (int) dto.getId() );
			assertEquals( bookName(), dto.getTitle() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13192")
	public void testNoAliasInWhereClauseSimplified() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Object> criteriaQuery = cb.createQuery();
			Root<Book> root = criteriaQuery.from( Book.class );
			criteriaQuery.where( cb.equal( root.get( "id" ), cb.parameter( Integer.class, "id" ) ) );
			criteriaQuery.select( root.get( "id" ).alias( "x" ) );

			List<Object> results = entityManager.createQuery( criteriaQuery )
					.setParameter( "id", 1 )
					.getResultList();
			assertEquals( 1, (int) results.get( 0 ) );
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
