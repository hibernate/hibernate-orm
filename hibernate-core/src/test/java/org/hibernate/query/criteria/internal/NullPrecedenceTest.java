package org.hibernate.query.criteria.internal;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Christian Beikov
 */
@TestForIssue( jiraKey = "HHH-14897" )
public class NullPrecedenceTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class };
	}

	@Test
	public void testNullPrecedence() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( new Foo( 1L, null ) );
			entityManager.persist( new Foo( 2L, "ABC" ) );
			entityManager.persist( new Foo( 3L, "DEF" ) );
			entityManager.persist( new Foo( 4L, "DEF" ) );
			final HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();

			final CriteriaQuery<Foo> cq = cb.createQuery( Foo.class );
			final Root<Foo> foo = cq.from( Foo.class );

			cq.orderBy(
					cb.desc( foo.get( "bar" ), true ),
					cb.desc( foo.get( "id" ) )
			);

			final TypedQuery<Foo> tq = entityManager.createQuery( cq );

			final List<Foo> resultList = tq.getResultList();
			Assert.assertEquals( 4, resultList.size() );
			Assert.assertEquals( 1L, resultList.get( 0 ).getId() );
			Assert.assertEquals( 4L, resultList.get( 1 ).getId() );
			Assert.assertEquals( 3L, resultList.get( 2 ).getId() );
			Assert.assertEquals( 2L, resultList.get( 3 ).getId() );
		} );
	}

	@Entity(name = "Foo")
	public static class Foo {

		private long id;
		private String bar;

		public Foo() {
		}

		public Foo(long id, String bar) {
			this.id = id;
			this.bar = bar;
		}

		@Id
		@Column(nullable = false)
		public long getId() {
			return this.id;
		}
		public void setId(final long id) {
			this.id = id;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}
	}
}
