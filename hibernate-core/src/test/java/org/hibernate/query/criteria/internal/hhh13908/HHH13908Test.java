package org.hibernate.query.criteria.internal.hhh13908;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@RequiresDialect( MySQLDialect.class )
public class HHH13908Test extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13908" )
	public void testTimeFunctionNotThrowException() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Foo> cq = cb.createQuery( Foo.class );
			final Root<Foo> foo = cq.from( Foo.class );
			cq.select( foo )
					.where(
							cb.lessThanOrEqualTo(
									cb.function( "TIME", String.class, foo.get( Foo_.startTime ) ),
									"17:00:00"
							)
					);
			// without fixing, the following exception will be thrown:
			// Parameter value [17:00:00] did not match expected type [java.util.Date (n/a)]
			//java.lang.IllegalArgumentException: Parameter value [17:00:00] did not match expected type [java.util.Date (n/a)]
			entityManager.createQuery( cq ).getResultList();
		} );
	}
}
