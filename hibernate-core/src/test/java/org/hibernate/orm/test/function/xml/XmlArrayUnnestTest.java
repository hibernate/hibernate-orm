/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.xml;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Nulls;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaFunctionJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = {
		XmlArrayUnnestTest.Book.class,
		XmlArrayUnnestTest.Publisher.class,
		XmlArrayUnnestTest.Label.class
})
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsStructuralArrays.class)
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsUnnest.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlAggregate.class)
public class XmlArrayUnnestTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new Book( 1L, "book1", new Publisher[0], List.of() ) );
			em.persist( new Book(
					2L,
					"book2",
					new Publisher[] { new Publisher( "abc" ), null, new Publisher( "def" ) },
					Arrays.asList( new Label( "k1", "v1" ), null, new Label( "k2", "v2" ) )
			) );
			em.persist( new Book( 3L, "book3", null, null ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUnnest(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-xml-array-unnest-aggregate-example[]
			List<Tuple> results = em.createQuery(
							"select e.id, p.name, l.name, l.value " +
									"from Book e " +
									"join lateral unnest(e.publishers) p " +
									"join lateral unnest(e.labels) l " +
									"order by e.id, p.name nulls first, l.name nulls first, l.value nulls first",
							Tuple.class
					)
					.getResultList();
			//end::hql-xml-array-unnest-aggregate-example[]

			// 1 row with 3 publishers and 3 labels => 3 * 3 = 9
			assertEquals( 9, results.size() );
			assertTupleEquals( results.get( 0 ), 2L, null, null, null );
			assertTupleEquals( results.get( 1 ), 2L, null, "k1", "v1" );
			assertTupleEquals( results.get( 2 ), 2L, null, "k2", "v2" );
			assertTupleEquals( results.get( 3 ), 2L, "abc", null, null );
			assertTupleEquals( results.get( 4 ), 2L, "abc", "k1", "v1" );
			assertTupleEquals( results.get( 5 ), 2L, "abc", "k2", "v2" );
			assertTupleEquals( results.get( 6 ), 2L, "def", null, null );
			assertTupleEquals( results.get( 7 ), 2L, "def", "k1", "v1" );
			assertTupleEquals( results.get( 8 ), 2L, "def", "k2", "v2" );
		} );
	}

	@Test
	public void testNodeBuilderUnnest(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final JpaRoot<Book> root = cq.from( Book.class );
			final JpaFunctionJoin<Publisher> p = root.joinArray( "publishers", SqmJoinType.LEFT );
			final JpaFunctionJoin<Label> l = root.joinArrayCollection( "labels", SqmJoinType.LEFT );
			cq.multiselect(
					root.get( "id" ),
					p.get( "name" ),
					l.get( "name" ),
					l.get( "value" )
			);
			cq.orderBy(
					cb.asc( root.get( "id" ) ),
					cb.asc( p.get( "name" ) ).nullPrecedence( Nulls.FIRST ),
					cb.asc( l.get( "name" ) ).nullPrecedence( Nulls.FIRST ),
					cb.asc( l.get( "value" ) ).nullPrecedence( Nulls.FIRST )
			);
			final List<Tuple> results = em.createQuery( cq ).getResultList();

			// 2 rows with null/empty publishers and labels
			// 1 row with 3 publishers and 3 labels => 3 * 3 = 9
			assertEquals( 11, results.size() );
			assertTupleEquals( results.get( 0 ), 1L, null, null, null );
			assertTupleEquals( results.get( 1 ), 2L, null, null, null );
			assertTupleEquals( results.get( 2 ), 2L, null, "k1", "v1" );
			assertTupleEquals( results.get( 3 ), 2L, null, "k2", "v2" );
			assertTupleEquals( results.get( 4 ), 2L, "abc", null, null );
			assertTupleEquals( results.get( 5 ), 2L, "abc", "k1", "v1" );
			assertTupleEquals( results.get( 6 ), 2L, "abc", "k2", "v2" );
			assertTupleEquals( results.get( 7 ), 2L, "def", null, null );
			assertTupleEquals( results.get( 8 ), 2L, "def", "k1", "v1" );
			assertTupleEquals( results.get( 9 ), 2L, "def", "k2", "v2" );
			assertTupleEquals( results.get( 10 ), 3L, null, null, null );
		} );
	}

	@Test
	public void testUnnestOrdinality(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-xml-array-unnest-aggregate-with-ordinality-example[]
			List<Tuple> results = em.createQuery(
							"select e.id, index(p), p.name " +
									"from Book e " +
									"join lateral unnest(e.publishers) p " +
									"order by e.id, index(p)",
							Tuple.class
					)
					.getResultList();
			//end::hql-xml-array-unnest-aggregate-with-ordinality-example[]

			// 1 row with 3 publishers
			assertEquals( 3, results.size() );

			assertPublisherTupleEquals( results.get( 0 ), 2L, 1L, "abc" );
			assertPublisherTupleEquals( results.get( 1 ), 2L, 2L, null );
			assertPublisherTupleEquals( results.get( 2 ), 2L, 3L, "def" );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "xmltable can't be used with a left join")
	public void testNodeBuilderUnnestOrdinality(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final JpaRoot<Book> root = cq.from( Book.class );
			final JpaFunctionJoin<Publisher> p = root.joinArray( "publishers", SqmJoinType.LEFT );
			cq.multiselect(
					root.get( "id" ),
					p.index(),
					p.get( "name" )
			);
			cq.orderBy(
					cb.asc( root.get( "id" ) ),
					cb.asc( p.index() )
			);
			final List<Tuple> results = em.createQuery( cq ).getResultList();

			// 2 rows with null/empty publishers and labels
			// 1 row with 3 publishers
			assertEquals( 5, results.size() );
			assertPublisherTupleEquals( results.get( 0 ), 1L, null, null );
			assertPublisherTupleEquals( results.get( 1 ), 2L, 1L, "abc" );
			assertPublisherTupleEquals( results.get( 2 ), 2L, 2L, null );
			assertPublisherTupleEquals( results.get( 3 ), 2L, 3L, "def" );
			assertPublisherTupleEquals( results.get( 4 ), 3L, null, null );
		} );
	}

	private void assertTupleEquals(Tuple tuple, long id, String publisherName, String labelName, String labelValue) {
		assertEquals( id, tuple.get( 0 ) );
		assertEquals( publisherName, tuple.get( 1 ) );
		assertEquals( labelName, tuple.get( 2 ) );
		assertEquals( labelValue, tuple.get( 3 ) );
	}

	private void assertPublisherTupleEquals(Tuple tuple, long id, Long index, String publisherName) {
		assertEquals( id, tuple.get( 0 ) );
		assertEquals( index, tuple.get( 1 ) );
		assertEquals( publisherName, tuple.get( 2 ) );
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		@JdbcTypeCode(SqlTypes.XML_ARRAY)
		private Publisher[] publishers;
		@JdbcTypeCode(SqlTypes.XML_ARRAY)
		private List<Label> labels;

		public Book() {
		}

		public Book(Long id, String title, Publisher[] publishers, List<Label> labels) {
			this.id = id;
			this.title = title;
			this.publishers = publishers;
			this.labels = labels;
		}
	}

	@Embeddable
	public static class Publisher {

		private String name;

		public Publisher() {
		}

		public Publisher(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class Label {

		private String name;
		@Column(name = "val")
		private String value;

		public Label() {
		}

		public Label(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

}
