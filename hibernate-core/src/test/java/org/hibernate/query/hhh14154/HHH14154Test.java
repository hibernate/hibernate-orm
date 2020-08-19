package org.hibernate.query.hhh14154;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@RequiresDialect( H2Dialect.class )
@TestForIssue( jiraKey = "HHH-14154" )
public class HHH14154Test extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { HHH14154Test.Foo.class };
	}

	@Test
	public void testNoExceptionThrown() {
		doInJPA( this::sessionFactory, em -> {
			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaQuery<Foo> cq = cb.createQuery( Foo.class );
			final Root<Foo> foo = cq.from( Foo.class );
			cq.select(foo)
				.where(
					cb.lessThanOrEqualTo(
						cb.concat(
							cb.function( "FORMATDATETIME", String.class, foo.get( "startTime" ), cb.literal( "HH:mm:ss" ) ),
							""
						),
						"17:00:00"
					)
				);

			em.createQuery( cq ).getResultList();
		} );
	}

	@Entity(name = "Foo")
	@Table(name = "Foo")
	public static class Foo {

		@Id
		private long id;

		private Date startTime;

	}

}
