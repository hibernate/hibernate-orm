/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.array;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsArrayToString;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsArrayAgg;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kowsar Atazadeh
 */
@DomainModel(
		annotatedClasses = {ArrayToStringWithArrayAggregateTest.Book.class, ArrayToStringWithArrayAggregateTest.Dummy.class})
@SessionFactory
@RequiresDialectFeature(feature = SupportsArrayToString.class)
@RequiresDialectFeature(feature = SupportsArrayAgg.class)
@JiraKey("HHH-18981")
public class ArrayToStringWithArrayAggregateTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new Book( 1, "title1" ) );
			em.persist( new Book( 2, "title2" ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createMutationQuery( "delete from Book" ).executeUpdate();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> q = cb.createTupleQuery();
			JpaRoot<Book> root = q.from( Book.class );

			q.multiselect(
					cb.arrayToString(
							cb.arrayAgg( cb.asc( root.get( "title" ) ), root.get( "title" ) ),
							","
					).alias( "titles" )
			);
			List<Tuple> list = em.createQuery( q ).getResultList();
			String titles = list.get( 0 ).get( "titles", String.class );
			assertThat( titles ).isEqualTo( "title1,title2" );
		} );
	}

	@Test
	public void testWithCte(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();

			JpaCriteriaQuery<Tuple> cteQuery = cb.createTupleQuery();
			JpaRoot<Book> cteRoot = cteQuery.from( Book.class );
			cteQuery.multiselect(
					cb.arrayAgg( cb.asc( cteRoot.get( "title" ) ), cteRoot.get( "title" ) )
							.alias( "titles_array" )
			);

			JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
			JpaCteCriteria<Tuple> titlesCte = query.with( cteQuery );
			JpaRoot<Tuple> root = query.from( titlesCte );
			query.multiselect(
					cb.arrayToString( root.get( "titles_array" ), cb.literal( "," ) )
							.alias( "titles" )
			);

			List<Tuple> list = em.createQuery( query ).getResultList();
			String titles = list.get( 0 ).get( "titles", String.class );
			assertThat( titles ).isEqualTo( "title1,title2" );
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

	// Needed for Oracle
	@Entity
	static class Dummy {
		@Id
		Long id;
		String[] theArray;
	}
}
