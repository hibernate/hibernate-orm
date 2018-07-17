/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.where;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Where;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * This test involves multiple entity types in the same table that have the same ID.
 * {@code @Where} is used to distinguish the actual entity type.
 *
 * @author Gail Badner
 *
 */
public class NonUniqueIdWhereTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Product.class, Value.class, Category.class, Rating.class };
	}

	@Before
	public void setup() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					session.createNativeQuery(
							"DROP TABLE PRODUCT_VALUES"
					).executeUpdate();

					session.createNativeQuery(
							"create table PRODUCT_VALUES( id integer not null, name varchar(255), valueType char(255) not null, primary key (id, valueType) )"
					).executeUpdate();

					session.createNativeQuery( "insert into PRODUCT_VALUES( id, valueType, name) VALUES( 1, 'C', 'clothes' )" ).executeUpdate();
					session.createNativeQuery( "insert into PRODUCT_VALUES( id, valueType, name) VALUES( 1, 'R', 'high' )" ).executeUpdate();

					Category c1 = session.get( Category.class, 1 );
					Rating r1 = session.get( Rating.class, 1 );

					Product p1 = new Product();
					p1.id = 1;
					p1.category = c1;
					p1.rating = r1;
					p1.grouping = new Grouping();
					p1.grouping.category = c1;
					p1.grouping.rating = r1;

					final Grouping anotherGrouping = new Grouping();
					anotherGrouping.category = c1;
					anotherGrouping.rating = r1;

					p1.groupings.add( anotherGrouping );
					session.persist( p1 );
				}
		);

		sessionFactory().getCache().evictAllRegions();
	}

	@After
	public void cleanup() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					Product p = session.get( Product.class, 1 );
					session.delete( p );
					session.createNativeQuery(
							"DROP TABLE PRODUCT_VALUES"
					).executeUpdate();
				}
		);
	}

	@Test
	public void testLoad() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					Product product = session.get( Product.class, 1 );
					assertTrue( Hibernate.isInitialized( product.rating ) );
					assertTrue( Hibernate.isInitialized( product.category ) );
					assertTrue( Hibernate.isInitialized( product.grouping.rating ) );
					assertTrue( Hibernate.isInitialized( product.grouping.category ) );
					assertEquals( "high", product.rating.getName() );
					assertEquals( "clothes", product.category.getName() );
					assertSame( product.rating, product.grouping.rating );
					assertSame( product.category, product.grouping.category );
					assertTrue( Hibernate.isInitialized( product.groupings ) );
					assertEquals( 1, product.groupings.size() );
					Grouping collectionElement = product.groupings.iterator().next();
					assertTrue( Hibernate.isInitialized( collectionElement.category ) );
					assertTrue( Hibernate.isInitialized( collectionElement.rating ) );
					assertSame( product.rating, collectionElement.rating );
					assertSame( product.category, collectionElement.category );
				}
		);
	}

	@Entity(name = "Product")
	public static class Product {
		@Id
		private int id;

		@ManyToOne(fetch = FetchType.EAGER)
		private Category category;

		@ManyToOne(fetch = FetchType.EAGER)
		private Rating rating;

		private Grouping grouping;

		@ElementCollection(fetch = FetchType.EAGER)
		private Set<Grouping> groupings = new HashSet<Grouping>();
	}

	@Embeddable
	public static class Grouping {

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "groupingCategory")
		private Category category;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "groupingRating")
		private Rating rating;
	}

	@MappedSuperclass
	public static class Value {
		@Id
		private int id;

		private String name;

		private String valueType;

		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Value ) ) {
				return false;
			}

			Value value = (Value) o;

			if ( id != value.id ) {
				return false;
			}
			return valueType.equals( value.valueType );

		}

		@Override
		public int hashCode() {
			int result = id;
			result = 31 * result + valueType.hashCode();
			return result;
		}
	}

	@Entity(name = "Category")
	@Table(name = "PRODUCT_VALUES")
	@Where( clause = "valueType = 'C'")
	public static class Category extends Value {
	}

	@Entity(name = "Rating")
	@Table(name = "PRODUCT_VALUES")
	@Where( clause = "valueType = 'R'")
	public static class Rating extends Value {
	}
}
